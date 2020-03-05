package ai.libs.hyperopt.experimenter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.aeonbits.owner.ConfigCache;
import org.aeonbits.owner.ConfigFactory;
import org.api4.java.algorithm.Timeout;
import org.api4.java.algorithm.exceptions.AlgorithmExecutionCanceledException;
import org.api4.java.algorithm.exceptions.AlgorithmTimeoutedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.hasco.serialization.ComponentLoader;
import ai.libs.hyperopt.api.IOptimizer;
import ai.libs.hyperopt.api.input.IOptimizerConfig;
import ai.libs.hyperopt.api.input.IPlanningOptimizationTask;
import ai.libs.hyperopt.api.output.IOptimizationOutput;
import ai.libs.hyperopt.example.meka.MekaClassifierConverter;
import ai.libs.hyperopt.example.meka.MekaClassifierEvaluator;
import ai.libs.hyperopt.impl.evaluator.AutoConvertingObjectEvaluator;
import ai.libs.hyperopt.impl.model.PlanningOptimizationTask;
import ai.libs.hyperopt.impl.optimizer.baseline.RandomSearch;
import ai.libs.hyperopt.impl.optimizer.htn.bf.BestFirstOptimizer;
import ai.libs.hyperopt.impl.optimizer.htn.mcts.MCTSOptimizer;
import ai.libs.hyperopt.impl.optimizer.pcs.IPCSOptimizerConfig;
import ai.libs.hyperopt.impl.optimizer.pcs.bohb.BOHBOptimizer;
import ai.libs.hyperopt.impl.optimizer.pcs.hb.HyperBandOptimizer;
import ai.libs.hyperopt.impl.optimizer.pcs.smac.SMACOptimizer;
import ai.libs.hyperopt.logger.DatabaseLogger;
import ai.libs.hyperopt.logger.SCandidateEvaluatedSchema;
import ai.libs.jaicore.basic.sets.SetUtil;
import ai.libs.jaicore.db.IDatabaseConfig;
import ai.libs.jaicore.experiments.ExperimentDBEntry;
import ai.libs.jaicore.experiments.ExperimentDatabasePreparer;
import ai.libs.jaicore.experiments.ExperimentRunner;
import ai.libs.jaicore.experiments.IExperimentDatabaseHandle;
import ai.libs.jaicore.experiments.IExperimentIntermediateResultProcessor;
import ai.libs.jaicore.experiments.IExperimentSetEvaluator;
import ai.libs.jaicore.experiments.databasehandle.ExperimenterMySQLHandle;
import ai.libs.jaicore.experiments.exceptions.ExperimentAlreadyExistsInDatabaseException;
import ai.libs.jaicore.experiments.exceptions.ExperimentDBInteractionFailedException;
import ai.libs.jaicore.experiments.exceptions.ExperimentEvaluationFailedException;
import ai.libs.jaicore.experiments.exceptions.IllegalExperimentSetupException;
import ai.libs.jaicore.ml.classification.multilabel.learner.IMekaClassifier;
import meka.classifiers.multilabel.Evaluation;
import meka.core.MLUtils;
import meka.core.Metrics;
import meka.core.Result;
import weka.core.Instances;

public class AutoMLCExperimenter implements IExperimentSetEvaluator {

	private static final String REQ_INTERFACE = "MLClassifier";
	private static final int EVAL_ITERATIONS = 5;
	private static final double EVAL_SPLIT = .7;
	public static final double LABEL_THRESHOLD = 0.5;

	/**
	 * Variables for the experiment and database setup
	 */
	private static final File configFile = new File("automlc-setup.properties");
	private static final IAutoMLCExperimentConfig m = (IAutoMLCExperimentConfig) ConfigCache.getOrCreate(IAutoMLCExperimentConfig.class).loadPropertiesFromFile(configFile);
	private static final IDatabaseConfig dbconfig = (IDatabaseConfig) ConfigFactory.create(IDatabaseConfig.class).loadPropertiesFromFile(configFile);
	private static final IExperimentDatabaseHandle dbHandle = new ExperimenterMySQLHandle(dbconfig);
	private static final Logger logger = LoggerFactory.getLogger(AutoMLCExperimenter.class);

	public static void main(final String[] args)
			throws ExperimentDBInteractionFailedException, AlgorithmTimeoutedException, IllegalExperimentSetupException, ExperimentAlreadyExistsInDatabaseException, InterruptedException, AlgorithmExecutionCanceledException {
		if (args.length > 0) {
			switch (args[0]) {
			case "init":
				createTableWithExperiments();
				break;
			case "run":
				runExperiments();
				break;
			case "delete":
				deleteTable();
				break;
			}
		} else {
			runExperiments();
		}
	}

	public static void createTableWithExperiments()
			throws ExperimentDBInteractionFailedException, AlgorithmTimeoutedException, IllegalExperimentSetupException, ExperimentAlreadyExistsInDatabaseException, InterruptedException, AlgorithmExecutionCanceledException {
		ExperimentDatabasePreparer preparer = new ExperimentDatabasePreparer(m, dbHandle);
		preparer.synchronizeExperiments();
	}

	public static void deleteTable() throws ExperimentDBInteractionFailedException {
		dbHandle.deleteDatabase();
	}

	public static void runExperiments() throws ExperimentDBInteractionFailedException, InterruptedException {
		ExperimentRunner runner = new ExperimentRunner(m, new AutoMLCExperimenter(), dbHandle);
		runner.randomlyConductExperiments(1);
	}

	@Override
	public void evaluate(final ExperimentDBEntry experimentEntry, final IExperimentIntermediateResultProcessor processor) throws ExperimentEvaluationFailedException, InterruptedException {
		/* get experiment setup */
		Map<String, String> description = experimentEntry.getExperiment().getValuesOfKeyFields();
		String algorithm = description.get("algorithm");
		String datasetName = description.get("dataset");
		String measure = description.get("measure");
		long seed = Long.parseLong(description.get("seed"));
		String split = description.get("split");

		Timeout globalTimeout = new Timeout(Integer.parseInt(description.get("globalTimeout")), TimeUnit.SECONDS);
		Timeout evaluationTimeout = new Timeout(Integer.parseInt(description.get("evaluationTimeout")), TimeUnit.SECONDS);

		Instances dataset = null;
		try {
			dataset = new Instances(new FileReader(new File(m.getDatasetFolder(), SetUtil.implode(Arrays.asList(datasetName, seed + "", split, "train"), "_") + ".arff")));
			MLUtils.prepareData(dataset);
		} catch (Exception e) {
			throw new ExperimentEvaluationFailedException("Could not load dataset", e);
		}

		MekaClassifierConverter converter = new MekaClassifierConverter();
		MekaClassifierEvaluator evaluator = new MekaClassifierEvaluator(dataset, algorithm, seed, EVAL_ITERATIONS, EVAL_SPLIT, measure, evaluationTimeout);

		ComponentLoader cl;
		try {
			cl = new ComponentLoader(m.getComponentFile());
		} catch (IOException e) {
			throw new ExperimentEvaluationFailedException("Could not load components.", e);
		}

		IOptimizerConfig config = ConfigFactory.create(IOptimizerConfig.class);
		config.setProperty(IOptimizerConfig.K_CPUS, experimentEntry.getExperiment().getNumCPUs() + "");
		IPlanningOptimizationTask<IMekaClassifier> task = new PlanningOptimizationTask<IMekaClassifier>(converter, evaluator, cl.getComponents(), REQ_INTERFACE, globalTimeout, evaluationTimeout, cl.getParamConfigs());

		IPCSOptimizerConfig pcsConfig = ConfigFactory.create(IPCSOptimizerConfig.class);
		pcsConfig.setProperty(IPCSOptimizerConfig.K_CPUS, experimentEntry.getExperiment().getNumCPUs() + "");

		IOptimizer<IPlanningOptimizationTask<IMekaClassifier>, IMekaClassifier> opt = null;
		switch (algorithm) {
		case "mcts":
			opt = new MCTSOptimizer<>(config, task);
			break;
		case "bf":
			opt = new BestFirstOptimizer<>(config, task);
			break;
		case "bohb":
			opt = new BOHBOptimizer<IMekaClassifier>(experimentEntry.getId() + "", pcsConfig, task);
			break;
		case "hb":
			opt = new HyperBandOptimizer<IMekaClassifier>(experimentEntry.getId() + "", pcsConfig, task);
			break;
		case "smac":
			opt = new SMACOptimizer<IMekaClassifier>(experimentEntry.getId() + "", pcsConfig, task);
			break;
		case "random":
			opt = new RandomSearch<>(config, task);
			break;
		}

		Map<String, Object> defaultValues = new HashMap<>();
		defaultValues.put(SCandidateEvaluatedSchema.EXPERIMENT_ID.getName(), experimentEntry.getId());
		defaultValues.put(SCandidateEvaluatedSchema.MEASURE.getName(), measure);

		try {
			DatabaseLogger<IMekaClassifier> logger = new DatabaseLogger<>(defaultValues);
			((AutoConvertingObjectEvaluator<IMekaClassifier>) task.getDirectEvaluator(opt.getClass().getSimpleName())).registerListener(logger);
		} catch (Exception e1) {
			throw new ExperimentEvaluationFailedException(e1);
		}

		try {
			// run optimizer
			IOptimizationOutput<IMekaClassifier> result = opt.call();

			Instances testDataset = new Instances(new FileReader(new File(m.getDatasetFolder(), SetUtil.implode(Arrays.asList(datasetName, seed + "", split, "test"), "_") + ".arff")));
			MLUtils.prepareData(testDataset);
			Result testResult = Evaluation.evaluateModel(result.getObject().getClassifier(), dataset, testDataset);
			Double measureValue = null;
			switch (measure) {
			case "FMacroAvgD":
				measureValue = Metrics.P_FmacroAvgD(testResult.allTrueValues(), testResult.allPredictions(LABEL_THRESHOLD));
				break;
			case "FMicroAvg":
				measureValue = Metrics.P_FmicroAvg(testResult.allTrueValues(), testResult.allPredictions(LABEL_THRESHOLD));
				break;
			case "FMacroAvgL":
				measureValue = Metrics.P_FmacroAvgL(testResult.allTrueValues(), testResult.allPredictions(LABEL_THRESHOLD));
				break;
			}

			Map<String, Object> results = new HashMap<>();
			results.put("finalScore", measureValue);
			results.put("done", true);
			processor.processResults(results);
		} catch (Exception e) {
			throw new ExperimentEvaluationFailedException(e);
		}
	}
}

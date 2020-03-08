package results.testeval;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aeonbits.owner.ConfigCache;
import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.api4.java.algorithm.exceptions.AlgorithmExecutionCanceledException;
import org.api4.java.algorithm.exceptions.AlgorithmTimeoutedException;
import org.api4.java.datastructure.kvstore.IKVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.serialization.ComponentLoader;
import ai.libs.hyperopt.api.output.IOptimizationOutput;
import ai.libs.hyperopt.api.output.IOptimizationSolutionCandidateFoundEvent;
import ai.libs.hyperopt.example.meka.MekaClassifierConverter;
import ai.libs.hyperopt.experimenter.IAutoMLCExperimentConfig;
import ai.libs.hyperopt.impl.evaluator.AutoConvertingObjectEvaluator;
import ai.libs.hyperopt.impl.model.OptimizationOutput;
import ai.libs.hyperopt.impl.model.OptimizationSolutionCandidateFoundEvent;
import ai.libs.hyperopt.impl.optimizer.pcs.HASCOToPCSConverter;
import ai.libs.hyperopt.logger.DatabaseLogger;
import ai.libs.hyperopt.logger.SCandidateEvaluatedSchema;
import ai.libs.hyperopt.util.ComponentInstanceAdapter;
import ai.libs.jaicore.basic.sets.SetUtil;
import ai.libs.jaicore.db.IDatabaseConfig;
import ai.libs.jaicore.db.sql.SQLAdapter;
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
import meka.core.MLUtils;
import weka.core.Instances;

public class TestEvalExperimenter implements IExperimentSetEvaluator {

	private static final String REQ_INTERFACE = "MLClassifier";
	private static final int EVAL_ITERATIONS = 5;
	private static final double EVAL_SPLIT = .7;
	public static final double LABEL_THRESHOLD = 0.5;

	/**
	 * Variables for the experiment and database setup
	 */
	private static final File configFile = new File("test-eval.properties");
	private static final IAutoMLCExperimentConfig m = (IAutoMLCExperimentConfig) ConfigCache.getOrCreate(IAutoMLCExperimentConfig.class).loadPropertiesFromFile(configFile);
	private static final IDatabaseConfig dbconfig = (IDatabaseConfig) ConfigFactory.create(IDatabaseConfig.class).loadPropertiesFromFile(configFile);
	private static final IExperimentDatabaseHandle dbHandle = new ExperimenterMySQLHandle(dbconfig);
	private static final Logger logger = LoggerFactory.getLogger(TestEvalExperimenter.class);

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
		ExperimentRunner runner = new ExperimentRunner(m, new TestEvalExperimenter(), dbHandle);
		runner.randomlyConductExperiments(-1);
	}

	@Override
	public void evaluate(final ExperimentDBEntry experimentEntry, final IExperimentIntermediateResultProcessor processor) throws ExperimentEvaluationFailedException, InterruptedException {
		/* get experiment setup */
		System.out.println("Retrieved experiment to test evaluate: " + experimentEntry.getExperiment().getValuesOfKeyFields());
		Map<String, String> description = experimentEntry.getExperiment().getValuesOfKeyFields();
		String algorithm = description.get("algorithm");
		String datasetName = description.get("dataset");
		String measure = description.get("measure");
		long seed = Long.parseLong(description.get("seed"));
		String split = description.get("split");

		System.out.println("Load dataset...");
		Instances train = null;
		Instances test = null;
		try {
			train = new Instances(new FileReader(new File(m.getDatasetFolder(), SetUtil.implode(Arrays.asList(datasetName, seed + "", split, "train"), "_") + ".arff")));
			MLUtils.prepareData(train);

			test = new Instances(new FileReader(new File(m.getDatasetFolder(), SetUtil.implode(Arrays.asList(datasetName, seed + "", split, "test"), "_") + ".arff")));
			MLUtils.prepareData(test);
		} catch (Exception e) {
			throw new ExperimentEvaluationFailedException("Could not load datasets", e);
		}

		System.out.println("Load evaluator...");
		MekaClassifierConverter converter = new MekaClassifierConverter();
		MekaFixedSplitEvaluator evaluator = new MekaFixedSplitEvaluator(train, test, algorithm, measure);
		AutoConvertingObjectEvaluator<IMekaClassifier> autoConvEval = new AutoConvertingObjectEvaluator<>(algorithm, converter, evaluator);

		System.out.println("Load components...");
		ComponentLoader cl;
		try {
			cl = new ComponentLoader(m.getComponentFile());
		} catch (IOException e) {
			throw new ExperimentEvaluationFailedException("Could not load components.", e);
		}

		String jobsTable = "";
		String evalTable = "";

		switch (algorithm) {
		case "bf":
			jobsTable = "automlc_clusterjobs";
			evalTable = "automlc_eval";
			break;
		case "bohb":
			jobsTable = "hblike_clusterjobs";
			evalTable = "hblike_eval";
			break;
		case "ggp":
			jobsTable = "ggp_clusterjobs";
			evalTable = "ggp_eval";
			break;
		case "hb":
			jobsTable = "hblike_clusterjobs";
			evalTable = "hblike_eval";
			break;
		case "smac":
			jobsTable = "smac_clusterjobs";
			evalTable = "smac_eval";
			break;
		case "random":
			jobsTable = "automlc_clusterjobs";
			evalTable = "automlc_eval";
			break;
		}
		System.out.println("Assigned jobs table " + jobsTable + " and eval table " + evalTable);

		Map<String, Object> defaultValues = new HashMap<>();
		defaultValues.put(SCandidateEvaluatedSchema.EXPERIMENT_ID.getName(), experimentEntry.getId());
		defaultValues.put(SCandidateEvaluatedSchema.MEASURE.getName(), measure);

		System.out.println("Register database logger for test eval with default value " + defaultValues);
		DatabaseLogger<IMekaClassifier> logger = null;
		try {
			logger = new DatabaseLogger<>(defaultValues);
		} catch (Exception e1) {
			throw new ExperimentEvaluationFailedException(e1);
		}

		try {
			System.out.println("Create sql adapter...");
			SQLAdapter adapter = new SQLAdapter(dbconfig);

			System.out.println("Retrieve original job ID");
			String queryMeasure = measure;
			if (algorithm.equals("random")) {
				queryMeasure = "FMacroAvgD";
			}
			// Check whether we can find a job which is in state done
			List<IKVStore> job = adapter.getResultsOfQuery("SELECT * FROM " + jobsTable + " WHERE done='true' AND dataset='" + datasetName + "' AND algorithm='" + algorithm + "' AND seed=" + seed + " AND split=" + split + " LIMIT 1");
			if (job.isEmpty()) {
				Map<String, Object> results = new HashMap<>();
				results.put("done", "N/A");
				processor.processResults(results);
				return;
			}

			// experiment id
			int experimentID = job.get(0).getAsInt("experiment_id");
			System.out.println("Found original experiment ID: " + experimentID);

			// Retrieve evaluations for experiment id that did not fail
			System.out.println("Get non-failing evaluations for this experiment");
			List<IKVStore> evaluations = adapter.getResultsOfQuery("SELECT * FROM " + evalTable + " WHERE experiment_id=" + experimentID + " AND exception IS NULL ORDER BY time_until_found");
			System.out.println(evaluations.size());

			System.out.println("Filter evaluations for only the relevant evaluations");
			List<IKVStore> filteredEvaluations = new ArrayList<>();
			Double bestValue = null;
			ObjectMapper mapper = new ObjectMapper();
			for (IKVStore store : evaluations) {
				JsonNode evalReport = mapper.readTree(store.getAsString("evaluation_report"));
				int n = evalReport.get(measure + "_n").asInt();
				double evalValue = evalReport.get(measure + "_mean").asDouble() * Math.pow(10, 5 - n);
				if (bestValue == null || evalValue < bestValue) {
					System.out.println(bestValue + " " + evalValue);
					bestValue = evalValue;
					filteredEvaluations.add(store);
				}
			}

			// Evaluate filtered candidates on test data
			System.out.println("Evaluate candidates on test data...");
			HASCOToPCSConverter pcsConverter = new HASCOToPCSConverter(cl.getComponents(), "MLClassifier");
			File outputFile = File.createTempFile("output", ".pcs");
			outputFile.deleteOnExit();
			pcsConverter.generatePCSFile(outputFile);

			ComponentInstanceAdapter ciAdapter = new ComponentInstanceAdapter(cl.getComponents());
			Map<Long, Double> trace = new HashMap<>();
			Double lastScore = null;

			for (int i = 0; i < filteredEvaluations.size(); i++) {
				IKVStore testEvalStore = filteredEvaluations.get(i);
				String ciString = testEvalStore.getAsString(SCandidateEvaluatedSchema.COMPONENT_INSTANCE.getName());
				ComponentInstance ci = ciAdapter.stringToComponentInstance(ciString);
				replaceCatValues(ci, pcsConverter.getReverseMaskedCatValues());
				Map<String, DescriptiveStatistics> evallog = new HashMap<>();
				double score = autoConvEval.evaluate(ci, evaluator.getMaxBudget(), evallog);
				trace.put(testEvalStore.getAsLong("time_until_found"), score);
				lastScore = score;

				System.out.println(testEvalStore.getAsLong("time_until_found") + ": " + score);

				IOptimizationOutput<IMekaClassifier> output = new OptimizationOutput<>(testEvalStore.getAsLong(SCandidateEvaluatedSchema.TIMESTAMP_FOUND.getName()),
						testEvalStore.getAsLong(SCandidateEvaluatedSchema.TIME_UNTIL_FOUND.getName()), null, score, ci, evallog);
				IOptimizationSolutionCandidateFoundEvent<IMekaClassifier> logEntry;
				if (i < filteredEvaluations.size() - 1) {
					logEntry = new OptimizationSolutionCandidateFoundEvent<>(algorithm, output);
				} else {
					logEntry = new OptimizationSolutionCandidateFoundEvent<>(algorithm, output, "lastCandidate");
				}
				logger.rcvCandidateEvaluatedEvent(logEntry);

			}

			Map<String, Object> results = new HashMap<>();
			results.put("finalScore", lastScore);
			results.put("trace", new ObjectMapper().writeValueAsString(trace));
			results.put("done", true);
			processor.processResults(results);
		} catch (Exception e) {
			throw new ExperimentEvaluationFailedException(e);
		}
	}

	private static void replaceCatValues(final ComponentInstance ci, final Map<String, String> maskedValues) {
		Map<String, String> update = new HashMap<>();
		ci.getParameterValues().entrySet().stream().filter(x -> maskedValues.containsKey(x.getValue())).forEach(x -> update.put(x.getKey(), maskedValues.get(x.getValue())));
		ci.getParameterValues().putAll(update);
		ci.getSatisfactionOfRequiredInterfaces().values().forEach(x -> replaceCatValues(x, maskedValues));
	}
}

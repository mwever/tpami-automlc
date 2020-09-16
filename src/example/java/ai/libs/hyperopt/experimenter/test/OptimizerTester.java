package ai.libs.hyperopt.experimenter.test;

import java.util.HashMap;
import java.util.Map;

import ai.libs.jaicore.experiments.Experiment;
import ai.libs.jaicore.experiments.ExperimentDBEntry;
import ai.libs.jaicore.experiments.IExperimentIntermediateResultProcessor;
import ai.libs.jaicore.experiments.exceptions.ExperimentEvaluationFailedException;
import benchmark.meka.experimenter.AutoMLCExperimenter;

public class OptimizerTester {

	public static final int NUM_CPUS = 4;
	public static final int MAX_MEM = 4096;

	public static final String DATASET = "flags";
	public static final int SEED = 0;
	public static final int SPLIT = 0;
	public static final String MEASURE = "FMacroAvgD";
	public static final int GLOBAL_TIMEOUT = 60;
	public static final int EVAL_TIMEOUT = 45;

	public OptimizerTester(final String optimizer) throws ExperimentEvaluationFailedException, InterruptedException {
		AutoMLCExperimenter runner = new AutoMLCExperimenter();

		Map<String, String> valuesOfKeyFields = new HashMap<>();

		valuesOfKeyFields.put("algorithm", optimizer);
		valuesOfKeyFields.put("dataset", OptimizerTester.DATASET);
		valuesOfKeyFields.put("seed", OptimizerTester.SEED + "");
		valuesOfKeyFields.put("split", OptimizerTester.SPLIT + "");
		valuesOfKeyFields.put("measure", OptimizerTester.MEASURE);
		valuesOfKeyFields.put("globalTimeout", OptimizerTester.GLOBAL_TIMEOUT + "");
		valuesOfKeyFields.put("evaluationTimeout", OptimizerTester.EVAL_TIMEOUT + "");
		ExperimentDBEntry experimentEntry = new ExperimentDBEntry(0, new Experiment(MAX_MEM, NUM_CPUS, valuesOfKeyFields));

		runner.evaluate(experimentEntry, new IExperimentIntermediateResultProcessor() {
			@Override
			public void processResults(final Map<String, Object> results) {
				System.out.println("Received final incumbent: " + results);
			}
		});
	}

}

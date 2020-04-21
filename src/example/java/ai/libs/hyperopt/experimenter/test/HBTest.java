package ai.libs.hyperopt.experimenter.test;

import java.util.HashMap;
import java.util.Map;

import ai.libs.hyperopt.experimenter.AutoMLCExperimenter;
import ai.libs.jaicore.experiments.Experiment;
import ai.libs.jaicore.experiments.ExperimentDBEntry;
import ai.libs.jaicore.experiments.IExperimentIntermediateResultProcessor;
import ai.libs.jaicore.experiments.exceptions.ExperimentEvaluationFailedException;

public class HBTest {

	public static final int MAX_MEM = 4096;
	public static final int NUM_CPUS = 8;

	public static void main(final String[] args) throws ExperimentEvaluationFailedException, InterruptedException {
		AutoMLCExperimenter runner = new AutoMLCExperimenter();

		Map<String, String> valuesOfKeyFields = new HashMap<>();

		valuesOfKeyFields.put("algorithm", "bohb");
		valuesOfKeyFields.put("dataset", "flags");
		valuesOfKeyFields.put("seed", "42");
		valuesOfKeyFields.put("split", "0");
		valuesOfKeyFields.put("measure", "FMacroAvgD");
		valuesOfKeyFields.put("globalTimeout", "60");
		valuesOfKeyFields.put("evaluationTimeout", "45");
		ExperimentDBEntry experimentEntry = new ExperimentDBEntry(0, new Experiment(MAX_MEM, NUM_CPUS, valuesOfKeyFields));

		runner.evaluate(experimentEntry, new IExperimentIntermediateResultProcessor() {
			@Override
			public void processResults(final Map<String, Object> results) {
				System.out.println("Obtained results: " + results);
			}
		});
	}

}

import java.util.HashMap;
import java.util.Map;

import ai.libs.jaicore.experiments.Experiment;
import ai.libs.jaicore.experiments.ExperimentDBEntry;
import ai.libs.jaicore.experiments.IExperimentIntermediateResultProcessor;
import ai.libs.jaicore.experiments.exceptions.ExperimentEvaluationFailedException;
import results.testeval.TestEvalExperimenter;

public class TestEvalExperimenterTest {
	public static final int MAX_MEM = 4096;
	public static final int NUM_CPUS = 8;

	public static void main(final String[] args) throws ExperimentEvaluationFailedException, InterruptedException {
		TestEvalExperimenter runner = new TestEvalExperimenter();
		Map<String, String> valuesOfKeyFields = new HashMap<>();
		valuesOfKeyFields.put("algorithm", "hb");
		valuesOfKeyFields.put("dataset", "bibtex");
		valuesOfKeyFields.put("seed", "42");
		valuesOfKeyFields.put("split", "0");
		valuesOfKeyFields.put("measure", "FMacroAvgD");
		ExperimentDBEntry experimentEntry = new ExperimentDBEntry(0, new Experiment(MAX_MEM, NUM_CPUS, valuesOfKeyFields));

		runner.evaluate(experimentEntry, new IExperimentIntermediateResultProcessor() {
			@Override
			public void processResults(final Map<String, Object> results) {
				System.out.println("Obtained results: " + results);
			}
		});

	}
}

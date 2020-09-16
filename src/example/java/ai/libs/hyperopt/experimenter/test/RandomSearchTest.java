package ai.libs.hyperopt.experimenter.test;

import ai.libs.jaicore.experiments.exceptions.ExperimentEvaluationFailedException;

/**
 * Test run for a simple test configuration for a random search.
 *
 * @author mwever
 *
 */
public class RandomSearchTest {

	public static void main(final String[] args) throws ExperimentEvaluationFailedException, InterruptedException {
		new OptimizerTester("random");
	}

}

package ai.libs.hyperopt.experimenter.test;

import ai.libs.jaicore.experiments.exceptions.ExperimentEvaluationFailedException;

/**
 * Test run with a simple test configuration for BOHB.
 *
 * @author mwever
 *
 */
public class BOHBTest {

	public static void main(final String[] args) throws ExperimentEvaluationFailedException, InterruptedException {
		new OptimizerTester("bohb");
	}

}

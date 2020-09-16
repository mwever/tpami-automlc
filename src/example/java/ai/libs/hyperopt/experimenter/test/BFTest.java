package ai.libs.hyperopt.experimenter.test;

import ai.libs.jaicore.experiments.exceptions.ExperimentEvaluationFailedException;

/**
 * Test run for a simple test configuration for HTN-BF
 *
 * @author mwever
 */
public class BFTest {

	public static void main(final String[] args) throws ExperimentEvaluationFailedException, InterruptedException {
		new OptimizerTester("bf");
	}

}

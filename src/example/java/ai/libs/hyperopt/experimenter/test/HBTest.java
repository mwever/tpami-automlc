package ai.libs.hyperopt.experimenter.test;

import ai.libs.jaicore.experiments.exceptions.ExperimentEvaluationFailedException;

/**
 * Test run for a simple test config for Hyperband
 *
 * @author mwever
 *
 */
public class HBTest {

	public static void main(final String[] args) throws ExperimentEvaluationFailedException, InterruptedException {
		new OptimizerTester("hb");
	}

}

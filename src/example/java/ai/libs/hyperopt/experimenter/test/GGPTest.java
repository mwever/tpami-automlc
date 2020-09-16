package ai.libs.hyperopt.experimenter.test;

import ai.libs.jaicore.experiments.exceptions.ExperimentEvaluationFailedException;

public class GGPTest {

	public static void main(final String[] args) throws ExperimentEvaluationFailedException, InterruptedException {
		new OptimizerTester("ggp");
	}

}

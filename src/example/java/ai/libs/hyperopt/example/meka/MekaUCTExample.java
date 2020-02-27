package ai.libs.hyperopt.example.meka;

import java.io.IOException;

import org.api4.java.algorithm.exceptions.AlgorithmException;
import org.api4.java.algorithm.exceptions.AlgorithmExecutionCanceledException;
import org.api4.java.algorithm.exceptions.AlgorithmTimeoutedException;

import ai.libs.hyperopt.api.IOptimizer;
import ai.libs.hyperopt.api.input.IOptimizationTask;
import ai.libs.hyperopt.impl.optimizer.htn.UCTOptimizer;
import ai.libs.jaicore.ml.classification.multilabel.learner.IMekaClassifier;

public class MekaUCTExample extends AMekaExample {

	protected MekaUCTExample() throws IOException, AlgorithmTimeoutedException, InterruptedException, AlgorithmExecutionCanceledException, AlgorithmException {
		super();
	}

	@Override
	public IOptimizer<? extends IOptimizationTask<IMekaClassifier>, IMekaClassifier> getOptimizer() {
		return new UCTOptimizer<IMekaClassifier>(this.getConfig(), this.getTask());
	}

	public static void main(final String[] args) throws IOException, AlgorithmTimeoutedException, InterruptedException, AlgorithmExecutionCanceledException, AlgorithmException {
		new MekaUCTExample();
	}

}

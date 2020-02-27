package ai.libs.hyperopt.example.meka;

import java.io.IOException;

import org.api4.java.algorithm.exceptions.AlgorithmException;
import org.api4.java.algorithm.exceptions.AlgorithmExecutionCanceledException;
import org.api4.java.algorithm.exceptions.AlgorithmTimeoutedException;

import ai.libs.hyperopt.api.IOptimizer;
import ai.libs.hyperopt.impl.optimizer.htn.MCTSOptimizer;
import ai.libs.jaicore.ml.classification.multilabel.learner.IMekaClassifier;

public class MekaMCTSExample extends AMekaExample {

	public MekaMCTSExample() throws IOException, AlgorithmTimeoutedException, InterruptedException, AlgorithmExecutionCanceledException, AlgorithmException {
		super();
	}

	@Override
	public IOptimizer<?, IMekaClassifier> getOptimizer() {
		return new MCTSOptimizer<>(this.getConfig(), this.getTask());
	}

	public static void main(final String[] args) throws AlgorithmTimeoutedException, IOException, InterruptedException, AlgorithmExecutionCanceledException, AlgorithmException {
		new MekaMCTSExample();
	}

}

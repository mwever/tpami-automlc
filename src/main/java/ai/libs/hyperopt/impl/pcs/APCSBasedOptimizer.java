package ai.libs.hyperopt.impl.pcs;

import org.api4.java.algorithm.events.IAlgorithmEvent;
import org.api4.java.algorithm.exceptions.AlgorithmException;
import org.api4.java.algorithm.exceptions.AlgorithmExecutionCanceledException;
import org.api4.java.algorithm.exceptions.AlgorithmTimeoutedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.hyperopt.api.input.IOptimizationTask;
import ai.libs.hyperopt.api.input.IOptimizerConfig;
import ai.libs.hyperopt.api.output.IOptimizationSolutionCandidateFoundEvent;
import ai.libs.jaicore.basic.algorithm.AOptimizer;

public abstract class APCSBasedOptimizer<M> extends AOptimizer<IOptimizationTask<M>, IOptimizationSolutionCandidateFoundEvent<M>, Double> {

	private static final Logger LOGGER = LoggerFactory.getLogger(APCSBasedOptimizer.class);

	protected APCSBasedOptimizer(final IOptimizationTask<M> input) {
		super(input);
	}

	protected APCSBasedOptimizer(final IOptimizerConfig config, final IOptimizationTask<M> input) {
		super(config, input);
	}

	@Override
	public IAlgorithmEvent nextWithException() throws InterruptedException, AlgorithmExecutionCanceledException, AlgorithmTimeoutedException, AlgorithmException {
		switch (this.getState()) {
		case CREATED:
			return super.activate();
		case ACTIVE:
			LOGGER.debug("Prepare optimization.");
			try {
				this.prepareOptimization();
			} catch (InterruptedException e) {
				throw e;
			} catch (Exception e) {
				throw new AlgorithmException("Could not prepare optimization", e);
			}

			LOGGER.debug("Start optimization.");
			try {
				this.runOptimization();
			} catch (InterruptedException e) {
				throw e;
			} catch (Exception e) {
				throw new AlgorithmException("Could not run optimization", e);
			}

			LOGGER.debug("Post handling of the optimization.");
			try {
				this.postOptimize();
			} catch (InterruptedException e) {
				throw e;
			} catch (Exception e) {
				throw new AlgorithmException("Could not post handle optimization", e);
			}

			return this.getBestSeenSolution();
		default:
			throw new IllegalStateException("Cannot do anything in state " + this.getState());
		}
	}

	public void prepareOptimization() throws Exception {

	}

	public abstract void runOptimization() throws Exception;

	public void postOptimize() throws Exception {

	}

}

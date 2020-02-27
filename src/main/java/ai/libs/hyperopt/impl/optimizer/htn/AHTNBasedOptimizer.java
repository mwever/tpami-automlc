package ai.libs.hyperopt.impl.optimizer.htn;

import org.api4.java.algorithm.events.IAlgorithmEvent;
import org.api4.java.algorithm.exceptions.AlgorithmException;
import org.api4.java.algorithm.exceptions.AlgorithmExecutionCanceledException;
import org.api4.java.algorithm.exceptions.AlgorithmTimeoutedException;
import org.api4.java.common.attributedobjects.ScoredItem;

import com.google.common.eventbus.Subscribe;

import ai.libs.hasco.events.HASCOSolutionEvent;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.variants.forwarddecomposition.HASCOViaFDFactory;
import ai.libs.hyperopt.api.ConversionFailedException;
import ai.libs.hyperopt.api.IOptimizer;
import ai.libs.hyperopt.api.input.IOptimizerConfig;
import ai.libs.hyperopt.api.input.IPlanningOptimizationTask;
import ai.libs.hyperopt.api.output.IOptimizationOutput;
import ai.libs.hyperopt.impl.model.OptimizationOutput;
import ai.libs.jaicore.basic.algorithm.AOptimizer;
import ai.libs.jaicore.planning.hierarchical.algorithms.forwarddecomposition.graphgenerators.tfd.TFDNode;
import ai.libs.jaicore.search.algorithms.standard.bestfirst.events.RolloutEvent;
import ai.libs.jaicore.search.probleminputs.GraphSearchWithPathEvaluationsInput;

public abstract class AHTNBasedOptimizer<M> extends AOptimizer<IPlanningOptimizationTask<M>, IOptimizationOutput<M>, Double> implements IOptimizer<IPlanningOptimizationTask<M>, M> {

	private HASCOViaFDFactory<GraphSearchWithPathEvaluationsInput<TFDNode, String, Double>, Double> factory;

	@SuppressWarnings("unchecked")
	public AHTNBasedOptimizer(final IOptimizerConfig config, final IPlanningOptimizationTask<M> input) {
		super(config, input);
	}

	@Override
	public IAlgorithmEvent nextWithException() throws InterruptedException, AlgorithmExecutionCanceledException, AlgorithmTimeoutedException, AlgorithmException {
		switch (this.getState()) {
		case CREATED:
			// wait for HASCO to initialize.
			return super.activate();
		case ACTIVE:
			AOptimizer algo = this.getAlgo();
			algo.setTimeout(this.getInput().getGlobalTimeout());
			algo.registerListener(new Object() {
				@Subscribe
				public void rcvEVent(final Object event) {
					System.out.println(event.getClass());

					if (event instanceof RolloutEvent) {
						System.out.println(((RolloutEvent) event).getScore());
					}
				}

				@Subscribe
				public void rcvEvent(final HASCOSolutionEvent<Double> event) {
					try {
						AHTNBasedOptimizer.this.updateBestSeenSolution(new OptimizationOutput<M>(AHTNBasedOptimizer.this.getInput().getConverter().convert(event.getSolutionCandidate().getComponentInstance()), event.getScore(),
								event.getSolutionCandidate().getComponentInstance()));
					} catch (ConversionFailedException e) {
						e.printStackTrace();
					}
				}
			});
			ScoredItem result = null;
			try {
				result = algo.call();
			} catch (AlgorithmTimeoutedException e) {
				result = algo.getBestSeenSolution();
			}
			return this.terminate();
		default:
			throw new IllegalStateException("HASCO cannot do anything in state " + this.getState());
		}
	}

	@Override
	public ComponentInstance getResultAsComponentInstance() {
		return null;
	}

	@Override
	public M getResult() {
		return ((IOptimizationOutput<M>) this.getAlgo().getBestSeenSolution()).getObject();
	}

	public abstract AOptimizer getAlgo();

}

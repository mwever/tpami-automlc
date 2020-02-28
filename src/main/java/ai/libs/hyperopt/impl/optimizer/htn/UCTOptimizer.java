package ai.libs.hyperopt.impl.optimizer.htn;

import org.api4.java.ai.graphsearch.problem.IPathSearchWithPathEvaluationsInput;

import ai.libs.hyperopt.api.input.IOptimizerConfig;
import ai.libs.hyperopt.api.input.IPlanningOptimizationTask;
import ai.libs.jaicore.basic.algorithm.AOptimizer;
import ai.libs.jaicore.planning.hierarchical.algorithms.forwarddecomposition.graphgenerators.tfd.TFDNode;
import ai.libs.jaicore.search.algorithms.standard.mcts.UCTPathSearchFactory;

public class UCTOptimizer<M> extends AHTNBasedOptimizer<M> {

	private AOptimizer search;

	public UCTOptimizer(final IOptimizerConfig config, final IPlanningOptimizationTask<M> input) {
		super(config, input);

		/* Configure UCT factory and retrieve search algorithm */
		UCTPathSearchFactory<IPathSearchWithPathEvaluationsInput<TFDNode, String, Double>, TFDNode, String> factory = new UCTPathSearchFactory<>();
		factory.setSeed(0);
		factory.setEvaluationFailurePenalty(2.0);
		factory.setProblemInput(new OptimizationSearchPathProblem<M>(this.getClass().getSimpleName(), input));
		this.search = factory.getAlgorithm();
	}

	@Override
	public AOptimizer getAlgo() {
		return this.search;
	}

}

package ai.libs.hyperopt.impl.optimizer.htn.mcts;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import ai.libs.hyperopt.api.input.IOptimizerConfig;
import ai.libs.hyperopt.api.input.IPlanningOptimizationTask;
import ai.libs.hyperopt.impl.optimizer.htn.AHTNBasedOptimizer;
import ai.libs.jaicore.basic.algorithm.AOptimizer;
import ai.libs.jaicore.search.algorithms.standard.mcts.comparison.FixedCommitmentMCTSPathSearch;

public class MCTSOptimizer<M> extends AHTNBasedOptimizer<M> {

	public static final String NAME = "mcts";

	private AOptimizer search;

	public MCTSOptimizer(final IOptimizerConfig config, final IPlanningOptimizationTask<M> input) {
		super(config, input);
		/* Configure UCT factory and retrieve search algorithm */
		this.search = new FixedCommitmentMCTSPathSearch<>(new OptimizationSearchPathProblem<>(this.getClass().getSimpleName(), input), 0.0, 100, DescriptiveStatistics::getMean);
		this.search.setNumCPUs(config.cpus());
		System.out.println("NUM_CPUS: " + this.search.getNumCPUs());
	}

	@Override
	public AOptimizer getAlgo() {
		return this.search;
	}

	@Override
	public String getName() {
		return NAME;
	}

}

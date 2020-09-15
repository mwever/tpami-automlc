package benchmark.core.impl.optimizer.pcs.bohb;

import benchmark.core.api.input.IOptimizerConfig;
import benchmark.core.api.input.IPlanningOptimizationTask;
import benchmark.core.impl.optimizer.pcs.AHBLikeOptimizer;

/**
 *
 * @author mwever
 *
 */
public class BOHBOptimizer<M> extends AHBLikeOptimizer<M> {

	private static final String NAME = "bohb";

	public BOHBOptimizer(final String id, final IOptimizerConfig config, final IPlanningOptimizationTask<M> builder) {
		super(id, config, builder);
	}

	@Override
	public String getName() {
		return NAME;
	}

}

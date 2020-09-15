package benchmark.core.impl.optimizer.pcs.hb;

import benchmark.core.api.input.IOptimizerConfig;
import benchmark.core.api.input.IPlanningOptimizationTask;
import benchmark.core.impl.optimizer.pcs.AHBLikeOptimizer;

/**
 *
 * @author mwever
 *
 */
public class HyperBandOptimizer<M> extends AHBLikeOptimizer<M> {

	private static final String NAME = "hb";

	public HyperBandOptimizer(final String id, final IOptimizerConfig config, final IPlanningOptimizationTask<M> builder) {
		super(id, config, builder);
	}

	@Override
	public String getName() {
		return NAME;
	}

}

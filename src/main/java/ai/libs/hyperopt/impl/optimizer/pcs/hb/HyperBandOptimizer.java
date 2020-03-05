package ai.libs.hyperopt.impl.optimizer.pcs.hb;

import ai.libs.hyperopt.api.input.IOptimizerConfig;
import ai.libs.hyperopt.api.input.IPlanningOptimizationTask;
import ai.libs.hyperopt.impl.optimizer.pcs.AHBLikeOptimizer;

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

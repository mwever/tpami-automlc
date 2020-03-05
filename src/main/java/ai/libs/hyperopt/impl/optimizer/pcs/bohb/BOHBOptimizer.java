package ai.libs.hyperopt.impl.optimizer.pcs.bohb;

import ai.libs.hyperopt.api.input.IOptimizerConfig;
import ai.libs.hyperopt.api.input.IPlanningOptimizationTask;
import ai.libs.hyperopt.impl.optimizer.pcs.AHBLikeOptimizer;

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

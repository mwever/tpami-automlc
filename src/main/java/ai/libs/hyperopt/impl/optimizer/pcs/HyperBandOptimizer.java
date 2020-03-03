package ai.libs.hyperopt.impl.optimizer.pcs;

import java.util.List;

import ai.libs.hyperopt.api.input.IOptimizationTask;
import ai.libs.hyperopt.api.input.IPlanningOptimizationTask;
import ai.libs.hyperopt.impl.pcs.AHyperbandLikeOptimizer;

/**
 *
 * @author kadirayk
 *
 */
public class HyperBandOptimizer<M> extends APCSBasedOptimizer<M> {

	public HyperBandOptimizer(final HyperbandLikeOptimizerConfig config, final IPlanningOptimizationTask<M> builder) {
		super(config, builder);
	}

	@Override
	public List<String> getCommand() {
		return null;
	}

}

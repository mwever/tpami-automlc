package ai.libs.hyperopt.impl.optimizer.pcs;

import java.util.List;

import ai.libs.hyperopt.api.input.IOptimizationTask;
import ai.libs.hyperopt.api.input.IPlanningOptimizationTask;
import ai.libs.jaicore.basic.algorithm.AOptimizer;

/**
 *
 * @author kadirayk
 *
 */
public class BOHBOptimizer<M> extends APCSBasedOptimizer<M> {

	public BOHBOptimizer(final HyperbandLikeOptimizerConfig config, final IPlanningOptimizationTask<M> input) {
		super(config, input);
	}

	@Override
	public List<String> getCommand() {
		return null;
	}


}

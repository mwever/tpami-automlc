package ai.libs.hyperopt.impl.optimizer.pcs.bohb;

import java.util.List;

import ai.libs.hyperopt.api.input.IPlanningOptimizationTask;
import ai.libs.hyperopt.impl.optimizer.pcs.APCSBasedOptimizer;
import ai.libs.hyperopt.impl.optimizer.pcs.IPCSOptimizerConfig;

/**
 *
 * @author kadirayk
 *
 */
public class BOHBOptimizer<M> extends APCSBasedOptimizer<M> {

	public BOHBOptimizer(final String id, final IPCSOptimizerConfig config, final IPlanningOptimizationTask<M> input) {
		super(id, config, input);
	}

	@Override
	public List<String> getCommand() {
		return null;
	}

	@Override
	protected void runOptimizer() throws Exception {
		// TODO Auto-generated method stub

	}

}

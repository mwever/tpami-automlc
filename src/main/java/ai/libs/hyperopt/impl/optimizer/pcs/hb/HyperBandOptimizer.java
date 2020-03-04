package ai.libs.hyperopt.impl.optimizer.pcs.hb;

import java.util.Arrays;
import java.util.List;

import ai.libs.hyperopt.api.input.IOptimizerConfig;
import ai.libs.hyperopt.api.input.IPlanningOptimizationTask;
import ai.libs.hyperopt.impl.optimizer.pcs.APCSBasedOptimizer;

/**
 *
 * @author mwever
 *
 */
public class HyperBandOptimizer<M> extends APCSBasedOptimizer<M> {

	private final String scriptName = "testsmac.py";

	public HyperBandOptimizer(final String id, final IOptimizerConfig config, final IPlanningOptimizationTask<M> builder) {
		super(id, config, builder);
	}

	@Override
	public List<String> getCommand() {
		return Arrays.asList("singularity", "exec", this.getConfig().getSingularityContainer().getAbsolutePath(), "python", this.scriptName, "--min_budget", "1", "--max_budget", "5", "--n_iterations", "500");
	}

	@Override
	protected void runOptimizer() throws Exception {
		// TODO Auto-generated method stub

	}

}

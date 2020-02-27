package ai.libs.hyperopt.impl.optimizer.pcs;

import ai.libs.hyperopt.api.input.IOptimizationTask;
import ai.libs.hyperopt.impl.pcs.AHyperbandLikeOptimizer;

/**
 *
 * @author kadirayk
 *
 */
public class HyperBandOptimizer<M> extends AHyperbandLikeOptimizer<M> {

	public HyperBandOptimizer(final HyperbandLikeOptimizerConfig config, final IOptimizationTask<M> builder) {
		super(config, builder);
	}

	@Override
	public String getScriptExec() {
		return "python HpBandSterOptimizer.py";
	}

	@Override
	public String getOutputLog() {
		return "testrsc/hpband.log";
	}

}

package ai.libs.hyperopt.impl.optimizer.pcs;

import ai.libs.hyperopt.api.input.IOptimizationTask;
import ai.libs.hyperopt.impl.pcs.AHyperbandLikeOptimizer;

/**
 *
 * @author kadirayk
 *
 */
public class BOHBOptimizer<M> extends AHyperbandLikeOptimizer<M> {

	public BOHBOptimizer(final HyperbandLikeOptimizerConfig config, final IOptimizationTask<M> input) {
		super(config, input);
	}

	@Override
	public String getScriptExec() {
		return "python BOHBOptimizerRunner.py";
	}

	@Override
	public String getOutputLog() {
		return "testrsc/bohb.log";
	}

}

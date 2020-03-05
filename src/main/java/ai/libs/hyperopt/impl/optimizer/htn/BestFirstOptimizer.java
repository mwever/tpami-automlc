package ai.libs.hyperopt.impl.optimizer.htn;

import org.aeonbits.owner.ConfigFactory;
import org.api4.java.ai.graphsearch.problem.pathsearch.pathevaluation.IPathEvaluator;
import org.api4.java.ai.graphsearch.problem.pathsearch.pathevaluation.PathEvaluationException;
import org.api4.java.datastructure.graph.ILabeledPath;

import ai.libs.hasco.core.HASCOConfig;
import ai.libs.hasco.core.RefinementConfiguredSoftwareConfigurationProblem;
import ai.libs.hasco.core.SoftwareConfigurationProblem;
import ai.libs.hasco.variants.forwarddecomposition.HASCOViaFDAndBestFirstWithRandomCompletionsFactory;
import ai.libs.hyperopt.api.input.IOptimizerConfig;
import ai.libs.hyperopt.api.input.IPlanningOptimizationTask;
import ai.libs.jaicore.basic.algorithm.AOptimizer;
import ai.libs.jaicore.planning.hierarchical.algorithms.forwarddecomposition.graphgenerators.tfd.TFDNode;

public class BestFirstOptimizer<M> extends AHTNBasedOptimizer<M> {

	public static final String NAME = "bf";

	private final HASCOViaFDAndBestFirstWithRandomCompletionsFactory hascoFactory;
	private final AOptimizer algo;

	public BestFirstOptimizer(final IOptimizerConfig config, final IPlanningOptimizationTask<M> input) {
		super(config, input);
		this.hascoFactory = new HASCOViaFDAndBestFirstWithRandomCompletionsFactory(0, 10);
		HASCOConfig hascoConfig = ConfigFactory.create(HASCOConfig.class);
		hascoConfig.setProperty(HASCOConfig.K_CPUS, "" + config.cpus());
		hascoConfig.setProperty(HASCOConfig.K_MEMORY, "" + config.memory());
		this.hascoFactory.withAlgorithmConfig(hascoConfig);
		this.hascoFactory.setPreferredNodeEvaluator(new IPathEvaluator<TFDNode, String, Double>() {
			@Override
			public Double evaluate(final ILabeledPath<TFDNode, String> path) throws PathEvaluationException, InterruptedException {
				return (path.getArcs().isEmpty()) ? 0.0 : null;
			}
		});

		SoftwareConfigurationProblem<Double> coreProblem = new SoftwareConfigurationProblem<>(input.getComponents(), input.getRequestedInterface(), input.getDirectEvaluator(this.getClass().getSimpleName()));
		this.hascoFactory.setProblemInput(new RefinementConfiguredSoftwareConfigurationProblem<>(coreProblem, input.getParameterRefinementConfiguration()));
		this.algo = this.hascoFactory.getAlgorithm();
		this.algo.setNumCPUs(config.cpus());

	}

	@Override
	public AOptimizer getAlgo() {
		return this.algo;
	}

	@Override
	public String getName() {
		return NAME;
	}

}

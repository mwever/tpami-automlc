package ai.libs.hyperopt.impl.optimizer.htn.bf;

import org.aeonbits.owner.ConfigFactory;
import org.api4.java.ai.graphsearch.problem.pathsearch.pathevaluation.IPathEvaluator;
import org.api4.java.ai.graphsearch.problem.pathsearch.pathevaluation.PathEvaluationException;
import org.api4.java.datastructure.graph.ILabeledPath;

import ai.libs.hasco.builder.HASCOBuilder;
import ai.libs.hasco.builder.forwarddecomposition.HASCOViaFDAndBestFirstWithRandomCompletionsBuilder;
import ai.libs.hasco.core.HASCOConfig;
import ai.libs.hyperopt.api.input.IOptimizerConfig;
import ai.libs.hyperopt.api.input.IPlanningOptimizationTask;
import ai.libs.hyperopt.impl.optimizer.htn.AHTNBasedOptimizer;
import ai.libs.jaicore.basic.algorithm.AOptimizer;
import ai.libs.jaicore.components.model.RefinementConfiguredSoftwareConfigurationProblem;
import ai.libs.jaicore.components.model.SoftwareConfigurationProblem;
import ai.libs.jaicore.planning.hierarchical.algorithms.forwarddecomposition.graphgenerators.tfd.TFDNode;

public class BestFirstOptimizer<M> extends AHTNBasedOptimizer<M> {

	public static final String NAME = "bf";

	private final AOptimizer algo;

	public BestFirstOptimizer(final IOptimizerConfig config, final IPlanningOptimizationTask<M> input) {
		super(config, input);
		HASCOConfig hascoConfig = ConfigFactory.create(HASCOConfig.class);
		hascoConfig.setProperty(HASCOConfig.K_CPUS, "" + config.cpus());
		hascoConfig.setProperty(HASCOConfig.K_MEMORY, "" + config.memory());
		HASCOViaFDAndBestFirstWithRandomCompletionsBuilder builder = HASCOBuilder.get().withBestFirst().viaRandomCompletions();
		builder.withAlgorithmConfig(hascoConfig);
		builder.withSeed(0).withNumSamples(10).withPreferredNodeEvaluator(new IPathEvaluator<TFDNode, String, Double>() {
			@Override
			public Double evaluate(final ILabeledPath<TFDNode, String> path) throws PathEvaluationException, InterruptedException {
				return (path.getArcs().isEmpty()) ? 0.0 : null;
			}
		});

		SoftwareConfigurationProblem<Double> coreProblem = new SoftwareConfigurationProblem<>(input.getComponents(), input.getRequestedInterface(), input.getDirectEvaluator(this.getClass().getSimpleName()));
		builder.setProblemInput(new RefinementConfiguredSoftwareConfigurationProblem<>(coreProblem, input.getParameterRefinementConfiguration()));
		this.algo = builder.getAlgorithm();
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

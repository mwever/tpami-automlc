package ai.libs.hyperopt.impl.optimizer.htn.mcts;

import org.api4.java.ai.graphsearch.problem.IPathSearchWithPathEvaluationsInput;
import org.api4.java.ai.graphsearch.problem.implicit.graphgenerator.IPathGoalTester;
import org.api4.java.ai.graphsearch.problem.pathsearch.pathevaluation.IPathEvaluator;
import org.api4.java.datastructure.graph.implicit.IGraphGenerator;

import ai.libs.hasco.core.HASCO;
import ai.libs.hasco.core.HASCOFactory;
import ai.libs.hasco.core.RefinementConfiguredSoftwareConfigurationProblem;
import ai.libs.hasco.core.SoftwareConfigurationProblem;
import ai.libs.hasco.variants.forwarddecomposition.HASCOViaFDAndBestFirstWithRandomCompletionsFactory;
import ai.libs.hyperopt.api.input.IPlanningOptimizationTask;
import ai.libs.jaicore.planning.hierarchical.algorithms.forwarddecomposition.graphgenerators.tfd.TFDNode;

public class OptimizationSearchPathProblem<M> implements IPathSearchWithPathEvaluationsInput<TFDNode, String, Double> {

	private final String algorithmID;
	private final IPlanningOptimizationTask<M> input;
	private final IGraphGenerator<TFDNode, String> graphGenerator;

	public OptimizationSearchPathProblem(final String algorithmID, final IPlanningOptimizationTask<M> input) {
		this.algorithmID = algorithmID;
		this.input = input;

		// get graph generator
		HASCOFactory<?, TFDNode, String, Double> hascoFactory = new HASCOViaFDAndBestFirstWithRandomCompletionsFactory(0, 10);
		hascoFactory.withDefaultAlgorithmConfig();
		SoftwareConfigurationProblem<Double> coreProblem = new SoftwareConfigurationProblem<>(input.getComponents(), input.getRequestedInterface(), input.getDirectEvaluator(algorithmID));
		hascoFactory.setProblemInput(new RefinementConfiguredSoftwareConfigurationProblem<>(coreProblem, input.getParameterRefinementConfiguration()));
		HASCO<?, TFDNode, String, Double> hasco = hascoFactory.getAlgorithm();
		this.graphGenerator = hasco.getGraphGenerator();

	}

	@Override
	public IGraphGenerator<TFDNode, String> getGraphGenerator() {
		return this.graphGenerator;
	}

	@Override
	public IPathGoalTester<TFDNode, String> getGoalTester() {
		return (p) -> p.getHead().getRemainingTasks().isEmpty();
	}

	@Override
	public IPathEvaluator<TFDNode, String, Double> getPathEvaluator() {
		return new HASCOPathEvaluator(this.input.getComponents(), this.input.getDirectEvaluator(this.algorithmID));
	}

}

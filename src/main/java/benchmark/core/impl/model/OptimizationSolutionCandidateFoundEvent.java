package benchmark.core.impl.model;

import java.util.Map;

import ai.libs.jaicore.components.model.ComponentInstance;
import benchmark.core.api.output.IOptimizationOutput;
import benchmark.core.api.output.IOptimizationSolutionCandidateFoundEvent;

/**
 * Basic implementation of an optimizer solution, providing access to the optimizer solution found and its score as well as the timestamp when it has been found.
 *
 * @author mwever
 */
public class OptimizationSolutionCandidateFoundEvent<M> implements IOptimizationSolutionCandidateFoundEvent<M> {

	private final String algorithmId;
	private final IOptimizationOutput<M> output;
	private String exception = null;

	public OptimizationSolutionCandidateFoundEvent(final String algorithmId, final IOptimizationOutput<M> output) {
		this.algorithmId = algorithmId;
		this.output = output;
	}

	public OptimizationSolutionCandidateFoundEvent(final String algorithmID, final IOptimizationOutput<M> output, final String exception) {
		this(algorithmID, output);
		this.exception = exception;
	}

	@Override
	public String getAlgorithmId() {
		return this.algorithmId;
	}

	@Override
	public ComponentInstance getSolutionCandidate() {
		return this.output.getSolutionDescription();
	}

	@Override
	public M getObject() {
		return this.output.getObject();
	}

	@Override
	public IOptimizationOutput<M> getOutput() {
		return this.output;
	}

	@Override
	public Double getScore() {
		return this.output.getScore();
	}

	@Override
	public String getException() {
		return this.exception;
	}

	@Override
	public long getTimestamp() {
		return this.output.getTimestamp();
	}

	@Override
	public Map<String, ? extends Object> getEvaluationReport() {
		return this.output.getEvaluationReport();
	}

	@Override
	public long getTimeUntilFound() {
		return this.output.getTimeUntilFound();
	}

}

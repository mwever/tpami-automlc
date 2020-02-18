package ai.libs.hyperopt.impl.model;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hyperopt.api.IOptimizationSolutionCandidateFoundEvent;

/**
 * Basic implementation of an optimizer solution, providing access to the optimizer solution found and its score as well as the timestamp when it has been found.
 *
 * @author mwever
 */
public class OptimizationSolutionCandidateFoundEvent<M> implements IOptimizationSolutionCandidateFoundEvent<M> {

	private final String algorithmId;
	private final ComponentInstance solutionCandidate;
	private final M object;
	private final Double score;
	private final long timestamp;

	public OptimizationSolutionCandidateFoundEvent(final String algorithmId, final ComponentInstance solutionCandidate, final M object, final Double score) {
		this.algorithmId = algorithmId;
		this.solutionCandidate = solutionCandidate;
		this.object = object;
		this.score = score;
		this.timestamp = System.currentTimeMillis();
	}

	@Override
	public String getAlgorithmId() {
		return this.algorithmId;
	}

	@Override
	public ComponentInstance getSolutionCandidate() {
		return this.solutionCandidate;
	}

	@Override
	public M getObject() {
		return this.object;
	}

	@Override
	public Double getScore() {
		return this.score;
	}

	@Override
	public long getTimestamp() {
		return this.timestamp;
	}

}

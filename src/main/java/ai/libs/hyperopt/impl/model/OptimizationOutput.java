package ai.libs.hyperopt.impl.model;

import java.util.Map;

import ai.libs.hyperopt.api.output.IOptimizationOutput;
import ai.libs.jaicore.components.model.ComponentInstance;

public class OptimizationOutput<M> implements IOptimizationOutput<M> {

	/** The evaluable object. */
	private M object;
	/** The score obtained from the evaluator */
	private Double score;
	/** The description of the candidate in terms of a component instance. */
	private ComponentInstance solutionDescription;
	/** The timestamp when this candidate was found. */
	private long timestamp;
	/** <Optional> KVStore for storing additional annotations/information. */
	private Map<String, ? extends Object> evaluationReport = null;
	/** The time needed to find this candidate. */
	private long timeUntilFound;

	public OptimizationOutput(final long timestampOptStarted, final M object, final Double score, final ComponentInstance solutionDescription, final Map<String, ? extends Object> annotations) {
		this.object = object;
		this.score = score;
		this.solutionDescription = solutionDescription;
		this.evaluationReport = annotations;
		this.timestamp = System.currentTimeMillis();
		this.timeUntilFound = this.timestamp - timestampOptStarted;
	}

	public OptimizationOutput(final long timestamp, final long timeUntilFound, final M object, final Double score, final ComponentInstance solutionDescription, final Map<String, ? extends Object> annotations) {
		this.object = object;
		this.score = score;
		this.solutionDescription = solutionDescription;
		this.evaluationReport = annotations;
		this.timestamp = timestamp;
		this.timeUntilFound = timeUntilFound;
	}

	public OptimizationOutput(final long timestampOptStarted, final M object, final Double score, final ComponentInstance solutionDescription) {
		this(timestampOptStarted, object, score, solutionDescription, null);
	}

	@Override
	public Double getScore() {
		return this.score;
	}

	@Override
	public M getObject() {
		return this.object;
	}

	@Override
	public ComponentInstance getSolutionDescription() {
		return this.solutionDescription;
	}

	@Override
	public Map<String, ? extends Object> getEvaluationReport() {
		return this.evaluationReport;
	}

	@Override
	public long getTimestamp() {
		return this.timestamp;
	}

	@Override
	public long getTimeUntilFound() {
		return this.timeUntilFound;
	}

}

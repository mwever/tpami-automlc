package de.upb.ml2plan.event;

import java.util.Map;

import ai.libs.hyperopt.api.output.IOptimizationOutput;

public class CandidateEvaluatedEventImpl<M> implements CandidateEvaluatedEvent<M> {

	private final String threadID;
	private final IOptimizationOutput<M> optOut;
	private final int orderNo;
	private final long timestamp;

	private Map<String, Object> evaluationReport;
	private String exception;

	private CandidateEvaluatedEventImpl(final String threadID, final IOptimizationOutput<M> optOut, final int orderNo) {
		this.threadID = threadID;
		this.optOut = optOut;
		this.orderNo = orderNo;
		this.timestamp = System.currentTimeMillis();
	}

	public CandidateEvaluatedEventImpl(final String threadID, final IOptimizationOutput<M> optOut, final int orderNo, final Map<String, Object> evaluationReport) {
		this(threadID, optOut, orderNo);
		this.evaluationReport = evaluationReport;
	}

	public CandidateEvaluatedEventImpl(final String threadID, final IOptimizationOutput<M> optOut, final int orderNo, final String exception) {
		this(threadID, optOut, orderNo);
		this.exception = exception;
	}

	@Override
	public IOptimizationOutput<M> getOptimizationOutput() {
		return this.optOut;
	}

	@Override
	public Map<String, Object> getEvaluationReport() {
		return this.evaluationReport;
	}

	@Override
	public String getThreadID() {
		return this.threadID;
	}

	@Override
	public int getOrderNo() {
		return this.orderNo;
	}

	@Override
	public String getException() {
		return this.exception;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder().append(this.threadID).append(": ").append(this.orderNo);

		if (this.exception != null) {
			sb.append(" FAIL");
		} else if (this.evaluationReport != null) {
			sb.append(" ").append(this.evaluationReport);
		}

		sb.append(" ").append(this.optOut.toString());

		return sb.toString();
	}

	@Override
	public String getAlgorithmId() {
		return this.threadID;
	}

	@Override
	public long getTimestamp() {
		return this.timestamp;
	}

}

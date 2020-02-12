package de.upb.ml2plan.event;

import java.util.Map;

import ai.libs.hasco.model.ComponentInstance;

public class CandidateEvaluatedEventImpl implements CandidateEvaluatedEvent {

	private final String threadID;
	private final ComponentInstance componentInstance;
	private final int orderNo;

	private Map<String, Object> evaluationReport;
	private String exception;

	private CandidateEvaluatedEventImpl(final String threadID, final ComponentInstance componentInstance, final int orderNo) {
		this.threadID = threadID;
		this.componentInstance = componentInstance;
		this.orderNo = orderNo;
	}

	public CandidateEvaluatedEventImpl(final String threadID, final ComponentInstance componentInstance, final int orderNo, final Map<String, Object> evaluationReport) {
		this(threadID, componentInstance, orderNo);
		this.evaluationReport = evaluationReport;
	}

	public CandidateEvaluatedEventImpl(final String threadID, final ComponentInstance componentInstance, final int orderNo, final String exception) {
		this(threadID, componentInstance, orderNo);
		this.exception = exception;
	}

	@Override
	public ComponentInstance getComponentInstance() {
		return this.componentInstance;
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

}

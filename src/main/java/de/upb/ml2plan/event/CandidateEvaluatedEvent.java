package de.upb.ml2plan.event;

import java.util.Map;

import ai.libs.hasco.model.ComponentInstance;

public interface CandidateEvaluatedEvent {

	public ComponentInstance getComponentInstance();

	public Map<String, Object> getEvaluationReport();

	public String getThreadID();

	public int getOrderNo();

	public String getException();

}

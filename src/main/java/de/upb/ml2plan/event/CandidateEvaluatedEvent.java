package de.upb.ml2plan.event;

import java.util.Map;

import org.api4.java.algorithm.events.IAlgorithmEvent;

import ai.libs.hyperopt.api.output.IOptimizationOutput;

public interface CandidateEvaluatedEvent<M> extends IAlgorithmEvent {

	public IOptimizationOutput<M> getOptimizationOutput();

	public Map<String, Object> getEvaluationReport();

	public String getThreadID();

	public int getOrderNo();

	public String getException();

}

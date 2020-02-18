package ai.libs.hyperopt.impl;

import org.api4.java.algorithm.IAlgorithm;
import org.api4.java.algorithm.events.result.IScoredSolutionCandidateFoundEvent;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.jaicore.basic.algorithm.ASolutionCandidateFoundEvent;

public class PCSBasedOptimizationEvent extends ASolutionCandidateFoundEvent<ComponentInstance> implements IScoredSolutionCandidateFoundEvent<ComponentInstance, Double> {

	private ComponentInstance componentInstance;
	private Double score;
	private String algorithmId;
	private long timeStamp;

	public PCSBasedOptimizationEvent(final IAlgorithm<?, ?> algorithm, final ComponentInstance componentInstance, final Double score, final String algorithmId) {
		super(algorithm, componentInstance);
		this.componentInstance = componentInstance;
		this.score = score;
		this.algorithmId = algorithmId;
		this.timeStamp = System.currentTimeMillis();
	}

	@Override
	public String getAlgorithmId() {
		return this.algorithmId;
	}

	@Override
	public long getTimestamp() {
		return this.timeStamp;
	}

	@Override
	public Double getScore() {
		return this.score;
	}

	@Override
	public ComponentInstance getSolutionCandidate() {
		return this.componentInstance;
	}

}

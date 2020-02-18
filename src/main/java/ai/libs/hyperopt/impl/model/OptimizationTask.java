package ai.libs.hyperopt.impl.model;

import java.util.Collection;

import org.api4.java.algorithm.Timeout;
import org.api4.java.common.attributedobjects.IObjectEvaluator;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hyperopt.api.IConverter;
import ai.libs.hyperopt.api.IOptimizationTask;

public class OptimizationTask<M> implements IOptimizationTask<M> {

	private final IConverter<ComponentInstance, M> converter;
	private final IObjectEvaluator<M, Double> evaluator;

	private int numCpus;
	private Timeout globalTimeout;
	private Timeout evaluationTimeout;

	private final Collection<Component> components;
	private final String requestedInterface;

	/**
	 * Standard c'tor defining an optimization task.
	 * @param converter Converter for transforming component instances into an evaluable object.
	 * @param evaluator The evaluator for assessing the quality of an object.
	 */
	public OptimizationTask(final IConverter<ComponentInstance, M> converter, final IObjectEvaluator<M, Double> evaluator, final int numCpus, final Collection<Component> components, final String requestedInterface,
			final Timeout globalTimeout, final Timeout evaluationTimeout) {
		// Components for the execution of the optimization task
		this.converter = converter;
		this.evaluator = evaluator;

		// Technical specifications of the optimization task
		this.numCpus = numCpus;
		this.globalTimeout = globalTimeout;
		this.evaluationTimeout = evaluationTimeout;

		// Descriptive specification of the optimization task
		this.components = components;
		this.requestedInterface = requestedInterface;
	}

	@Override
	public IConverter<ComponentInstance, M> getConverter() {
		return this.converter;
	}

	@Override
	public IObjectEvaluator<M, Double> getEvaluator() {
		return this.evaluator;
	}

	@Override
	public int getNumCpus() {
		return this.numCpus;
	}

	@Override
	public Collection<Component> getComponents() {
		return this.components;
	}

	@Override
	public String getRequestedInterface() {
		return this.requestedInterface;
	}

	@Override
	public Timeout getGlobalTimeout() {
		return this.globalTimeout;
	}

	@Override
	public Timeout getEvaluationTimeout() {
		return this.evaluationTimeout;
	}

	@Override
	public void setNumCpus(final int numCpus) {
		this.numCpus = numCpus;
	}

	@Override
	public void setGlobalTimeout(final Timeout globalTimeout) {
		this.globalTimeout = globalTimeout;
	}

	@Override
	public void setEvaluationTimeout(final Timeout evaluationTimeout) {
		this.evaluationTimeout = evaluationTimeout;
	}

}

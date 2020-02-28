package ai.libs.hyperopt.impl.model;

import java.util.Collection;

import org.api4.java.algorithm.Timeout;
import org.api4.java.common.attributedobjects.IObjectEvaluator;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hyperopt.api.IConverter;
import ai.libs.hyperopt.api.input.IOptimizationTask;
import ai.libs.hyperopt.impl.evaluator.AutoConvertingObjectEvaluator;

public class OptimizationTask<M> implements IOptimizationTask<M> {

	private final IConverter<ComponentInstance, M> converter;
	private final IObjectEvaluator<M, Double> evaluator;

	private Timeout globalTimeout;
	private Timeout evaluationTimeout;

	private final Collection<Component> components;
	private final String requestedInterface;
	private IObjectEvaluator<ComponentInstance, Double> directEvaluator;

	/**
	 * Standard c'tor defining an optimization task.
	 * @param converter Converter for transforming component instances into an evaluable object.
	 * @param evaluator The evaluator for assessing the quality of an object.
	 */
	public OptimizationTask(final IConverter<ComponentInstance, M> converter, final IObjectEvaluator<M, Double> evaluator, final Collection<Component> components, final String requestedInterface, final Timeout globalTimeout,
			final Timeout evaluationTimeout) {
		// Components for the execution of the optimization task
		this.converter = converter;
		this.evaluator = evaluator;

		// Technical specifications of the optimization task
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
	public IObjectEvaluator<ComponentInstance, Double> getDirectEvaluator(final String algorithmID) {
		if (this.directEvaluator == null) {
			this.directEvaluator = new AutoConvertingObjectEvaluator<>(algorithmID, this.converter, this.evaluator);
		}
		return this.directEvaluator;
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
	public void setGlobalTimeout(final Timeout globalTimeout) {
		this.globalTimeout = globalTimeout;
	}

	@Override
	public void setEvaluationTimeout(final Timeout evaluationTimeout) {
		this.evaluationTimeout = evaluationTimeout;
	}

}

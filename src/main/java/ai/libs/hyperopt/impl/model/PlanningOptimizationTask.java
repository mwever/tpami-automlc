package ai.libs.hyperopt.impl.model;

import java.util.Collection;
import java.util.Map;

import org.api4.java.algorithm.Timeout;
import org.api4.java.common.attributedobjects.IObjectEvaluator;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.model.Parameter;
import ai.libs.hasco.model.ParameterRefinementConfiguration;
import ai.libs.hyperopt.api.IConverter;
import ai.libs.hyperopt.api.input.IPlanningOptimizationTask;

public class PlanningOptimizationTask<M> extends OptimizationTask<M> implements IPlanningOptimizationTask<M> {

	private final Map<Component, Map<Parameter, ParameterRefinementConfiguration>> parameterRefinementConfiguration;

	/**
	 * Standard c'tor defining an optimization task.
	 * @param converter Converter for transforming component instances into an evaluable object.
	 * @param evaluator The evaluator for assessing the quality of an object.
	 */
	public PlanningOptimizationTask(final IConverter<ComponentInstance, M> converter, final IObjectEvaluator<M, Double> evaluator, final Collection<Component> components, final String requestedInterface, final Timeout globalTimeout,
			final Timeout evaluationTimeout, final Map<Component, Map<Parameter, ParameterRefinementConfiguration>> parameterRefinementConfiguration) {
		super(converter, evaluator, components, requestedInterface, globalTimeout, evaluationTimeout);
		this.parameterRefinementConfiguration = parameterRefinementConfiguration;
	}

	@Override
	public Map<Component, Map<Parameter, ParameterRefinementConfiguration>> getParameterRefinementConfiguration() {
		return this.parameterRefinementConfiguration;
	}

}

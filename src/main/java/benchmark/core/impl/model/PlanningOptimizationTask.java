package benchmark.core.impl.model;

import java.util.Collection;
import java.util.Map;

import org.api4.java.algorithm.Timeout;

import ai.libs.jaicore.components.model.Component;
import ai.libs.jaicore.components.model.ComponentInstance;
import ai.libs.jaicore.components.model.Parameter;
import ai.libs.jaicore.components.model.ParameterRefinementConfiguration;
import benchmark.core.api.IConverter;
import benchmark.core.api.IHyperoptObjectEvaluator;
import benchmark.core.api.input.IPlanningOptimizationTask;

public class PlanningOptimizationTask<M> extends OptimizationTask<M> implements IPlanningOptimizationTask<M> {

	private final Map<Component, Map<Parameter, ParameterRefinementConfiguration>> parameterRefinementConfiguration;

	/**
	 * Standard c'tor defining an optimization task.
	 * @param converter Converter for transforming component instances into an evaluable object.
	 * @param evaluator The evaluator for assessing the quality of an object.
	 */
	public PlanningOptimizationTask(final IConverter<ComponentInstance, M> converter, final IHyperoptObjectEvaluator<M> evaluator, final Collection<Component> components, final String requestedInterface, final Timeout globalTimeout,
			final Timeout evaluationTimeout, final Map<Component, Map<Parameter, ParameterRefinementConfiguration>> parameterRefinementConfiguration) {
		super(converter, evaluator, components, requestedInterface, globalTimeout, evaluationTimeout);
		this.parameterRefinementConfiguration = parameterRefinementConfiguration;
	}

	@Override
	public Map<Component, Map<Parameter, ParameterRefinementConfiguration>> getParameterRefinementConfiguration() {
		return this.parameterRefinementConfiguration;
	}

}

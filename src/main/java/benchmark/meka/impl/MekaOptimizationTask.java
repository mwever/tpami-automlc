package benchmark.meka.impl;

import java.util.Collection;
import java.util.Map;

import org.api4.java.algorithm.Timeout;

import ai.libs.jaicore.components.model.Component;
import ai.libs.jaicore.components.model.ComponentInstance;
import ai.libs.jaicore.components.model.Parameter;
import ai.libs.jaicore.components.model.ParameterRefinementConfiguration;
import ai.libs.jaicore.ml.classification.multilabel.learner.IMekaClassifier;
import benchmark.core.api.IConverter;
import benchmark.core.api.IHyperoptObjectEvaluator;
import benchmark.core.api.input.IPlanningOptimizationTask;
import benchmark.core.impl.model.PlanningOptimizationTask;

public class MekaOptimizationTask extends PlanningOptimizationTask<IMekaClassifier> implements IPlanningOptimizationTask<IMekaClassifier> {

	public MekaOptimizationTask(final IConverter<ComponentInstance, IMekaClassifier> converter, final IHyperoptObjectEvaluator<IMekaClassifier> evaluator, final Collection<Component> components, final String requestedInterface,
			final Timeout globalTimeout, final Timeout evaluationTimeout, final Map<Component, Map<Parameter, ParameterRefinementConfiguration>> parameterRefinementConfiguration) {
		super(converter, evaluator, components, requestedInterface, globalTimeout, evaluationTimeout, parameterRefinementConfiguration);
	}

}

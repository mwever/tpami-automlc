package ai.libs.hyperopt.example.meka;

import java.util.Collection;
import java.util.Map;

import org.api4.java.algorithm.Timeout;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.model.Parameter;
import ai.libs.hasco.model.ParameterRefinementConfiguration;
import ai.libs.hyperopt.api.IConverter;
import ai.libs.hyperopt.api.IHyperoptObjectEvaluator;
import ai.libs.hyperopt.api.input.IPlanningOptimizationTask;
import ai.libs.hyperopt.impl.model.PlanningOptimizationTask;
import ai.libs.jaicore.ml.classification.multilabel.learner.IMekaClassifier;

public class MekaOptimizationTask extends PlanningOptimizationTask<IMekaClassifier> implements IPlanningOptimizationTask<IMekaClassifier> {

	public MekaOptimizationTask(final IConverter<ComponentInstance, IMekaClassifier> converter, final IHyperoptObjectEvaluator<IMekaClassifier> evaluator, final Collection<Component> components, final String requestedInterface,
			final Timeout globalTimeout, final Timeout evaluationTimeout, final Map<Component, Map<Parameter, ParameterRefinementConfiguration>> parameterRefinementConfiguration) {
		super(converter, evaluator, components, requestedInterface, globalTimeout, evaluationTimeout, parameterRefinementConfiguration);
	}

}

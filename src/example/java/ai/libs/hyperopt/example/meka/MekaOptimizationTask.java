package ai.libs.hyperopt.example.meka;

import java.util.Collection;

import org.api4.java.algorithm.Timeout;
import org.api4.java.common.attributedobjects.IObjectEvaluator;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hyperopt.api.IConverter;
import ai.libs.hyperopt.api.IOptimizationTask;
import ai.libs.hyperopt.impl.model.OptimizationTask;
import ai.libs.jaicore.ml.classification.multilabel.learner.IMekaClassifier;

public class MekaOptimizationTask extends OptimizationTask<IMekaClassifier> implements IOptimizationTask<IMekaClassifier> {

	public MekaOptimizationTask(final IConverter<ComponentInstance, IMekaClassifier> converter, final IObjectEvaluator<IMekaClassifier, Double> evaluator, final int numCpus, final Collection<Component> components,
			final String requestedInterface, final Timeout globalTimeout, final Timeout evaluationTimeout) {
		super(converter, evaluator, numCpus, components, requestedInterface, globalTimeout, evaluationTimeout);
	}

}

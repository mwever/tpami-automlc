package ai.libs.hyperopt.example.meka;

import org.api4.java.common.attributedobjects.IObjectEvaluator;
import org.api4.java.common.attributedobjects.ObjectEvaluationFailedException;

import ai.libs.jaicore.ml.classification.multilabel.learner.IMekaClassifier;

public class MekaClassifierEvaluator implements IObjectEvaluator<IMekaClassifier, Double> {

	@Override
	public Double evaluate(final IMekaClassifier object) throws InterruptedException, ObjectEvaluationFailedException {
		return 0.0;
	}

}

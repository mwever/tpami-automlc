package ai.libs.hyperopt.example.meka;

import ai.libs.hyperopt.api.ConversionFailedException;
import ai.libs.hyperopt.api.IConverter;
import ai.libs.jaicore.components.exceptions.ComponentInstantiationFailedException;
import ai.libs.jaicore.components.model.ComponentInstance;
import ai.libs.jaicore.ml.classification.multilabel.learner.IMekaClassifier;
import ai.libs.mlplan.multilabel.mekamlplan.MekaPipelineFactory;

public class MekaClassifierConverter implements IConverter<ComponentInstance, IMekaClassifier> {

	private final MekaPipelineFactory factory = new MekaPipelineFactory();

	public MekaClassifierConverter() {

	}

	@Override
	public IMekaClassifier convert(final ComponentInstance source) throws ConversionFailedException {
		try {
			return this.factory.getComponentInstantiation(source);
		} catch (ComponentInstantiationFailedException e) {
			throw new ConversionFailedException(e);
		}
	}

}

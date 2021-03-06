package benchmark.meka.impl;

import ai.libs.jaicore.components.exceptions.ComponentInstantiationFailedException;
import ai.libs.jaicore.components.model.ComponentInstance;
import ai.libs.jaicore.ml.classification.multilabel.learner.IMekaClassifier;
import ai.libs.mlplan.multilabel.mekamlplan.MekaPipelineFactory;
import benchmark.core.api.ConversionFailedException;
import benchmark.core.api.IConverter;

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

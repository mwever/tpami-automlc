package ai.libs.hyperopt.example.weka;

import ai.libs.hasco.exceptions.ComponentInstantiationFailedException;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hyperopt.api.ConversionFailedException;
import ai.libs.hyperopt.api.IConverter;
import ai.libs.jaicore.ml.weka.classification.learner.IWekaClassifier;
import ai.libs.mlplan.multiclass.wekamlplan.weka.WekaPipelineFactory;

public class WekaClassifierConverter implements IConverter<ComponentInstance, IWekaClassifier> {

	private final WekaPipelineFactory factory = new WekaPipelineFactory();

	@Override
	public IWekaClassifier convert(final ComponentInstance source) throws ConversionFailedException {
		try {
			return this.factory.getComponentInstantiation(source);
		} catch (ComponentInstantiationFailedException e) {
			throw new ConversionFailedException(e);
		}
	}

}

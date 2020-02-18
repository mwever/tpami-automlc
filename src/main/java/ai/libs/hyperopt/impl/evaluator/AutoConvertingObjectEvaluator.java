package ai.libs.hyperopt.impl.evaluator;

import org.api4.java.common.attributedobjects.IObjectEvaluator;
import org.api4.java.common.attributedobjects.ObjectEvaluationFailedException;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hyperopt.api.ConversionFailedException;
import ai.libs.hyperopt.api.IComponentInstanceEvaluator;
import ai.libs.hyperopt.api.IConverter;
import ai.libs.hyperopt.impl.model.OptimizationSolutionCandidateFoundEvent;
import de.upb.ml2plan.AListenable;

public class AutoConvertingObjectEvaluator<M> extends AListenable implements IComponentInstanceEvaluator {

	private IConverter<ComponentInstance, M> converter;
	private IObjectEvaluator<M, Double> evaluator;

	public AutoConvertingObjectEvaluator(final IConverter<ComponentInstance, M> converter, final IObjectEvaluator<M, Double> evaluator) {
		this.converter = converter;
		this.evaluator = evaluator;
	}

	@Override
	public Double evaluate(final ComponentInstance source) throws InterruptedException, ObjectEvaluationFailedException {
		try {
			M convertedObject = this.converter.convert(source);
			double res = this.evaluator.evaluate(convertedObject);
			this.getEventBus().post(new OptimizationSolutionCandidateFoundEvent<M>(this.toString(), source, convertedObject, res));
			return res;
		} catch (ConversionFailedException e) {
			throw new ObjectEvaluationFailedException(e);
		}
	}

	@Override
	public String toString() {
		return new StringBuilder(this.getClass().getName()).append("(").append(this.converter.getClass().getName()).append(",").append(this.evaluator.getClass().getName()).append(")").toString();
	}

}

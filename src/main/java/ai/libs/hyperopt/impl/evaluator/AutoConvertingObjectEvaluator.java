package ai.libs.hyperopt.impl.evaluator;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.api4.java.common.attributedobjects.IObjectEvaluator;
import org.api4.java.common.attributedobjects.ObjectEvaluationFailedException;
import org.codehaus.plexus.util.ExceptionUtils;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hyperopt.api.ConversionFailedException;
import ai.libs.hyperopt.api.IComponentInstanceEvaluator;
import ai.libs.hyperopt.api.IConverter;
import ai.libs.hyperopt.api.ILoggingObjectEvaluator;
import ai.libs.hyperopt.api.output.IOptimizationOutput;
import ai.libs.hyperopt.impl.model.OptimizationOutput;
import ai.libs.hyperopt.impl.model.OptimizationSolutionCandidateFoundEvent;
import de.upb.ml2plan.AListenable;

public class AutoConvertingObjectEvaluator<M> extends AListenable implements IComponentInstanceEvaluator {

	private final String algorithmID;
	private IConverter<ComponentInstance, M> converter;
	private IObjectEvaluator<M, Double> evaluator;

	public AutoConvertingObjectEvaluator(final String algorithmID, final IConverter<ComponentInstance, M> converter, final IObjectEvaluator<M, Double> evaluator) {
		this.algorithmID = algorithmID;
		this.converter = converter;
		this.evaluator = evaluator;
	}

	@Override
	public Double evaluate(final ComponentInstance source) throws InterruptedException, ObjectEvaluationFailedException {
		M convertedObject = null;
		try {
			convertedObject = this.converter.convert(source);
		} catch (ConversionFailedException e) {
			this.getEventBus().post(new OptimizationSolutionCandidateFoundEvent<M>(this.algorithmID, new OptimizationOutput<M>(null, null, source), ExceptionUtils.getFullStackTrace(e)));
			throw new ObjectEvaluationFailedException(e);
		}

		Map<String, DescriptiveStatistics> log = new HashMap<>();
		Double score = null;
		try {

			if (this.evaluator instanceof ILoggingObjectEvaluator) {
				score = ((ILoggingObjectEvaluator<M, Double>) this.evaluator).evaluate(convertedObject, log);
			} else {
				score = this.evaluator.evaluate(convertedObject);
			}

			IOptimizationOutput<M> output = new OptimizationOutput<M>(convertedObject, score, source, log);
			this.getEventBus().post(new OptimizationSolutionCandidateFoundEvent<M>(this.algorithmID, output));
			return score;
		} catch (ObjectEvaluationFailedException e) {
			IOptimizationOutput<M> output = new OptimizationOutput<M>(convertedObject, score, source, log);
			this.getEventBus().post(new OptimizationSolutionCandidateFoundEvent<M>(this.algorithmID, output, ExceptionUtils.getFullStackTrace(e)));
			throw e;
		}
	}

	@Override
	public String toString() {
		return new StringBuilder(this.getClass().getName()).append("(").append(this.converter.getClass().getName()).append(",").append(this.evaluator.getClass().getName()).append(")").toString();
	}

}

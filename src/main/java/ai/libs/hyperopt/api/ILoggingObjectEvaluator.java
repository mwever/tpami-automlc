package ai.libs.hyperopt.api;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.api4.java.common.attributedobjects.IObjectEvaluator;
import org.api4.java.common.attributedobjects.ObjectEvaluationFailedException;

public interface ILoggingObjectEvaluator<T> extends IObjectEvaluator<T, Double>, IHyperoptObjectEvaluator<T> {
	
	@Override
	default Double evaluate(final T object, final int budget) throws ObjectEvaluationFailedException, InterruptedException {
		return this.evaluate(object,new HashMap<>(), budget);
	}

	default Double evaluate(final T object, final Map<String, DescriptiveStatistics> log) throws ObjectEvaluationFailedException, InterruptedException {
		return this.evaluate(object,log,this.getMaxBudget());
	}

	public Double evaluate(final T object, final Map<String, DescriptiveStatistics> log, int budget) throws ObjectEvaluationFailedException, InterruptedException;
}

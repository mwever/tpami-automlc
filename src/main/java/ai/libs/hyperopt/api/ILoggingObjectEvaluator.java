package ai.libs.hyperopt.api;

import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.api4.java.common.attributedobjects.IObjectEvaluator;
import org.api4.java.common.attributedobjects.ObjectEvaluationFailedException;

public interface ILoggingObjectEvaluator<T, V extends Comparable<V>> extends IObjectEvaluator<T, V> {

	public double evaluate(T object, Map<String, DescriptiveStatistics> log) throws ObjectEvaluationFailedException, InterruptedException;

}

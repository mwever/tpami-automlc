package benchmark.core.api.output;

import java.util.Map;

import org.api4.java.common.attributedobjects.ScoredItem;

import ai.libs.jaicore.components.model.ComponentInstance;

public interface IOptimizationOutput<M> extends ScoredItem<Double> {

	public M getObject();

	public ComponentInstance getSolutionDescription();

	public Map<String, ? extends Object> getEvaluationReport();

	public long getTimestamp();

	public long getTimeUntilFound();

}

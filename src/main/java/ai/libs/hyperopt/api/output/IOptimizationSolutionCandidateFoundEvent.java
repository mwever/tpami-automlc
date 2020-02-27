package ai.libs.hyperopt.api.output;

import java.util.Map;

import org.api4.java.algorithm.events.result.IScoredSolutionCandidateFoundEvent;
import org.api4.java.common.attributedobjects.ScoredItem;

import ai.libs.hasco.model.ComponentInstance;

/**
 * The interface for describing a solution found by an optimizer.
 *
 * @author mwever
 */
public interface IOptimizationSolutionCandidateFoundEvent<M> extends IScoredSolutionCandidateFoundEvent<ComponentInstance, Double>, ScoredItem<Double> {

	public M getObject();

	public String getException();

	public Map<String, ? extends Object> getEvaluationReport();

}

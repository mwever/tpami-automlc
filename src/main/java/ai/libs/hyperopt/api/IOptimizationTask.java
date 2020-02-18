package ai.libs.hyperopt.api;

import java.util.Collection;

import org.api4.java.algorithm.Timeout;
import org.api4.java.common.attributedobjects.IObjectEvaluator;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hyperopt.impl.evaluator.AutoConvertingObjectEvaluator;

/**
 * This class describes a black box optimization task, where an evaluable object of type M needs to be optimized.
 *
 * @author mwever
 *
 * @param <M> The type of black box model that is to be optimized.
 */
public interface IOptimizationTask<M> {

	/**
	 * @return An evaluator for assessing an instantiation's quality.
	 */
	public IObjectEvaluator<M, Double> getEvaluator();

	/**
	 * @return The converter translating a ComponentInstance into a representation accessible by the evaluator.
	 */
	public IConverter<ComponentInstance, M> getConverter();

	/**
	 * @return An object evaluator combining the given converter and evaluator into an automatically converting object evaluator.
	 */
	default IObjectEvaluator<ComponentInstance, Double> getDirectEvaluator() {
		return new AutoConvertingObjectEvaluator<M>(this.getConverter(), this.getEvaluator());
	}

	/**
	 * @return The number of cpus to use for parallellization.
	 */
	public int getNumCpus();

	/**
	 * Sets the number of cpus to the provided value.
	 *
	 * @param numCpus The number of cpus to be used.
	 */
	public void setNumCpus(final int numCpus);

	/**
	 * @return The collection of components to work with in this optimization task.
	 */
	public Collection<Component> getComponents();

	/**
	 * @return The name of the requested interface.
	 */
	public String getRequestedInterface();

	/**
	 * @return The timeout for the entire optimization task.
	 */
	public Timeout getGlobalTimeout();

	/**
	 * Sets the global timeout to the provided value.
	 * @param globalTimeout timeout The value for the global timeout.
	 */
	public void setGlobalTimeout(final Timeout globalTimeout);

	/**
	 * @return The timeout for a single candidate evaluation.
	 */
	public Timeout getEvaluationTimeout();

	/**
	 * Sets the timeout for a single candidate evaluation.
	 *
	 * @param evaluationTimeout The value for the single candidate evaluation timeout.
	 */
	public void setEvaluationTimeout(Timeout evaluationTimeout);

}

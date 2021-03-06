package benchmark.core.api.input;

import java.util.Collection;

import org.api4.java.algorithm.Timeout;
import org.api4.java.common.attributedobjects.IObjectEvaluator;

import ai.libs.jaicore.components.model.Component;
import ai.libs.jaicore.components.model.ComponentInstance;
import ai.libs.jaicore.components.model.SoftwareConfigurationProblem;
import benchmark.core.api.IConverter;
import benchmark.core.api.IHyperoptObjectEvaluator;
import benchmark.core.impl.evaluator.AutoConvertingObjectEvaluator;

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
	public IHyperoptObjectEvaluator<M> getEvaluator();

	/**
	 * @return The converter translating a ComponentInstance into a representation accessible by the evaluator.
	 */
	public IConverter<ComponentInstance, M> getConverter();

	/**
	 * @param algorithmID The id/name of the algorithm being applied to the problem, can also be an empty string.
	 * @return An object evaluator combining the given converter and evaluator into an automatically converting object evaluator.
	 */
	default IObjectEvaluator<ComponentInstance, Double> getDirectEvaluator(final String algorithmID) {
		return new AutoConvertingObjectEvaluator<M>(algorithmID, this.getConverter(), this.getEvaluator());
	}

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

	/**
	 * @param algorithmID The id/name of the algorithm being applied to the problem, can also be an empty string.
	 * @return The software configuration problem description according to this optimization task.
	 */
	default SoftwareConfigurationProblem<Double> getSoftwareConfigurationProblem(final String algorithmID) {
		return new SoftwareConfigurationProblem<>(this.getComponents(), this.getRequestedInterface(), this.getDirectEvaluator(algorithmID));
	}

}

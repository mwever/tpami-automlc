package ai.libs.hyperopt.api;

import org.api4.java.algorithm.IAlgorithm;

import ai.libs.hyperopt.api.output.IOptimizationOutput;
import ai.libs.jaicore.components.model.ComponentInstance;

public interface IOptimizer<T, M> extends IAlgorithm<T, IOptimizationOutput<M>> {

	public ComponentInstance getResultAsComponentInstance();

	public M getResult();

	public String getName();

}

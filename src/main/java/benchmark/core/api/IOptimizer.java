package benchmark.core.api;

import org.api4.java.algorithm.IAlgorithm;

import ai.libs.jaicore.components.model.ComponentInstance;
import benchmark.core.api.output.IOptimizationOutput;

public interface IOptimizer<T, M> extends IAlgorithm<T, IOptimizationOutput<M>> {

	public ComponentInstance getResultAsComponentInstance();

	public M getResult();

	public String getName();

}

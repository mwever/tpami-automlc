package ai.libs.hyperopt.api;

import org.api4.java.algorithm.IAlgorithm;

import ai.libs.hasco.model.ComponentInstance;

public interface IOptimizer<M> extends IAlgorithm<IOptimizationTask<M>, M> {

	public ComponentInstance getResultAsComponentInstance();

	public M getResult();

}

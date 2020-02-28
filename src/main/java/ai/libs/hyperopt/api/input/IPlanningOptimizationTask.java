package ai.libs.hyperopt.api.input;

import java.util.Map;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.Parameter;
import ai.libs.hasco.model.ParameterRefinementConfiguration;

public interface IPlanningOptimizationTask<M> extends IOptimizationTask<M> {

	public Map<Component, Map<Parameter, ParameterRefinementConfiguration>> getParameterRefinementConfiguration();

}

package ai.libs.hyperopt.api.input;

import java.util.Map;

import ai.libs.jaicore.components.model.Component;
import ai.libs.jaicore.components.model.Parameter;
import ai.libs.jaicore.components.model.ParameterRefinementConfiguration;

public interface IPlanningOptimizationTask<M> extends IOptimizationTask<M> {

	public Map<Component, Map<Parameter, ParameterRefinementConfiguration>> getParameterRefinementConfiguration();

}

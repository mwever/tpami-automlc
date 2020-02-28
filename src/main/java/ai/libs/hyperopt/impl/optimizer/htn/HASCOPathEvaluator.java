package ai.libs.hyperopt.impl.optimizer.htn;

import java.util.Collection;

import org.api4.java.ai.graphsearch.problem.pathsearch.pathevaluation.IPathEvaluator;
import org.api4.java.ai.graphsearch.problem.pathsearch.pathevaluation.PathEvaluationException;
import org.api4.java.common.attributedobjects.IObjectEvaluator;
import org.api4.java.common.attributedobjects.ObjectEvaluationFailedException;
import org.api4.java.datastructure.graph.ILabeledPath;

import ai.libs.hasco.core.Util;
import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.jaicore.planning.hierarchical.algorithms.forwarddecomposition.graphgenerators.tfd.TFDNode;

public class HASCOPathEvaluator implements IPathEvaluator<TFDNode, String, Double> {

	private final Collection<Component> components;
	private final IObjectEvaluator<ComponentInstance, Double> evaluator;

	public HASCOPathEvaluator(final Collection<Component> components, final IObjectEvaluator<ComponentInstance, Double> evaluator) {
		this.components = components;
		this.evaluator = evaluator;
	}

	@Override
	public Double evaluate(final ILabeledPath<TFDNode, String> p) throws PathEvaluationException, InterruptedException {
		ComponentInstance ci = Util.getSolutionCompositionFromState(this.components, p.getNodes().get(p.getNodes().size() - 1).getState(), true);
		try {
			return this.evaluator.evaluate(ci);
		} catch (ObjectEvaluationFailedException e) {
			throw new PathEvaluationException("Could not evaluate component instance " + Util.getComponentNamesOfComposition(ci), e);
		}
	}

}

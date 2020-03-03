package ai.libs.hyperopt.api;

import org.api4.java.common.attributedobjects.IObjectEvaluator;
import org.api4.java.common.attributedobjects.ObjectEvaluationFailedException;

public interface IHyperoptObjectEvaluator<M> extends IObjectEvaluator<M, Double> { 

	public Double evaluate(M source, int budget) throws ObjectEvaluationFailedException, InterruptedException;
	
	public int getMaxBudget();
	
}

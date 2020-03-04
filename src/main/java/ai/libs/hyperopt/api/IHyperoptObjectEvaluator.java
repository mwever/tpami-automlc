package ai.libs.hyperopt.api;

import org.api4.java.common.attributedobjects.IObjectEvaluator;
import org.api4.java.common.attributedobjects.ObjectEvaluationFailedException;

public interface IHyperoptObjectEvaluator<M> extends IObjectEvaluator<M, Double> { 

	@Override
	default Double evaluate(final M source) throws ObjectEvaluationFailedException, InterruptedException {
		return this.evaluate(source, this.getMaxBudget());
	}
	
	public Double evaluate(M source, int budget) throws ObjectEvaluationFailedException, InterruptedException;
	
	public int getMaxBudget();
	
}

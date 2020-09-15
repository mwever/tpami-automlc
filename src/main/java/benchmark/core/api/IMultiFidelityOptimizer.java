package benchmark.core.api;

public interface IMultiFidelityOptimizer {

	/**
	 * Sets the minimum budget.
	 * @param minBudget The minimum budget.
	 */
	public void setMinBudget(double minBudget);

	/**
	 * @return The minimum budget.
	 */
	public double getMinBudget();

	/**
	 * Sets the maximum budget.
	 * @param maxBudget The maximum budget.
	 */
	public void setMaxBudget(double maxBudget);

	/**
	 * @return The maximum budget.
	 */
	public double getMaxBudget();

	/**
	 * Sets the number of iterations.
	 * @param numIterations The number of iterations.
	 */
	public void setNumIterations(int numIterations);

	/**
	 * @return The number of iterations.
	 */
	public int getNumIterations();
}

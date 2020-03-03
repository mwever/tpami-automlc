package ai.libs.hyperopt.impl.optimizer.pcs;

public interface HyperbandLikeOptimizerConfig extends IPCSBasedOptimizerConfig {

	public static final String K_EXEC_PATH = "hbl.execution_path";
	public static final String K_MIN_BUDGET = "hbl.budget.min";
	public static final String K_MAX_BUDGET = "hbl.budget.max";
	public static final String K_N_ITERATIONS = "hbl.budget.iterations";

	public String getExecutionPath();

	public Double getMinBudget();

	public Double getMaxBudget();

	public Integer getNIterations();

}

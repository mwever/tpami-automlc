package ai.libs.hyperopt.impl.optimizer.smac;

import ai.libs.hyperopt.impl.pcs.IPCSBasedOptimizerConfig;

public interface SMACOptimizerConfig extends IPCSBasedOptimizerConfig {

	public static final String K_SMAC_EXEC_PATH = "smac.execution_path";
	public static final String K_SMAC_NUM_THREADS = "smac.num_threads";

	public static final String K_SMAC_CFG_GRPC_PORT = "smac.cfg.grpcPort";
	public static final String K_SMAC_CFG_ALGO_RUNS_TIMELIMIT = "smac.cfg.algo_runs_time_limit";
	public static final String K_SMAC_CFG_ALWAYS_RACE_DEFAULT = "smac.cfg.always_race_default";
	public static final String K_SMAC_CFG_COST_FOR_CRASH = "smac.cfg.cost_for_crash";
	public static final String K_SMAC_CFG_CUTOFF = "smac.cfg.cutoff";
	public static final String K_SMAC_CFG_DETERMINISTIC = "smac.cfg.deterministic";
	public static final String K_SMAC_CFG_MEMORY_LIMIT = "smac.cfg.memory_limit";
	public static final String K_SMAC_CFG_OVERALL_OBJ = "smac.cfg.overall_obj";
	public static final String K_SMAC_CFG_RUN_OBJ = "smac.cfg.run_obj";
	public static final String K_SMAC_CFG_RUN_COUNT_LIMIT = "smac.cfg.run_count_limit";
	public static final String K_SMAC_CFG_WALL_CLOCK_LIMIT = "smac.cfg.wall_clock_limit";
	public static final String K_SMAC_CFG_PARAM_FILE = "smac.cfg.paramfile";
	public static final String K_SMAC_CFG_ALGO = "smac.cfg.algo";

	@Key(K_SMAC_EXEC_PATH)
	public String getExecutionPath();

	@Key(K_SMAC_NUM_THREADS)
	@DefaultValue("1")
	public Integer getNumThreads();

	@Key(K_SMAC_CFG_PARAM_FILE)
	@DefaultValue("searchspace.pcs")
	public String getParamFile();

	@Key(K_SMAC_CFG_ALGO_RUNS_TIMELIMIT)
	public Integer getAlgoRunsTimeLimit();

	@Key(K_SMAC_CFG_ALWAYS_RACE_DEFAULT)
	public Integer getAlwaysRaceDefault();

	@Key(K_SMAC_CFG_COST_FOR_CRASH)
	@DefaultValue("2147483647.0")
	public Double getCostForCrash();

	@Key(K_SMAC_CFG_CUTOFF)
	public Double getCutoff();

	@Key(K_SMAC_CFG_DETERMINISTIC)
	@DefaultValue("true")
	public Integer getDeterministic();

	@Key(K_SMAC_CFG_MEMORY_LIMIT)
	public Integer getMemoryLimit();

	@Key(K_SMAC_CFG_OVERALL_OBJ)
	public String getOverallObj();

	@Key(K_SMAC_CFG_RUN_OBJ)
	@DefaultValue("quality")
	public String getRunObj();
	
	@Key(K_SMAC_CFG_ALGO)
	@DefaultValue("python SMACOptimizerClient.py")
	public String getAlgo();

	@Key(K_SMAC_CFG_RUN_COUNT_LIMIT)
	public Integer getRunCountLimit();

	@Key(K_SMAC_CFG_WALL_CLOCK_LIMIT)
	public Double getWallClockLimit();

}

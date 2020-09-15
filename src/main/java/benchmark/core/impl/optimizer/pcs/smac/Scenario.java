package benchmark.core.impl.optimizer.pcs.smac;

import ai.libs.jaicore.basic.IOwnerBasedAlgorithmConfig;

public interface Scenario extends IOwnerBasedAlgorithmConfig {

	public static final String K_ABORT_ON_FIRST_CRASH = "abort_on_first_crash";
	public static final String K_ALGO = "algo";
	public static final String ALWAYS_RACE_DEFAULT = "always_race_default";
	public static final String K_COST_FOR_CRASH = "cost_for_crash";
	public static final String K_CUTOFF_TIME = "cutoff_time";
	public static final String K_DETERMINISTIC = "deterministic";
	public static final String K_INPUT_PSMAC_DIRS = "input_psmac_dirs";
	public static final String K_MAXR = "maxR";
	public static final String K_MINR = "minR";
	public static final String K_SHARED_MODEL = "shared_model";
	public static final String K_WALLCLOCK_LIMIT = "wallclock_limit";

	/**
	 * If true, SMAC will abort if the first run of the target algorithm crashes. Default: True.
	 */
	@Key(K_ABORT_ON_FIRST_CRASH)
	@DefaultValue("true")
	public boolean getAbortOnFirstRunCrash();

	/**
	 * Specifies the target algorithm call that SMAC will optimize. Interpreted as a bash-command.
	 */
	@Key(K_ALGO)
	public String getAlgo();

	/**
	 * Race new incumbents
	 */
	@Key(ALWAYS_RACE_DEFAULT)
	public String getAlwaysRaceDefault();

	/**
	 * cutoff_time:	Maximum runtime, after which the target algorithm is cancelled. Required if *run_obj* is runtime.
	 */
	@Key(K_CUTOFF_TIME)
	public String getCutoffTime();

	/**
	 * cost_for_crash:	Defines the cost-value for crashed runs on scenarios with quality as run-obj. Default: 2147483647.0
	 */
	@Key(K_COST_FOR_CRASH)
	@DefaultValue("2147483647.0")
	public Double getCostForCrash();

	@Key(K_DETERMINISTIC)
	@DefaultValue("true")
	public Boolean getDeterministic();

	@Key(K_INPUT_PSMAC_DIRS)
	public String getInputPsmacDirs();

	@Key(K_MAXR)
	@DefaultValue("1")
	public String getMaxR();

	@Key(K_MINR)
	@DefaultValue("1")
	public String getMinR();

	@Key(K_SHARED_MODEL)
	@DefaultValue("false")
	public Boolean getSharedModel();

	/**
	 * Maximum amount of wallclock-time used for optimization. Default: inf.
	 */
	@Key(K_WALLCLOCK_LIMIT)
	@DefaultValue("inf")
	public String getWallclockLimit();

}

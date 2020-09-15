package benchmark.core.impl.optimizer.pcs;

import ai.libs.jaicore.basic.IOwnerBasedAlgorithmConfig;

public interface Scenario extends IOwnerBasedAlgorithmConfig {

	public static final String K_ABORT_ON_FIRST_CRASH = "abort_on_first_crash";
	
	/**
	 * 
	 */
	public boolean getAbortOnFirstRunCrash();
	
	
}

package benchmark.core.api.input;

import java.io.File;

import ai.libs.jaicore.basic.IOwnerBasedAlgorithmConfig;

public interface IOptimizerConfig extends IOwnerBasedAlgorithmConfig {

	public static final String K_WORKING_DIR = "workingDirectory";

	@Key(K_WORKING_DIR)
	@DefaultValue("working_dir")
	public File getWorkingDir();

}

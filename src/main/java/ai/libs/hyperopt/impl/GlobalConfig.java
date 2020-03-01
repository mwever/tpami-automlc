package ai.libs.hyperopt.impl;

import java.io.File;
import java.util.Arrays;

import ai.libs.jaicore.basic.sets.SetUtil;

public class GlobalConfig {

	public static final String OPTIMIZER_CONFIG_PATH = "conf/smac-optimizer-config.properties";

//	public static final String PYTHON_EXEC = SetUtil.implode(Arrays.asList("C:" + File.separator, "Users", "Marcel", "AppData", "Local", "Programs", "Python", "Python38-32", "python.exe"), File.separator);
	public static final String PYTHON_EXEC = "python";

}

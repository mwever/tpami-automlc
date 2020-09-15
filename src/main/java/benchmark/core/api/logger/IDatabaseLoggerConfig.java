package benchmark.core.api.logger;

import ai.libs.jaicore.db.IDatabaseConfig;

public interface IDatabaseLoggerConfig extends IDatabaseConfig {

	public static final String K_LOG_TABLE = "candidate_eval_table";

	@Key(K_LOG_TABLE)
	public String getLogTable();

}

package ai.libs.hyperopt;

import java.io.File;

import org.aeonbits.owner.ConfigFactory;
import org.junit.Test;

import ai.libs.hyperopt.impl.optimizer.pcs.smac.SMACOptimizerConfig;
import ai.libs.hyperopt.util.ScenarioFileUtil;

public class ConfigWriterTest {

	@Test
	public void testConfigWriting() {
		SMACOptimizerConfig config = ConfigFactory.create(SMACOptimizerConfig.class);
		ScenarioFileUtil.propertiesToScenarioText(new File("test.scenario"), config, SMACOptimizerConfig.PCS_BASED_OPTIMIZER_IP, SMACOptimizerConfig.PCS_OPTIMIZER_PORT);
	}

}

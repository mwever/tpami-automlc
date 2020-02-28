package ai.libs.hyperopt.impl.pcs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.aeonbits.owner.ConfigFactory;

import ai.libs.hyperopt.api.IOptimizerConfig;

/**
 *
 * @author kadirayk
 *
 */
public interface IPCSBasedOptimizerConfig extends IOptimizerConfig {

	public static final String PCS_BASED_OPTIMIZER_IP = "pcs_based_optimizer.ip";
	public static final String PCS_OPTIMIZER_PORT = "pcs_based_optimizer.gRPC_port";

	@Key(PCS_BASED_OPTIMIZER_IP)
	@DefaultValue("localhost")
	public String getIP();

	@Key(PCS_OPTIMIZER_PORT)
	@DefaultValue("8081")
	public Integer getPort();

	public static IPCSBasedOptimizerConfig get(final String file) {
		return get(new File(file));
	}

	public static IPCSBasedOptimizerConfig get(final File file) {
		Properties props = new Properties();
		try {
			props.load(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			System.err.println("Could not find config file " + file + ". Assuming default configuration");
		} catch (IOException e) {
			System.err.println("Encountered problem with config file " + file + ". Assuming default configuration. Problem:" + e.getMessage());
		}
		return ConfigFactory.create(IPCSBasedOptimizerConfig.class, props);
	}

}

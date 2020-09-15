package benchmark.core.impl.optimizer.pcs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.aeonbits.owner.ConfigFactory;

import benchmark.core.api.input.IOptimizerConfig;

public interface IPCSOptimizerConfig extends IOptimizerConfig {

	public static final String K_PCS_BASED_OPTIMIZER_IP = "pcs_based_optimizer.ip";
	public static final String K_PCS_OPTIMIZER_PORT = "pcs_based_optimizer.gRPC_port";

	public static final String K_SEARCHSPACE_FILENAME = "searchSpaceFile";
	public static final String K_GPRPC_SCRIPTS = "gprpcScripts";
	public static final String K_GPRPC_DIR = "gprpcResourceDirectory";
	public static final String K_SINGULARITY_CONTAINER = "singularityContainer";

	@Key(K_GPRPC_DIR)
	@DefaultValue("resources/")
	public File getGPRPCDirectory();

	@Key(K_GPRPC_SCRIPTS)
	@DefaultValue("PCSBasedComponentParameter_pb2_grpc.py,PCSBasedComponentParameter_pb2.py,PCSBasedComponentParameter.proto")
	public List<String> getGPRPCScripts();

	@Key(K_SINGULARITY_CONTAINER)
	@DefaultValue("automlc.img")
	public File getSingularityContainer();

	@Key(K_SEARCHSPACE_FILENAME)
	@DefaultValue("searchspace.pcs")
	public String getSearchSpaceFileName();

	@Key(K_PCS_BASED_OPTIMIZER_IP)
	@DefaultValue("localhost")
	public String getIP();

	@Key(K_PCS_OPTIMIZER_PORT)
	@DefaultValue("8081")
	public Integer getPort();

	public static IPCSOptimizerConfig get(final String file) {
		return get(new File(file));
	}

	public static IPCSOptimizerConfig get(final File file) {
		Properties props = new Properties();
		try {
			props.load(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			System.err.println("Could not find config file " + file + ". Assuming default configuration");
		} catch (IOException e) {
			System.err.println("Encountered problem with config file " + file + ". Assuming default configuration. Problem:" + e.getMessage());
		}
		return ConfigFactory.create(IPCSOptimizerConfig.class, props);
	}
}

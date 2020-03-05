package ai.libs.hyperopt.impl.optimizer.pcs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Files;

import ai.libs.hyperopt.api.input.IOptimizerConfig;
import ai.libs.hyperopt.api.input.IPlanningOptimizationTask;
import ai.libs.jaicore.processes.ProcessIDNotRetrievableException;
import ai.libs.jaicore.processes.ProcessUtil;

public abstract class AHBLikeOptimizer<M> extends APCSBasedOptimizer<M> {
	private static final Logger LOGGER = LoggerFactory.getLogger(AHBLikeOptimizer.class);

	private static final String grpcOptRunScript = "run.py";
	private static final String grpcOptWorkerScript = "evalworker.py";
	private static final String clientConfig = "client.conf";
	private final File optDir;

	protected AHBLikeOptimizer(final String id, final IOptimizerConfig config, final IPlanningOptimizationTask<M> task) {
		super(id, config, task);
		this.optDir = new File(this.getConfig().getGPRPCDirectory(), this.getName());
	}

	public JsonNode getClientConfig() {
		ObjectMapper om = new ObjectMapper();
		ObjectNode root = om.createObjectNode();
		root.put("gRPC_port", this.getConfig().getPort());
		return root;
	}

	@Override
	public void prepareConfigFiles() throws IOException {
		// Copy the run script into the working directory
		File runScript = new File(this.optDir, grpcOptRunScript);
		File tempRunScript = new File(this.getWorkingDirectory(), grpcOptRunScript);
		LOGGER.trace("Copy {} to {}", runScript.getAbsolutePath(), tempRunScript.getAbsolutePath());
		Files.copy(runScript, tempRunScript);

		// Copy the client script into the working directory
		File workerScript = new File(this.optDir, grpcOptWorkerScript);
		File tempWorkerScript = new File(this.getWorkingDirectory(), grpcOptWorkerScript);
		LOGGER.trace("Copy {} to {}", workerScript.getAbsolutePath(), tempWorkerScript.getAbsolutePath());
		Files.copy(workerScript, tempWorkerScript);

		// Create client config file
		File clientConfigFile = new File(this.getWorkingDirectory(), clientConfig);
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(clientConfigFile))) {
			bw.write(new ObjectMapper().writeValueAsString(this.getClientConfig()));
		}
	}

	@Override
	protected void runOptimizer() throws Exception {
		ProcessBuilder pb = new ProcessBuilder(AHBLikeOptimizer.this.getCommand()).directory(AHBLikeOptimizer.this.getWorkingDirectory()).redirectOutput(Redirect.INHERIT).redirectError(Redirect.INHERIT);
		Process p = null;
		int processID = -1;
		try {
			p = pb.start();
			try {
				processID = ProcessUtil.getPID(p);
			} catch (ProcessIDNotRetrievableException e1) {
				LOGGER.warn("Could not get process id.");
			}
			p.waitFor(AHBLikeOptimizer.this.getInput().getGlobalTimeout().milliseconds(), TimeUnit.MILLISECONDS);
		} catch (IOException e) {
			LOGGER.warn("Could not spawn smac process.", e);
		} catch (InterruptedException e) {
			LOGGER.warn("Got interrupted while waiting for the process to finish.");
			e.printStackTrace();
			if (p.isAlive()) {
				if (processID >= 0) {
					try {
						ProcessUtil.killProcess(processID);
					} catch (IOException e1) {
						e1.printStackTrace();
						p.destroyForcibly();
					}
				} else {
					p.destroyForcibly();
				}
			}
		}
	}

	public List<String> getCommand() {
		return Arrays.asList("singularity", "exec", this.getConfig().getSingularityContainer().getAbsolutePath(), "python", grpcOptRunScript, "--min_budget", "1", "--max_budget", "5", "--n_iterations", "100", "--n_workers",
				this.getConfig().cpus() + "", "--id", this.getID() + "");
	}

}

package ai.libs.hyperopt.impl.optimizer.pcs.smac;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Files;

import ai.libs.hyperopt.ScenarioFileBuilder;
import ai.libs.hyperopt.api.input.IOptimizerConfig;
import ai.libs.hyperopt.api.input.IPlanningOptimizationTask;
import ai.libs.hyperopt.impl.optimizer.pcs.APCSBasedOptimizer;
import ai.libs.jaicore.processes.ProcessIDNotRetrievableException;
import ai.libs.jaicore.processes.ProcessUtil;

/**
 *
 * @author mwever
 *
 */
public class SMACOptimizer<M> extends APCSBasedOptimizer<M> {
	private static final String NAME = "smac";
	private static final String grpcOptRunScript = "run.py";
	private static final String grpcOptClientScript = "SMACOptimizerClient.py";
	private static final String clientConfig = "client.conf";
	private static final String scenarioFileName = "scenario.txt";
	private final File optDir;

	private static final Logger LOGGER = LoggerFactory.getLogger(SMACOptimizer.class);

	public SMACOptimizer(final String id, final IOptimizerConfig config, final IPlanningOptimizationTask<M> input) {
		super(id, config, input);
		this.optDir = new File(this.getConfig().getGPRPCDirectory(), NAME);
	}

	@Override
	public String getName() {
		return NAME;
	}

	public List<String> getCommand(final int x) {
		return Arrays.asList("singularity", "exec", this.getConfig().getSingularityContainer().getAbsolutePath(), "python", grpcOptRunScript, "--scenario", "scenario.txt", "--seed", x + "");
	}

	@Override
	public void prepareConfigFiles() throws IOException {
		// Copy the run script into the working directory
		File runScript = new File(this.optDir, grpcOptRunScript);
		File tempRunScript = new File(this.getWorkingDirectory(), grpcOptRunScript);
		Files.copy(runScript, tempRunScript);

		// Copy the client script into the working directory
		File clientScript = new File(this.optDir, grpcOptClientScript);
		File tempClientScript = new File(this.getWorkingDirectory(), grpcOptClientScript);
		Files.copy(clientScript, tempClientScript);

		// Create client config file
		File clientConfigFile = new File(this.getWorkingDirectory(), clientConfig);
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(clientConfigFile))) {
			bw.write(new ObjectMapper().writeValueAsString(this.getClientConfig()));
		}

		// Create scenario file
		File scenarioFile = new File(this.getWorkingDirectory(), scenarioFileName);
		ScenarioFileBuilder scenarioBuilder = new ScenarioFileBuilder().withPCSFile(this.getConfig().getSearchSpaceFileName()).withAlgo("python " + grpcOptClientScript);
		if (this.getConfig().cpus() > 1) {
			scenarioBuilder.withSharedModel("True").withDefaultInputPsmacDirs();
		}
		scenarioBuilder.toScenarioFile(scenarioFile);
	}

	public JsonNode getClientConfig() {
		ObjectMapper om = new ObjectMapper();
		ObjectNode root = om.createObjectNode();
		root.put("gRPC_port", this.getConfig().getPort());
		return root;
	}

	@Override
	protected void runOptimizer() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(this.getConfig().cpus());
		IntStream.range(0, this.getConfig().cpus()).mapToObj(x -> new Runnable() {
			@Override
			public void run() {
				ProcessBuilder pb = new ProcessBuilder(SMACOptimizer.this.getCommand(x)).directory(SMACOptimizer.this.getWorkingDirectory()).redirectOutput(Redirect.INHERIT).redirectError(Redirect.INHERIT);
				Process p = null;
				int processID = -1;
				try {
					p = pb.start();
					try {
						processID = ProcessUtil.getPID(p);
					} catch (ProcessIDNotRetrievableException e1) {

					}
					p.waitFor(SMACOptimizer.this.getInput().getGlobalTimeout().milliseconds(), TimeUnit.MILLISECONDS);
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

		}).forEach(executor::submit);
		executor.shutdown();

		try {
			executor.awaitTermination(this.getInput().getGlobalTimeout().milliseconds(), TimeUnit.MILLISECONDS);
		} finally {
			if (!executor.isTerminated() || !executor.isShutdown()) {
				executor.shutdownNow();
			}
		}
	}

}

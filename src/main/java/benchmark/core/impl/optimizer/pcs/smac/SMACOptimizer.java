package benchmark.core.impl.optimizer.pcs.smac;

import java.io.File;
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

import ai.libs.jaicore.processes.ProcessIDNotRetrievableException;
import ai.libs.jaicore.processes.ProcessUtil;
import benchmark.core.api.input.IOptimizerConfig;
import benchmark.core.api.input.IPlanningOptimizationTask;
import benchmark.core.impl.optimizer.pcs.APCSBasedOptimizer;

/**
 *
 * @author mwever
 *
 */
public class SMACOptimizer<M> extends APCSBasedOptimizer<M> {
	private static final String NAME = "smac";
	private static final String GRPC_OPT_RUN_SCRIPT = "run.py";
	private static final String GRPC_OPT_WORKER_SCRIPT = "SMACOptimizerClient.py";
	private static final String scenarioFileName = "scenario.txt";

	private static final Logger LOGGER = LoggerFactory.getLogger(SMACOptimizer.class);

	public SMACOptimizer(final String id, final IOptimizerConfig config, final IPlanningOptimizationTask<M> input) {
		super(id, config, input);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getRunScript() {
		return GRPC_OPT_RUN_SCRIPT;
	}

	@Override
	public String getWorkerScript() {
		return GRPC_OPT_WORKER_SCRIPT;
	}

	public List<String> getCommand(final int x) {
		return Arrays.asList(CONFIG.getPythonCommand(), GRPC_OPT_RUN_SCRIPT, "--scenario", "scenario.txt", "--seed", x + "");
	}

	@Override
	public void prepareConfigFiles() throws Exception {
		super.prepareConfigFiles();

		// Create scenario file
		File scenarioFile = new File(this.getWorkingDirectory(), scenarioFileName);
		ScenarioFileBuilder scenarioBuilder = new ScenarioFileBuilder().withPCSFile(this.getConfig().getSearchSpaceFileName()).withAlgo("python " + GRPC_OPT_WORKER_SCRIPT);
		if (this.getConfig().cpus() > 1) {
			scenarioBuilder.withSharedModel("True").withDefaultInputPsmacDirs();
		}
		scenarioBuilder.toScenarioFile(scenarioFile);
	}

	@Override
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

package ai.libs.hyperopt.impl.optimizer.pcs.smac;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.hyperopt.api.input.IOptimizationTask;
import ai.libs.hyperopt.impl.GlobalConfig;
import ai.libs.hyperopt.impl.HASCOToPCSConverter;
import ai.libs.hyperopt.impl.PCSBasedOptimizerServiceImpl;
import ai.libs.hyperopt.impl.exception.OptimizationException;
import ai.libs.hyperopt.impl.pcs.APCSBasedOptimizer;
import ai.libs.hyperopt.impl.pcs.IPCSBasedOptimizerConfig;
import ai.libs.hyperopt.util.ScenarioFileUtil;
import ai.libs.jaicore.basic.FileUtil;
import io.grpc.Server;
import io.grpc.ServerBuilder;

/**
 *
 * @author kadirayk
 *
 */
public class SMACOptimizer<M> extends APCSBasedOptimizer<M> {

	private static final Logger LOGGER = LoggerFactory.getLogger(SMACOptimizer.class);

	public SMACOptimizer(final IPCSBasedOptimizerConfig config, final IOptimizationTask<M> input) {
		super(config, input);
	}

	@Override
	public void prepareOptimization() throws Exception {
		// write search space configuration file
		HASCOToPCSConverter.generatePCSFile(this.getInput(), new File(this.getConfig().getExecutionPath(), "searchspace.pcs"));

		// assemble execution parameters for SMAC and write them to scenario file
		String[] keysToWriteIntoScenario = { SMACOptimizerConfig.K_SMAC_CFG_PARAM_FILE, SMACOptimizerConfig.K_SMAC_CFG_ALGO_RUNS_TIMELIMIT, SMACOptimizerConfig.K_SMAC_CFG_ALWAYS_RACE_DEFAULT, SMACOptimizerConfig.K_SMAC_CFG_COST_FOR_CRASH,
				SMACOptimizerConfig.K_SMAC_CFG_CUTOFF, SMACOptimizerConfig.K_SMAC_CFG_DETERMINISTIC, SMACOptimizerConfig.K_SMAC_CFG_GRPC_PORT, SMACOptimizerConfig.K_SMAC_CFG_MEMORY_LIMIT, SMACOptimizerConfig.K_SMAC_CFG_OVERALL_OBJ,
				SMACOptimizerConfig.K_SMAC_CFG_RUN_COUNT_LIMIT, SMACOptimizerConfig.K_SMAC_CFG_RUN_OBJ, SMACOptimizerConfig.K_SMAC_CFG_WALL_CLOCK_LIMIT, SMACOptimizerConfig.PCS_BASED_OPTIMIZER_IP, SMACOptimizerConfig.PCS_OPTIMIZER_PORT };
		ScenarioFileUtil.propertiesToScenarioText(new File(this.getConfig().getExecutionPath(), "scenario.txt"), this.getConfig(), keysToWriteIntoScenario);
	}

	@Override
	public void runOptimization() throws OptimizationException {
		// start grpc server for evaluation
		Thread thread = new Thread(() -> {
			try {
				System.out.println(this.getConfig().getPort());
				Server server = ServerBuilder.forPort(this.getConfig().getPort()).addService(new PCSBasedOptimizerServiceImpl<M>(this.getInput())).build();
				server.start();
				server.awaitTermination();
			} catch (IOException | InterruptedException e) {
				LOGGER.error(e.getMessage());
			}
		});
		thread.start();
		LOGGER.info("started gRPC server");

		// run SMAC
		StringBuilder command = new StringBuilder();
		command.append("run.py --scenario scenario.txt");

		if (this.getConfig().getNumThreads() > 1) {
			command.append(" --shared_model True --input_psmac_dirs smac3-output*");

			ExecutorService executor = Executors.newFixedThreadPool(this.getConfig().getNumThreads());
			IntStream.range(0, this.getConfig().getNumThreads()).forEach(x -> executor.submit(() -> this.runSMAC(command.toString())));
			executor.shutdown();
			try {
				if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
					executor.shutdownNow();
				}
			} catch (InterruptedException ex) {
				executor.shutdownNow();
				Thread.currentThread().interrupt();
			}
		} else {
			this.runSMAC(command.toString());
		}

	}

	@Override
	public SMACOptimizerConfig getConfig() {
		return (SMACOptimizerConfig) super.getConfig();
	}

	@Override
	public void postOptimize() {
	}

	private void runSMAC(final String command) {
		ProcessBuilder builder = new ProcessBuilder().directory(new File(this.getConfig().getExecutionPath())).command(GlobalConfig.PYTHON_EXEC, "run.py", "--scenario", "scenario.txt").redirectErrorStream(true);
		Process p = null;
		System.out.println("Execute command " + command);
		try {
			p = builder.start();
		} catch (IOException e) {
			System.out.println("Exception!");
			LOGGER.error(e.getMessage());
			LOGGER.error("Unable spawn python process={} in path={}", command, this.getConfig().getExecutionPath());
		}
		BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));

		String line = null;
		List<String> smacOutLines = new ArrayList<>();
		while (true) {
			try {
				line = r.readLine();
			} catch (IOException e) {
				LOGGER.error(e.getMessage());
			}
			smacOutLines.add(line);
			System.out.println("SMAC out: " + line);
			if (line == null) {
				break;
			}
		}
		try {
			FileUtil.writeFileAsList(smacOutLines, "testrsc/smac.log");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

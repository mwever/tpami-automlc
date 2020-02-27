package ai.libs.hyperopt.impl.pcs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.hyperopt.api.input.IOptimizationTask;
import ai.libs.hyperopt.impl.PCSBasedOptimizerGrpcServer;
import ai.libs.hyperopt.impl.exception.OptimizationException;
import ai.libs.hyperopt.impl.optimizer.pcs.HyperbandLikeOptimizerConfig;
import ai.libs.hyperopt.util.ScenarioFileUtil;
import ai.libs.jaicore.basic.FileUtil;

public abstract class AHyperbandLikeOptimizer<M> extends APCSBasedOptimizer<M> {

	private Logger logger = LoggerFactory.getLogger(AHyperbandLikeOptimizer.class);

	protected AHyperbandLikeOptimizer(final HyperbandLikeOptimizerConfig config, final IOptimizationTask<M> input) {
		super(config, input);
	}

	/**
	 * options are appended to the command
	 * @return
	 */
	public String setOptions() {
		StringBuilder command = new StringBuilder(this.getScriptExec());
		if (this.getConfig().getMinBudget() != null) {
			command.append(" --min_budget ").append(this.getConfig().getMinBudget());
		}
		if (this.getConfig().getMaxBudget() != null) {
			command.append(" --max_budget ").append(this.getConfig().getMaxBudget());
		}
		if (this.getConfig().getNIterations() != null) {
			command.append(" --n_iterations ").append(this.getConfig().getNIterations());
		}
		return command.toString();
	}

	public void runOptimization() throws OptimizationException {
		if (StringUtils.isEmpty(this.getConfig().getExecutionPath())) {
			throw new OptimizationException("executionPath must be set for Optimizer");
		}
		String filePath = this.getConfig().getExecutionPath();

		// start server thread
		Runnable task = () -> {
			try {
				PCSBasedOptimizerGrpcServer.start(this.getInput());
			} catch (IOException | InterruptedException e) {
				this.logger.error(e.getMessage());
			}
		};
		Thread thread = new Thread(task);
		thread.start();
		this.logger.info("started gRPC server");

		String[] arr = this.getInput().getRequestedInterface().split("\\.");
		String name = arr[arr.length - 1];

		try {
			List<String> lines = null;
			List<String> newLines = new ArrayList<>();
			lines = FileUtil.readFileAsList(filePath + "/scenario.txt");
			for (String line : lines) {
				String newLine = line;
				if (line.startsWith("paramfile")) {
					newLine = "paramfile = " + name + ".pcs";
				}
				newLines.add(newLine);
			}
			FileUtil.writeFileAsList(newLines, filePath + "/scenario.txt");
		} catch (IOException e1) {
			this.logger.error(e1.getMessage());
			throw new OptimizationException(MessageFormat.format("Unable to set PCS file with path={0} for Component={1}", filePath, name));
		}

		String command = this.setOptions();

		// start HpBandSter
		Integer port = this.getConfig().getPort();
		ScenarioFileUtil.updateParam(filePath, "gRPC_port", String.valueOf(port));

		ProcessBuilder builder = new ProcessBuilder(command).directory(new File(filePath));
		builder.redirectErrorStream(true);
		Process p = null;
		try {
			p = builder.start();
		} catch (IOException e) {
			this.logger.error(e.getMessage());
			throw new OptimizationException(MessageFormat.format("Unable spawn python process={0} in path={1} ", command, filePath));
		}
		BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));

		String line = null;
		List<String> bohbOutLines = new ArrayList<>();
		while (true) {
			try {
				line = r.readLine();
			} catch (IOException e) {
				this.logger.error(e.getMessage());
			}
			bohbOutLines.add(line);
			if (line == null) {
				break;
			}
		}
		try {
			FileUtil.writeFileAsList(bohbOutLines, this.getOutputLog());
		} catch (IOException e) {
			throw new OptimizationException(e);
		}
	}

	public abstract String getScriptExec();

	public abstract String getOutputLog();

	@Override
	public HyperbandLikeOptimizerConfig getConfig() {
		return (HyperbandLikeOptimizerConfig) super.getConfig();
	}
}

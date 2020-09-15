package benchmark.core.impl.optimizer.pcs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;

import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.io.FileUtils;
import org.api4.java.algorithm.events.IAlgorithmEvent;
import org.api4.java.algorithm.exceptions.AlgorithmException;
import org.api4.java.algorithm.exceptions.AlgorithmExecutionCanceledException;
import org.api4.java.algorithm.exceptions.AlgorithmTimeoutedException;
import org.api4.java.common.attributedobjects.IObjectEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.Files;

import ai.libs.jaicore.basic.algorithm.AOptimizer;
import ai.libs.jaicore.components.model.ComponentInstance;
import benchmark.ISystemConfig;
import benchmark.core.api.AListenable;
import benchmark.core.api.IOptimizer;
import benchmark.core.api.input.IOptimizerConfig;
import benchmark.core.api.input.IPlanningOptimizationTask;
import benchmark.core.api.output.IOptimizationOutput;
import benchmark.core.api.output.IOptimizationSolutionCandidateFoundEvent;
import benchmark.core.impl.optimizer.pcs.grpc.PCSBasedOptimizerServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public abstract class APCSBasedOptimizer<M> extends AOptimizer<IPlanningOptimizationTask<M>, IOptimizationOutput<M>, Double> implements IOptimizer<IPlanningOptimizationTask<M>, M> {

	protected static final ISystemConfig CONFIG = ConfigFactory.create(ISystemConfig.class);

	private static final Logger LOGGER = LoggerFactory.getLogger(APCSBasedOptimizer.class);
	private static final String clientConfig = "client.conf";

	private HASCOToPCSConverter searchspaceConverter;

	private final String id;
	private File workingDirectory;
	private File pcsFile;

	private Server server;
	private final File optDir;

	private IOptimizationSolutionCandidateFoundEvent<M> bestSolution = null;

	class ResultListener {
		@Subscribe
		public void rcvEvent(final IOptimizationSolutionCandidateFoundEvent<M> e) {
			if (APCSBasedOptimizer.this.updateBestSeenSolution(e.getOutput())) {
				APCSBasedOptimizer.this.bestSolution = e;
				System.out.println("Update best seen solution with score " + e.getScore() + " and candidate " + e.getSolutionCandidate());
			}
		}
	}

	protected APCSBasedOptimizer(final String id, final IOptimizerConfig config, final IPlanningOptimizationTask<M> task) {
		super(config, task);
		this.searchspaceConverter = new HASCOToPCSConverter(task.getComponents(), task.getRequestedInterface());
		this.id = id;
		this.workingDirectory = new File(config.getWorkingDir(), id);

		if (this.workingDirectory.exists()) {
			try {
				FileUtils.cleanDirectory(this.workingDirectory);
			} catch (IOException e) {
				LOGGER.warn("Could not clearn working directory.", e);
			}
		}

		IObjectEvaluator<ComponentInstance, Double> evaluator = this.getInput().getDirectEvaluator(this.getClass().getSimpleName());
		if (evaluator instanceof AListenable) {
			((AListenable) evaluator).registerListener(new ResultListener());
		}

		this.workingDirectory.mkdirs();
		this.optDir = new File(this.getConfig().getGPRPCDirectory(), this.getName());
	}

	@Override
	public IAlgorithmEvent nextWithException() throws InterruptedException, AlgorithmExecutionCanceledException, AlgorithmTimeoutedException, AlgorithmException {
		switch (this.getState()) {
		case CREATED:
			// prepare optimization
			try {
				this.prepareSearchspace();
			} catch (Exception e) {
				throw new AlgorithmException("Could not create search space file.", e);
			}

			try {
				this.prepareEvaluationStub();
			} catch (Exception e) {
				throw new AlgorithmException("Could not prepare candidate evaluation infrastructure.", e);
			}

			try {
				// prepare evaluation interface server
				this.server = ServerBuilder.forPort(this.getPort()).addService(new PCSBasedOptimizerServiceImpl<M>(this.getInput(), this.searchspaceConverter)).build();
				this.server.start();
			} catch (Exception e) {
				throw new AlgorithmException("Could not start evaluation server.", e);
			}

			try {
				this.prepareConfigFiles();
			} catch (Exception e) {
				throw new AlgorithmException("Could not prepare configuration files for optimizer " + this.getClass().getSimpleName(), e);
			}

			return this.activate();
		case ACTIVE:
			// start optimizer program
			try {
				this.runOptimizer();
			} catch (Throwable e) {
				throw new AlgorithmException("A problem occurred while running the optimizer.", e);
			} finally {
				// shutdown grpc server
				this.server.shutdownNow();
			}
			// terminate algorithm as optimizer is finished now.
			return this.terminate();
		default:
			throw new IllegalStateException("The algorithm is already inactive and cannot be called anymore.");
		}
	}

	protected abstract void runOptimizer() throws Exception;

	private void prepareSearchspace() throws Exception {
		this.pcsFile = new File(this.workingDirectory, this.getConfig().getSearchSpaceFileName());
		this.searchspaceConverter.generatePCSFile(this.pcsFile);
	}

	private void prepareEvaluationStub() throws Exception {
		// copy protobuf files
		for (String evalStubFilename : this.getConfig().getGPRPCScripts()) {
			Files.copy(new File(this.getConfig().getGPRPCDirectory(), evalStubFilename), new File(this.getWorkingDirectory(), evalStubFilename));
		}
	}

	/**
	 * Hook for preparing configuration files.
	 * @throws Exception
	 */
	protected void prepareConfigFiles() throws Exception {
		// Create client config file
		File clientConfigFile = new File(this.getWorkingDirectory(), clientConfig);
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(clientConfigFile))) {
			bw.write(new ObjectMapper().writeValueAsString(this.getClientConfig()));
		}

		// Copy the run script into the working directory
		File runScript = new File(this.optDir, this.getRunScript());
		File tempRunScript = new File(this.getWorkingDirectory(), this.getRunScript());
		LOGGER.trace("Copy {} to {}", runScript.getAbsolutePath(), tempRunScript.getAbsolutePath());
		Files.copy(runScript, tempRunScript);

		// Copy the client script into the working directory
		File workerScript = new File(this.optDir, this.getWorkerScript());
		File tempWorkerScript = new File(this.getWorkingDirectory(), this.getWorkerScript());
		LOGGER.trace("Copy {} to {}", workerScript.getAbsolutePath(), tempWorkerScript.getAbsolutePath());
		Files.copy(workerScript, tempWorkerScript);
	}

	public abstract JsonNode getClientConfig();

	public abstract String getRunScript();

	public abstract String getWorkerScript();

	@Override
	public ComponentInstance getResultAsComponentInstance() {
		return this.bestSolution.getSolutionCandidate();
	}

	@Override
	public M getResult() {
		return this.bestSolution.getObject();
	}

	public String getID() {
		return this.id;
	}

	/**
	 * @return The directory for placing temporary files etc.
	 */
	public File getWorkingDirectory() {
		return this.workingDirectory;
	}

	private int getPort() {
		int port = this.getConfig().getPort();
		boolean available = false;
		while (!available) {
			// check whether we can spawn a server listening on the port
			try (ServerSocket ignored = new ServerSocket(port)) {
				available = true;
			} catch (IOException e) {
				LOGGER.info("Port {} seems not to be available, thus check for the next available port {}.", port, port + 1);
				port++;
			}
		}
		// update the config so that the information about the actually used port is available in sub-classes as well.
		this.getConfig().setProperty(IPCSOptimizerConfig.K_PCS_OPTIMIZER_PORT, port + "");
		return port;
	}

	@Override
	public IPCSOptimizerConfig getConfig() {
		return (IPCSOptimizerConfig) super.getConfig();
	}

}

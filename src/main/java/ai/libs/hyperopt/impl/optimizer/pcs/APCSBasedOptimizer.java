package ai.libs.hyperopt.impl.optimizer.pcs;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.api4.java.algorithm.events.IAlgorithmEvent;
import org.api4.java.algorithm.exceptions.AlgorithmException;
import org.api4.java.algorithm.exceptions.AlgorithmExecutionCanceledException;
import org.api4.java.algorithm.exceptions.AlgorithmTimeoutedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hyperopt.api.IOptimizer;
import ai.libs.hyperopt.api.input.IOptimizerConfig;
import ai.libs.hyperopt.api.input.IPlanningOptimizationTask;
import ai.libs.hyperopt.api.output.IOptimizationOutput;
import ai.libs.hyperopt.impl.GlobalConfig;
import ai.libs.hyperopt.impl.optimizer.pcs.grpc.PCSBasedOptimizerServiceImpl;
import ai.libs.jaicore.basic.algorithm.AOptimizer;
import ai.libs.jaicore.processes.ProcessUtil;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public abstract class APCSBasedOptimizer<M> extends AOptimizer<IPlanningOptimizationTask<M>, IOptimizationOutput<M>, Double> implements IOptimizer<IPlanningOptimizationTask<M>, M> {

	private static final Logger LOGGER = LoggerFactory.getLogger(APCSBasedOptimizer.class);
	private static final IPCSBasedOptimizerConfig GRPC_CONFIG = IPCSBasedOptimizerConfig.get(GlobalConfig.OPTIMIZER_CONFIG_PATH);
	
	private HASCOToPCSConverter searchspaceConverter;

	private File pcsFile;
	private File workingDirectory = new File("test");
	
	private Server server;

	protected APCSBasedOptimizer(final IOptimizerConfig config, final IPlanningOptimizationTask<M> task) {
		super(config, task);
		this.searchspaceConverter = new HASCOToPCSConverter(task.getComponents(), task.getRequestedInterface());
	}

	@Override
	public IAlgorithmEvent nextWithException() throws InterruptedException, AlgorithmExecutionCanceledException, AlgorithmTimeoutedException, AlgorithmException {
		switch (this.getState()) {
		case CREATED:
			// prepare optimization
			try {
				this.preparePCSFile();
			} catch (Exception e) {
				throw new AlgorithmException("Could not create search space file.",e);
			}
			try {
				// prepare evaluation interface server
				server = ServerBuilder.forPort(GRPC_CONFIG.getPort()).addService(new PCSBasedOptimizerServiceImpl<M>(this.getInput(), this.searchspaceConverter)).build();
				server.start();
			} catch (Exception e) {
				throw new AlgorithmException("Could not start evaluation server.", e);
			}
			return this.activate();
		case ACTIVE:
			// start optimizer program
			ProcessBuilder pb = new ProcessBuilder(this.getCommand()).directory(this.workingDirectory).redirectOutput(Redirect.INHERIT).redirectError(Redirect.INHERIT);
			Process p = null;
			try {
				// start optimization process
				p = pb.start();
				// ensure that we meet the timeout or the process is already finished
				p.waitFor(this.getInput().getGlobalTimeout().milliseconds(), TimeUnit.MILLISECONDS);
			} catch (IOException e) {
				LOGGER.warn("Could not run optimizer process due to exception.", e);
			} catch (InterruptedException e) {
				LOGGER.debug("Thread got interrupted, shutdown optimizer process if running.");
				if (p != null) {
					try {
						ProcessUtil.killProcess(p);
					} catch (IOException e1) {
						LOGGER.warn("Could not kill optimizer's process due to interrupted exception.", e1);
					}
				}
			}
			
			server.shutdownNow();
			// terminate algorithm as optimizer is finished now.
			return this.terminate();
		default:
			throw new IllegalStateException("The algorithm is already inactive and cannot be called anymore.");
		}
	}

	private void preparePCSFile() throws Exception {
//		this.pcsFile = File.createTempFile("searchspace", ".pcs");
//		this.pcsFile.deleteOnExit();
		this.pcsFile = new File(new File("test"), "searchspace.pcs");
		this.searchspaceConverter.generatePCSFile(this.pcsFile);
	}
	
	@Override
	public ComponentInstance getResultAsComponentInstance() {
		return null;
	}

	@Override
	public M getResult() {
		return null;
	}

	/**
	 * The command to be executed to run the pcs based optimizer.
	 * @return List of command parts to be executed.
	 */
	public abstract List<String> getCommand();

}

package ai.libs.hyperopt.impl.optimizer.pcs;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.api4.java.algorithm.events.IAlgorithmEvent;
import org.api4.java.algorithm.exceptions.AlgorithmException;
import org.api4.java.algorithm.exceptions.AlgorithmExecutionCanceledException;
import org.api4.java.algorithm.exceptions.AlgorithmTimeoutedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.hyperopt.api.input.IOptimizationTask;
import ai.libs.hyperopt.api.input.IOptimizerConfig;
import ai.libs.hyperopt.api.output.IOptimizationOutput;
import ai.libs.hyperopt.impl.HASCOToPCSConverter;
import ai.libs.jaicore.basic.algorithm.AOptimizer;
import ai.libs.jaicore.processes.ProcessUtil;

public abstract class APCSBasedOptimizer<M> extends AOptimizer<IOptimizationTask<M>, IOptimizationOutput<M>, Double> {

	private static final Logger LOGGER = LoggerFactory.getLogger(APCSBasedOptimizer.class);
	private HASCOToPCSConverter searchspaceConverter;

	private File pcsFile;
	private File workingDirectory;

	protected APCSBasedOptimizer(final IOptimizerConfig config, final IOptimizationTask<M> task) {
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
				throw new AlgorithmException("Could not create search space file.");
			}
			return this.activate();
		case ACTIVE:
			// start optimizer program
			ProcessBuilder pb = new ProcessBuilder(this.getCommand()).directory(this.workingDirectory);
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

	/**
	 * The command to be executed to run the pcs based optimizer.
	 * @return List of command parts to be executed.
	 */
	public abstract List<String> getCommand();

}

package ai.libs.hyperopt.impl.optimizer.baseline;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import org.api4.java.algorithm.events.IAlgorithmEvent;
import org.api4.java.algorithm.exceptions.AlgorithmException;
import org.api4.java.algorithm.exceptions.AlgorithmExecutionCanceledException;
import org.api4.java.algorithm.exceptions.AlgorithmTimeoutedException;
import org.api4.java.common.attributedobjects.IObjectEvaluator;
import org.api4.java.common.attributedobjects.ObjectEvaluationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.hasco.core.Util;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hyperopt.api.ConversionFailedException;
import ai.libs.hyperopt.api.IOptimizer;
import ai.libs.hyperopt.api.input.IOptimizerConfig;
import ai.libs.hyperopt.api.input.IPlanningOptimizationTask;
import ai.libs.hyperopt.api.output.IOptimizationOutput;
import ai.libs.hyperopt.impl.model.OptimizationOutput;
import ai.libs.hyperopt.util.ComponentUtil;
import ai.libs.jaicore.basic.algorithm.AOptimizer;

public class RandomSearch<M> extends AOptimizer<IPlanningOptimizationTask<M>, IOptimizationOutput<M>, Double> implements IOptimizer<IPlanningOptimizationTask<M>, M> {

	private static final Logger LOGGER = LoggerFactory.getLogger(RandomSearch.class);

	private Lock lock = new ReentrantLock();
	private Semaphore globalTimeoutSynchro = new Semaphore(0);

	private final List<ComponentInstance> allSelections;
	private final IObjectEvaluator<ComponentInstance, Double> evaluator;

	private final long timestamp = System.currentTimeMillis();

	private ExecutorService pool;

	public RandomSearch(final IOptimizerConfig config, final IPlanningOptimizationTask<M> input) {
		super(config, input);
		this.allSelections = new ArrayList<>(ComponentUtil.getAllAlgorithmSelectionInstances(input.getRequestedInterface(), input.getComponents()));
		this.evaluator = input.getDirectEvaluator(this.getClass().getSimpleName());
	}

	@Override
	public IAlgorithmEvent nextWithException() throws InterruptedException, AlgorithmExecutionCanceledException, AlgorithmTimeoutedException, AlgorithmException {
		switch (this.getState()) {
		case CREATED:
			// create thread pool for random search workers
			this.pool = Executors.newFixedThreadPool(this.getNumCPUs());
			return super.activate();
		case ACTIVE:
			IntStream.range(0, this.getNumCPUs()).mapToObj(x -> new RandomSearchWorker(x)).forEach(this.pool::submit);
			boolean timeouted = !this.globalTimeoutSynchro.tryAcquire(this.getNumCPUs(), this.getInput().getGlobalTimeout().milliseconds(), TimeUnit.MILLISECONDS);
			if (timeouted) {
				this.cancel();
			}
			return this.terminate();
		default:
			throw new IllegalStateException("RandomSearch cannot do anything in state " + this.getState());
		}
	}

	class RandomSearchWorker implements Runnable {
		private int id;
		private Random rand;

		public RandomSearchWorker(final int id) {
			this.id = id;
			this.rand = new Random(this.id);
		}

		@Override
		public void run() {
			try {
				while (!RandomSearch.this.isCanceled()) {
					LOGGER.trace("Sample new candidate...");
					ComponentInstance ci = this.sampleRandomComponent();
					try {
						LOGGER.trace("Evaluate candidate.");
						Double score = RandomSearch.this.evaluator.evaluate(ci);
						LOGGER.trace("Create candidate output object.");
						IOptimizationOutput<M> candidate = new OptimizationOutput<>(RandomSearch.this.timestamp, RandomSearch.this.getInput().getConverter().convert(ci), score, ci);
						LOGGER.info("Found candidate with score {} ({}).", score, ci);
						RandomSearch.this.lock.lock();
						try {
							RandomSearch.this.updateBestSeenSolution(candidate);
						} finally {
							RandomSearch.this.lock.unlock();
						}
					} catch (ConversionFailedException e) {
						LOGGER.info("Could not convert candidate {} into an executable object and a respective entry has been logged to the evaluation table.", Util.getComponentNamesOfComposition(ci));
					} catch (ObjectEvaluationFailedException e) {
						LOGGER.info("Could not evaluate candidate {} and a respective entry has been logged to the evaluation table.", Util.getComponentNamesOfComposition(ci));
					} catch (InterruptedException e) {
						LOGGER.info("Thread {} has been interrupted, thus, shutting down this thread.");
						break;
					}
				}
			} finally {
				LOGGER.info("Releasing one permit on the global timeout synchronization as this thread is going to shutdown");
				RandomSearch.this.globalTimeoutSynchro.release();
			}
		}

		/**
		 * Samples uniformly an unparametrized MLC classifier from the set of all configurable classifiers and then samples a random parameterization of this classifier and its nested components.
		 * @return The random component instantiation of an MLC Meka classifier.
		 */
		private ComponentInstance sampleRandomComponent() {
			LOGGER.trace("Sample uniformly algorithm selection.");
			ComponentInstance ciToInstantiate = new ComponentInstance(RandomSearch.this.allSelections.get(this.rand.nextInt(RandomSearch.this.allSelections.size())));
			List<ComponentInstance> queue = new LinkedList<>();
			queue.add(ciToInstantiate);

			LOGGER.trace("Sample parameters for contained components.");
			while (!queue.isEmpty()) {
				ComponentInstance currentCI = queue.remove(0);
				if (!currentCI.getComponent().getParameters().isEmpty()) {
					ComponentInstance parametrization = null;
					while (parametrization == null) {
						try {
							parametrization = ComponentUtil.randomParameterizationOfComponent(currentCI.getComponent(), this.rand);
						} catch (Exception e) {
							LOGGER.warn("Could not instantiate component instance {} with random parameters", ciToInstantiate, e);
						}
					}
					currentCI.getParameterValues().putAll(parametrization.getParameterValues());

				}

				if (!currentCI.getSatisfactionOfRequiredInterfaces().isEmpty()) {
					queue.addAll(currentCI.getSatisfactionOfRequiredInterfaces().values());
				}
			}

			LOGGER.trace("Return randomly sampled component instance.");
			return ciToInstantiate;
		}
	}

	@Override
	public ComponentInstance getResultAsComponentInstance() {
		return this.getBestSeenSolution().getSolutionDescription();
	}

	@Override
	public M getResult() {
		return this.getBestSeenSolution().getObject();
	}

	public Double getBestScore() {
		return this.getBestSeenSolution().getScore();
	}

	@Override
	public void cancel() {
		super.cancel();
		this.pool.shutdownNow();
	}
}

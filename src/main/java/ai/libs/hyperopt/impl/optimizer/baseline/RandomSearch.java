package ai.libs.hyperopt.impl.optimizer.baseline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.api4.java.algorithm.events.IAlgorithmEvent;
import org.api4.java.algorithm.exceptions.AlgorithmException;
import org.api4.java.algorithm.exceptions.AlgorithmExecutionCanceledException;
import org.api4.java.algorithm.exceptions.AlgorithmTimeoutedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hyperopt.api.IOptimizer;
import ai.libs.hyperopt.api.input.IOptimizerConfig;
import ai.libs.hyperopt.api.input.IPlanningOptimizationTask;
import ai.libs.hyperopt.api.output.IOptimizationOutput;
import ai.libs.hyperopt.impl.model.OptimizationOutput;
import ai.libs.jaicore.basic.algorithm.AOptimizer;
import de.upb.ml2plan.event.CandidateEvaluatedEventImpl;
import de.upb.ml2plan.randomsearch.ComponentUtil;

public class RandomSearch<M> extends AOptimizer<IPlanningOptimizationTask<M>, IOptimizationOutput<M>, Double> implements IOptimizer<IPlanningOptimizationTask<M>, M> {

	private static final Logger LOGGER = LoggerFactory.getLogger(RandomSearch.class);

	private Lock lock = new ReentrantLock();
	private Condition globalTimeout = this.lock.newCondition();

	private final AtomicInteger orderNo = new AtomicInteger(0);

	private EventBus eventBus;
	private final List<ComponentInstance> allSelections;

	private ExecutorService pool;

	public RandomSearch(final IOptimizerConfig config, final IPlanningOptimizationTask<M> input) {
		super(config, input);
		this.allSelections = new ArrayList<>(ComponentUtil.getAllAlgorithmSelectionInstances(input.getRequestedInterface(), input.getComponents()));
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
			boolean signalled = this.globalTimeout.await(this.getInput().getGlobalTimeout().milliseconds(), TimeUnit.MILLISECONDS);
			if (!signalled) {
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
		private M evaluableObject;
		private Double score;

		public RandomSearchWorker(final int id) {
			this.id = id;
			this.rand = new Random(this.id);
		}

		@Override
		public void run() {
			while (!RandomSearch.this.isCanceled()) {
				Map<String, Object> map = new HashMap<>();

				LOGGER.info("Sample new candidate...");
				ComponentInstance ci = this.sampleRandomComponent();
				this.evaluableObject = null;
				this.score = null;

				// Define future task
				Callable<IOptimizationOutput<M>> futureCall = new Callable<IOptimizationOutput<M>>() {
					@Override
					public IOptimizationOutput<M> call() throws Exception {
						long startTimestamp = System.currentTimeMillis();
						RandomSearchWorker.this.evaluableObject = RandomSearch.this.getInput().getConverter().convert(ci);
						RandomSearchWorker.this.score = RandomSearch.this.getInput().getEvaluator().evaluate(RandomSearchWorker.this.evaluableObject);
						map.put("evalTime", (System.currentTimeMillis() - startTimestamp));
						map.put("internalScore", RandomSearchWorker.this.score);
						map.put("exitState", "done");
						return new OptimizationOutput<>(RandomSearchWorker.this.evaluableObject, RandomSearchWorker.this.score, ci);
					}
				};
				FutureTask<IOptimizationOutput<M>> run = new FutureTask<>(futureCall);

				Thread future = new Thread(run);
				long starttime = System.currentTimeMillis();
				try {
					future.start();
					IOptimizationOutput<M> result = run.get(RandomSearch.this.getInput().getEvaluationTimeout().milliseconds(), TimeUnit.MILLISECONDS);
					if (result == null) {
						throw new NullPointerException("The result of the future task was null.");
					}
					map.put("runtime", (System.currentTimeMillis() - starttime));
					LOGGER.info("Evaluated candidate {} with score {} in time {}ms", "x", result.getScore());
					this.sendEvent(result, map);
				} catch (TimeoutException e) {
					LOGGER.info("A timeout occurred for the evaluation task as the future is not yet done."); // execution of candidate timed out.
					future.interrupt(); // ensure that future task thread is shutting down
					map.put("exitState", "timeout");
					map.put("internalScore", 20000);
					map.put("runtime", (System.currentTimeMillis() - starttime));
					this.sendEvent(new OptimizationOutput<>(this.evaluableObject, this.score, ci), map);
				} catch (InterruptedException e) {
					LOGGER.info("Worker thread got interrupted, so forward the interrupt to the current future task.");
					future.interrupt(); // forward interrupt to candidate evaluation future
					map.put("exitState", "interrutped");
					map.put("internalScore", 10000);
					map.put("runTime", (System.currentTimeMillis() - starttime));
					this.sendEvent(new OptimizationOutput<>(this.evaluableObject, this.score, ci), map);
				} catch (Exception e) {
					LOGGER.warn("An unexpected exception occurred!");
					future.interrupt(); // ensure future is exited.
					this.sendEvent(new OptimizationOutput<>(this.evaluableObject, this.score, ci), e); // send event with exception stack trace.
				}
			}
		}

		private void sendEvent(final IOptimizationOutput<M> result, final Map<String, Object> map) {
			RandomSearch.this.getEventBus().post(new CandidateEvaluatedEventImpl<>(Thread.currentThread().getName(), result, RandomSearch.this.orderNo.getAndIncrement(), map));
		}

		private void sendEvent(final IOptimizationOutput<M> result, final Throwable e) {
			RandomSearch.this.getEventBus().post(new CandidateEvaluatedEventImpl<>(Thread.currentThread().getName(), result, RandomSearch.this.orderNo.getAndIncrement(), ExceptionUtils.getStackTrace(e)));
		}

		/**
		 * Samples uniformly an unparametrized MLC classifier from the set of all configurable classifiers and then samples a random parameterization of this classifier and its nested components.
		 * @return The random component instantiation of an MLC Meka classifier.
		 */
		private ComponentInstance sampleRandomComponent() {
			ComponentInstance ciToInstantiate = new ComponentInstance(RandomSearch.this.allSelections.get(this.rand.nextInt(RandomSearch.this.allSelections.size())));
			List<ComponentInstance> queue = new LinkedList<>();
			queue.add(ciToInstantiate);
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

			return ciToInstantiate;
		}
	}

	@Override
	public void registerListener(final Object listener) {
		this.eventBus.register(listener);
	}

	public void unregisterListener(final Object listener) {
		this.eventBus.unregister(listener);
	}

	public EventBus getEventBus() {
		return this.eventBus;
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
		this.globalTimeout.notify();
		this.pool.shutdownNow();
	}
}

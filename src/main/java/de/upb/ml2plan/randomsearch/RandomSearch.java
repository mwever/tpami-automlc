package de.upb.ml2plan.randomsearch;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.api4.java.ai.ml.classification.IClassifierEvaluator;
import org.api4.java.algorithm.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.serialization.ComponentLoader;
import ai.libs.jaicore.interrupt.Interrupter;
import ai.libs.jaicore.ml.classification.multilabel.learner.IMekaClassifier;
import de.upb.ml2plan.IListenable;
import de.upb.ml2plan.event.CandidateEvaluatedEventImpl;
import de.upb.ml2plan.logger.DatabaseLogger;
import de.upb.ml2plan.logger.SCandidateEvaluatedSchema;
import meka.classifiers.multilabel.Evaluation;
import meka.core.MLUtils;
import weka.core.Instances;

public class RandomSearch implements IListenable {
	private static final Logger LOGGER = LoggerFactory.getLogger(RandomSearch.class);

	private final EventBus eventBus = new EventBus();

	private final int numCPUs;
	private final IClassifierEvaluator evaluator;
	private final Timeout timeout;
	private final Timeout candidateTimeout;

	private volatile AtomicBoolean running = new AtomicBoolean(true);

	private long finishByTimestamp;

	private final ComponentLoader cl;
	private final List<ComponentInstance> allSelections;
	private final MekaPipelineFactory factory = new MekaPipelineFactory();
	private final AtomicInteger orderNo = new AtomicInteger(0);
	private final Instances dataset;

	private final Interrupter interrupter;

	public RandomSearch(final int numCPUs, final Timeout timeout, final Timeout candidateTimeout, final IClassifierEvaluator evaluator) throws Exception {
		this.numCPUs = numCPUs;
		this.timeout = timeout;
		this.candidateTimeout = candidateTimeout;
		this.evaluator = evaluator;
		this.cl = new ComponentLoader(new File("testrsc/meka/mlplan-meka.json"));
		this.allSelections = new ArrayList<>(ComponentUtil.getAllAlgorithmSelectionInstances("MLClassifier", this.cl.getComponents()));
		this.dataset = new Instances(new FileReader(new File("testrsc/flags.arff")));
//		this.dataset.setClassIndex(this.dataset.numAttributes() - 1);
		MLUtils.prepareData(this.dataset);
		this.interrupter = Interrupter.get();
	}

	public void run() {
		this.finishByTimestamp = System.currentTimeMillis() + this.timeout.milliseconds();
		ExecutorService pool = Executors.newFixedThreadPool(this.numCPUs);
		IntStream.range(0, this.numCPUs).mapToObj(x -> new RandomSearchWorker(x)).forEach(pool::submit);
		pool.shutdown();
		try {
			pool.awaitTermination(this.finishByTimestamp - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
		}
	}

	@Override
	public EventBus getEventBus() {
		return this.eventBus;
	}

	private Timer timer = new Timer();

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
				while (RandomSearch.this.running.get()) {
					LOGGER.info("Sample new candidate...");
					ComponentInstance ci = this.sampleRandomComponent();
					TimerTask interruptTask = new TimerTask() {
						private Thread thread = Thread.currentThread();

						@Override
						public void run() {
							this.thread.interrupt();
						}
					};
					long startTime = System.currentTimeMillis();
					try {
						IMekaClassifier classifier = RandomSearch.this.factory.getComponentInstantiation(ci);
						RandomSearch.this.timer.schedule(interruptTask, 10000);

						Evaluation.cvModel(classifier.getClassifier(), new Instances(RandomSearch.this.dataset), 2, "0.5");
						classifier.getClassifier().getCapabilities().testWithFail(RandomSearch.this.dataset);
						interruptTask.cancel();

						Map<String, Object> map = new HashMap<>();
						map.put("runType", "finished");
						map.put("runTime", (System.currentTimeMillis() - startTime));
						RandomSearch.this.getEventBus().post(new CandidateEvaluatedEventImpl(Thread.currentThread().getName(), ci, RandomSearch.this.orderNo.getAndIncrement(), map));
					} catch (InterruptedException e) {
						Map<String, Object> map = new HashMap<>();
						map.put("runType", "interrutped");
						map.put("runTime", (System.currentTimeMillis() - startTime));
						LOGGER.info("Got interrupted which is a good sign, thus send success event!");
						RandomSearch.this.getEventBus().post(new CandidateEvaluatedEventImpl(Thread.currentThread().getName(), ci, RandomSearch.this.orderNo.getAndIncrement(), map));
					} catch (Exception e) {
						interruptTask.cancel();
						LOGGER.info("Capabilities do not match, send fail event!");
						RandomSearch.this.getEventBus().post(new CandidateEvaluatedEventImpl(Thread.currentThread().getName(), ci, RandomSearch.this.orderNo.getAndIncrement(), ExceptionUtils.getStackTrace(e)));
					}
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
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

	public static void main(final String[] args) throws Exception {
		RandomSearch rs = new RandomSearch(6, new Timeout(24, TimeUnit.HOURS), new Timeout(600, TimeUnit.SECONDS), null);
		rs.registerListener(new Object() {
			@Subscribe
			public void rcvEvent(final Object event) {
				System.out.println(event);
			}
		});

		Map<String, Object> defaultValues = new HashMap<>();
		defaultValues.put(SCandidateEvaluatedSchema.EXPERIMENT_ID.getName(), -1);
		rs.registerListener(new DatabaseLogger(defaultValues));
		rs.run();

	}

}

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.api4.java.ai.ml.classification.IClassifierEvaluator;
import org.api4.java.algorithm.Timeout;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.model.ComponentUtil;
import ai.libs.hasco.serialization.ComponentLoader;
import ai.libs.jaicore.interrupt.Interrupter;
import ai.libs.jaicore.interrupt.InterruptionTimerTask;
import ai.libs.jaicore.ml.classification.multilabel.learner.IMekaClassifier;
import ai.libs.mlplan.multilabel.mekamlplan.MekaPipelineFactory;
import de.upb.ml2plan.IListenable;
import de.upb.ml2plan.event.CandidateEvaluatedEventImpl;
import de.upb.ml2plan.logger.DatabaseLogger;
import de.upb.ml2plan.logger.SCandidateEvaluatedSchema;
import meka.classifiers.multilabel.Evaluation;
import meka.core.MLUtils;
import meka.core.Result;
import weka.core.Instances;

public class RandomSearch implements IListenable {

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
	private final Timer timer;

	public RandomSearch(final int numCPUs, final Timeout timeout, final Timeout candidateTimeout, final IClassifierEvaluator evaluator) throws Exception {
		this.numCPUs = numCPUs;
		this.timeout = timeout;
		this.candidateTimeout = candidateTimeout;
		this.evaluator = evaluator;
		this.cl = new ComponentLoader(new File("testrsc/meka/mlplan-meka.json"));
		this.allSelections = new ArrayList<>(ComponentUtil.getAllAlgorithmSelectionInstances("MLClassifier", this.cl.getComponents()));
		this.dataset = new Instances(new FileReader(new File("testrsc/flags.arff")));
		MLUtils.prepareData(this.dataset);
		this.interrupter = Interrupter.get();
		this.timer = new Timer();
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

	class RandomSearchWorker implements Runnable {
		private int id;
		private Random rand;

		public RandomSearchWorker(final int id) {
			this.id = id;
			this.rand = new Random(this.id);
		}

		@Override
		public void run() {
			while (RandomSearch.this.running.get()) {
				ComponentInstance ci = this.sampleRandomComponent();
				long startTimestamp = System.currentTimeMillis();
				InterruptionTimerTask interruptMe = new InterruptionTimerTask("CancelEval" + Thread.currentThread().getName(), Thread.currentThread());
				RandomSearch.this.timer.schedule(interruptMe, RandomSearch.this.candidateTimeout.milliseconds());
				try {
					System.out.println("Schedule timeout for in " + RandomSearch.this.candidateTimeout.milliseconds() + "ms");
					IMekaClassifier classifier = RandomSearch.this.factory.getComponentInstantiation(ci);
					Result res = Evaluation.cvModel(classifier.getClassifier(), new Instances(RandomSearch.this.dataset), 2, "0.5");
					interruptMe.cancel();
					RandomSearch.this.getEventBus().post(new CandidateEvaluatedEventImpl(Thread.currentThread().getName(), ci, RandomSearch.this.orderNo.getAndIncrement(), res.getStats(res, "1")));
				} catch (InterruptedException e) {
					Thread.interrupted();
					System.err.println(Thread.currentThread() + ": Timeout after " + (System.currentTimeMillis() - startTimestamp) + "ms");
					RandomSearch.this.getEventBus().post(new CandidateEvaluatedEventImpl(Thread.currentThread().getName(), ci, RandomSearch.this.orderNo.getAndIncrement(), "Eval Timeout"));
				} catch (Exception e) {
					interruptMe.cancel();
					RandomSearch.this.getEventBus().post(new CandidateEvaluatedEventImpl(Thread.currentThread().getName(), ci, RandomSearch.this.orderNo.getAndIncrement(), ExceptionUtils.getStackTrace(e)));
				}
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
					currentCI.getParameterValues().putAll(ComponentUtil.randomParameterizationOfComponent(currentCI.getComponent(), this.rand).getParameterValues());
				}

				if (!currentCI.getSatisfactionOfRequiredInterfaces().isEmpty()) {
					queue.addAll(currentCI.getSatisfactionOfRequiredInterfaces().values());
				}
			}

			return ciToInstantiate;
		}
	}

	public static void main(final String[] args) throws Exception {
		RandomSearch rs = new RandomSearch(40, new Timeout(24, TimeUnit.HOURS), new Timeout(600, TimeUnit.SECONDS), null);
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

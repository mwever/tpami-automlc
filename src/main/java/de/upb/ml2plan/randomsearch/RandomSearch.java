package de.upb.ml2plan.randomsearch;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.api4.java.ai.ml.classification.IClassifierEvaluator;
import org.api4.java.algorithm.Timeout;

import com.google.common.eventbus.EventBus;

import de.upb.ml2plan.IListenable;

public class RandomSearch implements IListenable {

	private final EventBus eventBus = new EventBus();

	private final int numCPUs;
	private final IClassifierEvaluator evaluator;
	private final Timeout timeout;

	private long finishByTimestamp;

	public RandomSearch(final int numCPUs, final Timeout timeout, final Timeout candidateTimeout, final IClassifierEvaluator evaluator) {
		this.numCPUs = numCPUs;
		this.timeout = timeout;
		this.evaluator = evaluator;
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

		public RandomSearchWorker(final int id) {
			this.id = id;
		}

		@Override
		public void run() {

		}
	}

}

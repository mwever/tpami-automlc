package ai.libs.hyperopt.example.meka;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.api4.java.algorithm.Timeout;
import org.api4.java.common.attributedobjects.ObjectEvaluationFailedException;

import ai.libs.hyperopt.api.ILoggingObjectEvaluator;
import ai.libs.hyperopt.experimenter.AutoMLCExperimenter;
import ai.libs.jaicore.ml.classification.multilabel.learner.IMekaClassifier;
import ai.libs.jaicore.ml.weka.WekaUtil;
import ai.libs.jaicore.ml.weka.dataset.IWekaInstances;
import ai.libs.jaicore.ml.weka.dataset.WekaInstances;
import meka.classifiers.multilabel.Evaluation;
import meka.core.Metrics;
import meka.core.Result;
import weka.core.Instances;

public class MekaClassifierEvaluator implements ILoggingObjectEvaluator<IMekaClassifier, Double> {

	private final Instances dataset;
	private final long seed;
	private final int iterations;
	private final double splitRatio;
	private final String measure;
	private final Timeout timeout;
	private Map<String, Eval> evals = new HashMap<>();

	public MekaClassifierEvaluator(final Instances dataset, final String algorithm, final long seed, final int iterations, final double splitRatio, final String measure, final Timeout timeout) {
		this.dataset = dataset;
		this.seed = seed;
		this.iterations = iterations;
		this.splitRatio = splitRatio;
		this.measure = measure;
		this.timeout = timeout;

		this.evals.put("FMacroAvgD", (gt, pred) -> 1 - Metrics.P_FmacroAvgD(gt, pred));
		this.evals.put("FMacroAvgL", (gt, pred) -> 1 - Metrics.P_FmacroAvgL(gt, pred));
		this.evals.put("FMicroAvg", (gt, pred) -> 1 - Metrics.P_FmicroAvg(gt, pred));
		this.evals.put("ExactMatch", (gt, pred) -> 1 - Metrics.P_ExactMatch(gt, pred));
		this.evals.put("HammingLoss", (gt, pred) -> Metrics.L_Hamming(gt, pred));
		this.evals.put("JaccardIndex", (gt, pred) -> 1 - Metrics.P_JaccardIndex(gt, pred));
	}

	@Override
	public Double evaluate(final IMekaClassifier object) throws ObjectEvaluationFailedException, InterruptedException {
		return this.evaluate(object, new HashMap<>());
	}

	private interface Eval {
		public double eval(int[][] gt, int[][] pred);
	}

	@Override
	public double evaluate(final IMekaClassifier object, final Map<String, DescriptiveStatistics> log) throws ObjectEvaluationFailedException, InterruptedException {
		Random rand = new Random(this.seed);
		Semaphore sem = new Semaphore(0);

		List<Throwable> exceptionsOfThread = new LinkedList<>();

		Thread evaluator = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					for (int i = 0; i < MekaClassifierEvaluator.this.iterations; i++) {
						List<IWekaInstances> split = WekaUtil.realizeSplit(new WekaInstances(MekaClassifierEvaluator.this.dataset),
								WekaUtil.getArbitrarySplit(new WekaInstances(MekaClassifierEvaluator.this.dataset), rand, MekaClassifierEvaluator.this.splitRatio));
						long startTime = System.currentTimeMillis();
						Result res = Evaluation.evaluateModel(object.getClassifier(), split.get(0).getInstances(), split.get(1).getInstances());
						log.computeIfAbsent("evalTime", t -> new DescriptiveStatistics()).addValue(System.currentTimeMillis() - startTime);
						int[][] gt = res.allTrueValues();
						int[][] pred = res.allPredictions(AutoMLCExperimenter.LABEL_THRESHOLD);

						for (String measureName : MekaClassifierEvaluator.this.evals.keySet()) {
							double measureValue = MekaClassifierEvaluator.this.evals.get(measureName).eval(gt, pred);
							log.computeIfAbsent(measureName, t -> new DescriptiveStatistics()).addValue(measureValue);
						}
					}
				} catch (Exception e) {
					exceptionsOfThread.add(e);
				} finally {
					sem.release();
				}
			}
		});
		evaluator.start();

		boolean done;
		try {
			done = sem.tryAcquire(this.timeout.milliseconds(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			evaluator.interrupt();
			throw e;
		}

		if (!done) {
			evaluator.interrupt();
			throw new ObjectEvaluationFailedException("Timeout");
		} else if (!exceptionsOfThread.isEmpty() && !(exceptionsOfThread.get(0) instanceof InterruptedException)) {
			throw new ObjectEvaluationFailedException(exceptionsOfThread.get(0));
		}

		return log.get(MekaClassifierEvaluator.this.measure).getMean();
	}
}

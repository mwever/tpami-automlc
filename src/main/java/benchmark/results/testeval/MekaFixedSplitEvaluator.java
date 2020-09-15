package benchmark.results.testeval;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.api4.java.common.attributedobjects.ObjectEvaluationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.jaicore.ml.classification.multilabel.learner.IMekaClassifier;
import ai.libs.jaicore.ml.weka.WekaUtil;
import benchmark.core.api.IHyperoptObjectEvaluator;
import benchmark.core.api.ILoggingObjectEvaluator;
import benchmark.meka.experimenter.AutoMLCExperimenter;
import benchmark.meka.impl.MekaClassifierEvaluator;
import meka.classifiers.multilabel.Evaluation;
import meka.core.Metrics;
import meka.core.Result;
import weka.core.Instances;

public class MekaFixedSplitEvaluator implements ILoggingObjectEvaluator<IMekaClassifier>, IHyperoptObjectEvaluator<IMekaClassifier> {
	private static final Logger LOGGER = LoggerFactory.getLogger(MekaClassifierEvaluator.class);

	private final Instances train;
	private final Instances test;
	private final String measure;
	private Map<String, Eval> evals = new HashMap<>();

	public MekaFixedSplitEvaluator(final Instances train, final Instances test, final String algorithm, final String measure) {
		this.train = train;
		this.test = test;
		this.measure = measure;

		this.evals.put("FMacroAvgD", (gt, pred) -> Metrics.P_FmacroAvgD(gt, pred));
		this.evals.put("FMacroAvgL", (gt, pred) -> Metrics.P_FmacroAvgL(gt, pred));
		this.evals.put("FMicroAvg", (gt, pred) -> Metrics.P_FmicroAvg(gt, pred));
		this.evals.put("ExactMatch", (gt, pred) -> Metrics.P_ExactMatch(gt, pred));
		this.evals.put("HammingLoss", (gt, pred) -> Metrics.L_Hamming(gt, pred));
		this.evals.put("JaccardIndex", (gt, pred) -> Metrics.P_JaccardIndex(gt, pred));
	}

	@Override
	public Double evaluate(final IMekaClassifier object) throws ObjectEvaluationFailedException, InterruptedException {
		return this.evaluate(object, new HashMap<>());
	}

	private interface Eval {
		public double eval(int[][] gt, int[][] pred);
	}

	@Override
	public Double evaluate(final IMekaClassifier object, final Map<String, DescriptiveStatistics> log, final int budget) throws ObjectEvaluationFailedException, InterruptedException {
		try {
			LOGGER.debug("Start evaluating candidate {}", WekaUtil.getClassifierDescriptor(object.getClassifier()));
			long startTime = System.currentTimeMillis();
			Result res = Evaluation.evaluateModel(object.getClassifier(), new Instances(this.train), new Instances(this.test));
			log.computeIfAbsent("evalTime", t -> new DescriptiveStatistics()).addValue(System.currentTimeMillis() - startTime);

			LOGGER.debug("Assess performance values for evaluated candidate.");
			int[][] gt = res.allTrueValues();
			int[][] pred = res.allPredictions(AutoMLCExperimenter.LABEL_THRESHOLD);

			for (String measureName : this.evals.keySet()) {
				double measureValue = this.evals.get(measureName).eval(gt, pred);
				log.computeIfAbsent(measureName, t -> new DescriptiveStatistics()).addValue(measureValue);
			}
		} catch (Exception e) {
			throw new ObjectEvaluationFailedException(e);
		}
		return log.get(this.measure).getMean();
	}

	@Override
	public int getMaxBudget() {
		return 1;
	}

}

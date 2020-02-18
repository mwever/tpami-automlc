package ai.libs.hyperopt;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.api4.java.algorithm.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.serialization.ComponentLoader;
import ai.libs.hyperopt.impl.HASCOToPCSConverter;
import ai.libs.hyperopt.impl.model.OptimizationTask;
import ai.libs.hyperopt.impl.optimizer.HyperBandOptimizer;
import ai.libs.jaicore.ml.weka.classification.learner.IWekaClassifier;
import ai.libs.mlplan.multiclass.wekamlplan.weka.WekaPipelineFactory;

public class PCSBasedOptimizationRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(PCSBasedOptimizationRunner.class);

	public static void main(final String[] args) throws Exception {
		// initialize
		File HASCOFileInput = new File("../mlplan/resources/automl/searchmodels/weka/autoweka.json");
		ComponentLoader cl = new ComponentLoader(HASCOFileInput);
		Collection<Component> components = cl.getComponents();
		String requestedInterface = "BaseClassifier";
		ComponentInstanceEvaluator evaluator = new ComponentInstanceEvaluator("testrsc/iris.arff", "HyperBandOptimizer");
		OptimizationTask<IWekaClassifier> input = new OptimizationTask<IWekaClassifier>(new WekaPipelineFactoryConverter(), evaluator, 4, components, requestedInterface, new Timeout(600, TimeUnit.SECONDS),
				new Timeout(30, TimeUnit.SECONDS));
		WekaPipelineFactory classifierFactory = new WekaPipelineFactory();

		// generate PCS files
		HASCOToPCSConverter.generatePCSFile(input, "PCSBasedOptimizerScripts/HyperBandOptimizer/");

		// optimization
		HyperBandOptimizer optimizer = HyperBandOptimizer.HyperBandOptimizerBuilder(input, evaluator).executionPath("PCSBasedOptimizerScripts/HyperBandOptimizer").maxBudget(230.0).minBudget(9.0).nIterations(4).build();

		optimizer.optimize("weka.classifiers.functions.Logistic");

	}

}

//package ai.libs.hyperopt;
//
//import java.io.File;
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.TimeUnit;
//
//import org.aeonbits.owner.ConfigFactory;
//import org.api4.java.algorithm.Timeout;
//import org.junit.Test;
//
//import ai.libs.hasco.model.Component;
//import ai.libs.hasco.serialization.ComponentLoader;
//import ai.libs.hyperopt.impl.model.OptimizationTask;
//import ai.libs.hyperopt.impl.optimizer.pcs.smac.SMACOptimizer;
//import ai.libs.hyperopt.impl.optimizer.pcs.smac.SMACOptimizerConfig;
//import ai.libs.jaicore.ml.weka.classification.learner.IWekaClassifier;
//
//public class SMACOptimizerTest {
//
//	@Test
//	public void testPrepareOptimization() throws Exception {
//		ComponentLoader cl = new ComponentLoader(new File("testrsc/weka/weka-singlelabel-base.json"));
//		Collection<Component> components = cl.getComponents();
//		Timeout globalTimeout = new Timeout(10, TimeUnit.MINUTES);
//		Timeout evaluationTimeout = new Timeout(30, TimeUnit.SECONDS);
//
//		OptimizationTask<IWekaClassifier> task = new OptimizationTask<>(new WekaPipelineFactoryConverter(), new ComponentInstanceEvaluator("testrsc/iris.arff", "bla"), 1, components, "weka.classifiers.functions.SMO", globalTimeout,
//				evaluationTimeout);
//
//		Map<String, String> propertiesToLoad = new HashMap<>();
//		propertiesToLoad.put(SMACOptimizerConfig.K_SMAC_EXEC_PATH, "testrsc/test/");
//
//		SMACOptimizerConfig config = ConfigFactory.create(SMACOptimizerConfig.class, propertiesToLoad);
//
//		SMACOptimizer<IWekaClassifier> smac = new SMACOptimizer<>(config, task);
//		smac.prepareOptimization();
//
//		smac.runOptimization();
//	}
//
//}

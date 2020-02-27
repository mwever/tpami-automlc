package ai.libs.hyperopt.example.meka;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.aeonbits.owner.ConfigFactory;
import org.api4.java.algorithm.Timeout;
import org.api4.java.algorithm.exceptions.AlgorithmException;
import org.api4.java.algorithm.exceptions.AlgorithmExecutionCanceledException;
import org.api4.java.algorithm.exceptions.AlgorithmTimeoutedException;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.serialization.ComponentLoader;
import ai.libs.hyperopt.api.input.IOptimizerConfig;
import ai.libs.hyperopt.impl.optimizer.htn.BestFirstOptimizer;

public class MekaHBExample {

	private static final Timeout GLOBAL_TIMEOUT = new Timeout(10, TimeUnit.MINUTES);
	private static final Timeout EVALUATION_TIMEOUT = new Timeout(2, TimeUnit.MINUTES);

	private static final int NUM_CPUS = 4;

	private static final File COMPONENTS_FILE = new File("testrsc/meka/mlplan-meka.json");
	private static final String REQ_INTERFACE = "MLClassifier";

	public static void main(final String[] args) throws IOException, AlgorithmTimeoutedException, InterruptedException, AlgorithmExecutionCanceledException, AlgorithmException {
		ComponentLoader cl = new ComponentLoader(COMPONENTS_FILE);
		Collection<Component> components = cl.getComponents();
		MekaOptimizationTask task = new MekaOptimizationTask(new MekaClassifierConverter(), new MekaClassifierEvaluator(), components, REQ_INTERFACE, GLOBAL_TIMEOUT, EVALUATION_TIMEOUT, cl.getParamConfigs());

		IOptimizerConfig config = ConfigFactory.create(IOptimizerConfig.class);
		config.setProperty(IOptimizerConfig.K_CPUS, NUM_CPUS + "");
		new BestFirstOptimizer(config, task).call();
	}

}

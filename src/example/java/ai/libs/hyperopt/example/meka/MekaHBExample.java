package ai.libs.hyperopt.example.meka;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.api4.java.algorithm.Timeout;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.serialization.ComponentLoader;

public class MekaHBExample {

	private static final Timeout GLOBAL_TIMEOUT = new Timeout(10, TimeUnit.MINUTES);
	private static final Timeout EVALUATION_TIMEOUT = new Timeout(2, TimeUnit.MINUTES);

	private static final int NUM_CPUS = 4;

	private static final File COMPONENTS_FILE = new File("testrsc/meka/mlplan-meka.json");
	private static final String REQ_INTERFACE = "MLClassifier";

	public static void main(final String[] args) throws IOException {
		Collection<Component> components = new ComponentLoader(COMPONENTS_FILE).getComponents();
		MekaOptimizationTask task = new MekaOptimizationTask(new MekaClassifierConverter(), new MekaClassifierEvaluator(), NUM_CPUS, components, REQ_INTERFACE, GLOBAL_TIMEOUT, EVALUATION_TIMEOUT);
	}

}

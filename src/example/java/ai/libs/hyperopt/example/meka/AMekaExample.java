package ai.libs.hyperopt.example.meka;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.aeonbits.owner.ConfigFactory;
import org.api4.java.algorithm.Timeout;
import org.api4.java.algorithm.exceptions.AlgorithmException;
import org.api4.java.algorithm.exceptions.AlgorithmExecutionCanceledException;
import org.api4.java.algorithm.exceptions.AlgorithmTimeoutedException;

import com.google.common.eventbus.Subscribe;

import ai.libs.hasco.serialization.ComponentLoader;
import ai.libs.hyperopt.api.IOptimizer;
import ai.libs.hyperopt.api.input.IOptimizerConfig;
import ai.libs.hyperopt.api.input.IPlanningOptimizationTask;
import ai.libs.jaicore.ml.classification.multilabel.learner.IMekaClassifier;

public abstract class AMekaExample {

	private static final File SEARCH_SPACE_FILE = new File("testrsc/meka/mlplan-meka.json");
	private static final String REQUESTED_INTERFACE = "MLClassifier";
	private static final Timeout GLOBAL_TIMEOUT = new Timeout(10, TimeUnit.MINUTES);
	private static final Timeout EVAL_TIMEOUT = new Timeout(1, TimeUnit.MINUTES);

	private static final int NUM_CPUS = 4;

	private final ComponentLoader cl;

	protected AMekaExample() throws IOException, AlgorithmTimeoutedException, InterruptedException, AlgorithmExecutionCanceledException, AlgorithmException {
		this.cl = new ComponentLoader(SEARCH_SPACE_FILE);
		IOptimizer opt = this.getOptimizer();
		opt.registerListener(this);
		opt.call();
	}

	@Subscribe
	public void rcvEvent(final Object event) {
		System.err.println("### Received event: " + event.getClass().getName());
	}

	public IPlanningOptimizationTask<IMekaClassifier> getTask() {
		MekaClassifierConverter converter = new MekaClassifierConverter();
		MekaClassifierEvaluator evaluator = new MekaClassifierEvaluator();
		return new MekaOptimizationTask(converter, evaluator, this.cl.getComponents(), REQUESTED_INTERFACE, GLOBAL_TIMEOUT, EVAL_TIMEOUT, this.cl.getParamConfigs());
	}

	public IOptimizerConfig getConfig() {
		IOptimizerConfig config = ConfigFactory.create(IOptimizerConfig.class);
		config.setProperty(IOptimizerConfig.K_CPUS, NUM_CPUS + "");
		return config;
	}

	public abstract IOptimizer<?, IMekaClassifier> getOptimizer();

}

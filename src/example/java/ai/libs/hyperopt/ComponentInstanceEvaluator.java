package ai.libs.hyperopt;

import java.util.List;
import java.util.Map;

import org.api4.java.ai.ml.core.dataset.splitter.SplitFailedException;
import org.api4.java.common.attributedobjects.IObjectEvaluator;
import org.api4.java.common.attributedobjects.ObjectEvaluationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

import ai.libs.hasco.exceptions.ComponentInstantiationFailedException;
import ai.libs.jaicore.graphvisualizer.events.graph.bus.AlgorithmEventListener;
import ai.libs.jaicore.ml.weka.WekaUtil;
import ai.libs.jaicore.ml.weka.classification.learner.IWekaClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

/**
 *
 * @author kadirayk
 *
 */
public class ComponentInstanceEvaluator implements IObjectEvaluator<IWekaClassifier, Double> {

	private Logger logger = LoggerFactory.getLogger(ComponentInstanceEvaluator.class);

	private String algorithmId;

	private String filePath;

	private EventBus eventBus;

	private List<Instances> split;

	// we create artifical parameter names for dependend parameters, because
	// parameter names should be unique in pcs files.
	private Map<String, String> parameterMapping;

	public ComponentInstanceEvaluator(final String filePath, final String algorithmId) {
		this.filePath = filePath;
		this.eventBus = new EventBus();
		this.algorithmId = algorithmId;
		Instances dataset = this.loadDataset(filePath);
		try {
			this.split = WekaUtil.getStratifiedSplit(dataset, 0, .7f);
		} catch (SplitFailedException | InterruptedException e) {
			this.logger.error(e.getMessage());
		}
	}

	public Map<String, String> getParameterMapping() {
		return this.parameterMapping;
	}

	public void setParameterMapping(final Map<String, String> paramMap) {
		this.parameterMapping = paramMap;
	}

	/**
	 * Concrete compontentInstance evaluated
	 */
	@Override
	public Double evaluate(final IWekaClassifier componentInstance) throws InterruptedException, ObjectEvaluationFailedException {
		Double score = 0.0;
		try {
			Classifier classifier = componentInstance.getClassifier();
			Evaluation eval = null;
			try {

				// Normalize dataset
				classifier.buildClassifier(this.split.get(0));
				eval = new Evaluation(this.split.get(0));
				eval.evaluateModel(classifier, this.split.get(1));
				score = eval.pctIncorrect();
				System.out.println("score:" + score);
				System.out.println("comp:" + componentInstance);
			} catch (Exception e) {
				this.logger.error(e.getMessage());
			}
		} catch (ComponentInstantiationFailedException e) {
			this.logger.error(e.getMessage());
		}
		return score;
	}

	public List<Instances> getInstances() {
		return this.split;
	}

	private Instances loadDataset(final String path) {
		Instances dataset = null;
		try {
			dataset = DataSource.read(path);
			if (dataset.classIndex() == -1) {
				dataset.setClassIndex(dataset.numAttributes() - 1);
			}
		} catch (Exception e) {
			this.logger.error(e.getMessage());
		}

		return dataset;
	}

	public void registerListener(final AlgorithmEventListener listener) {
		this.eventBus.register(listener);
	}

	public void UnregisterListener(final AlgorithmEventListener listener) {
		this.eventBus.unregister(listener);
	}

}
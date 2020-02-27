package ai.libs.hyperopt.example.weka;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

import org.api4.java.common.attributedobjects.IObjectEvaluator;
import org.api4.java.common.attributedobjects.ObjectEvaluationFailedException;

import ai.libs.jaicore.ml.weka.classification.learner.IWekaClassifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;

public class WekaClassifierEvaluator implements IObjectEvaluator<IWekaClassifier, Double> {

	private Instances dataset;

	public WekaClassifierEvaluator(final File datasetFile) throws IOException {
		this.dataset = new Instances(new FileReader(datasetFile));
		this.dataset.setClassIndex(this.dataset.numAttributes() - 1);
	}

	@Override
	public Double evaluate(final IWekaClassifier object) throws InterruptedException, ObjectEvaluationFailedException {
		try {
			Evaluation eval = new Evaluation(this.dataset);
			eval.crossValidateModel(object.getClassifier(), this.dataset, 10, new Random(42));
			return eval.errorRate();
		} catch (InterruptedException e) {
			throw e;
		} catch (Exception e) {
			throw new ObjectEvaluationFailedException(e);
		}
	}

}

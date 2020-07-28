import java.io.File;
import java.io.FileReader;
import java.util.List;

import org.aeonbits.owner.ConfigFactory;
import org.api4.java.datastructure.kvstore.IKVStore;

import ai.libs.hyperopt.experimenter.IAutoMLCExperimentConfig;
import ai.libs.hyperopt.util.ComponentInstanceAdapter;
import ai.libs.jaicore.components.model.ComponentInstance;
import ai.libs.jaicore.components.serialization.ComponentLoader;
import ai.libs.jaicore.db.sql.SQLAdapter;
import ai.libs.jaicore.ml.classification.multilabel.learner.IMekaClassifier;
import ai.libs.mlplan.multilabel.mekamlplan.MekaPipelineFactory;
import meka.classifiers.multilabel.Evaluation;
import meka.core.MLUtils;
import meka.core.Metrics;
import meka.core.Result;
import weka.core.Instances;

public class PostProcessorTest {
	public static void main(final String[] args) throws Exception {

		SQLAdapter adapter = new SQLAdapter(ConfigFactory.create(IAutoMLCExperimentConfig.class));

		List<IKVStore> res = adapter.getResultsOfQuery("SELECT * FROM test_eval WHERE id =108");
		String ciString = res.get(0).getAsString("component_instance");

		ComponentLoader cl = new ComponentLoader(new File("testrsc/meka/mlplan-meka.json"));

		ComponentInstanceAdapter ciadapter = new ComponentInstanceAdapter(cl.getComponents());
		ComponentInstance ci = ciadapter.stringToComponentInstance(ciString);

		MekaPipelineFactory factory = new MekaPipelineFactory();
		IMekaClassifier classifier = factory.getComponentInstantiation(ci);

		Instances trainDataset = new Instances(new FileReader(new File(new File("datasets"), "yeast_42_2_train.arff")));
		Instances testDataset = new Instances(new FileReader(new File(new File("datasets"), "yeast_42_2_test.arff")));
		MLUtils.prepareData(trainDataset);
		MLUtils.prepareData(testDataset);

		Result evalRes = Evaluation.evaluateModel(classifier.getClassifier(), trainDataset, testDataset);

		System.out.println(Metrics.P_FmacroAvgD(evalRes.allTrueValues(), evalRes.allPredictions(0.5)));

		// 0.6261585821585843
		adapter.close();
		//
	}

}

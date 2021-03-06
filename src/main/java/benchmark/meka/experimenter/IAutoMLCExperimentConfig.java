package benchmark.meka.experimenter;

import java.io.File;

import org.aeonbits.owner.Config.Sources;

import ai.libs.jaicore.db.IDatabaseConfig;
import ai.libs.jaicore.experiments.IExperimentSetConfig;

@Sources({ "file:automlc-setup.properties" })
public interface IAutoMLCExperimentConfig extends IExperimentSetConfig, IDatabaseConfig {

	public static final String K_DATASET_FOLDER = "datasetFolder";
	public static final String K_COMPONENT_FILE = "componentFile";

	@Key(K_DATASET_FOLDER)
	public File getDatasetFolder();

	@Key(K_COMPONENT_FILE)
	public File getComponentFile();

}

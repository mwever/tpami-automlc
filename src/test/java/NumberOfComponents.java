import java.io.File;
import java.io.IOException;

import ai.libs.hasco.model.ComponentUtil;
import ai.libs.hasco.serialization.ComponentLoader;

public class NumberOfComponents {

	public static void main(final String[] args) throws IOException {

		ComponentLoader cl = new ComponentLoader(new File("testrsc/meka/mlplan-meka.json"));
		System.out.println(ComponentUtil.getAllAlgorithmSelectionInstances("MLClassifier", cl.getComponents()).size());

		System.out.println(ComponentUtil.getAllAlgorithmSelectionInstances("AbstractClassifier", cl.getComponents()).size());
	}

}

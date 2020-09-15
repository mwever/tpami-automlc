import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Random;

import org.junit.Test;

import ai.libs.jaicore.components.exceptions.ComponentInstantiationFailedException;
import ai.libs.jaicore.components.model.Component;
import ai.libs.jaicore.components.model.ComponentInstance;
import ai.libs.jaicore.components.model.ComponentUtil;
import ai.libs.jaicore.components.serialization.ComponentLoader;
import ai.libs.mlplan.multiclass.wekamlplan.weka.WekaPipelineFactory;

public class InstantiationTest {

	@Test
	public void testInstantiation() throws IOException, ComponentInstantiationFailedException {
		Collection<Component> components = new ComponentLoader(new File("searchspace/weka-singlelabel-base.json")).getComponents();
		Component sgd = ComponentUtil.getComponentsProvidingInterface(components, "weka.classifiers.functions.SGD").iterator().next();
		ComponentInstance ci = ComponentUtil.getDefaultParameterizationOfComponent(sgd);

		WekaPipelineFactory factory = new WekaPipelineFactory();
		factory.getComponentInstantiation(ci);

		for (int i = 0; i < 100; i++) {
			factory.getComponentInstantiation(ComponentUtil.getRandomParameterizationOfComponent(sgd, new Random(i)));
		}
	}

}

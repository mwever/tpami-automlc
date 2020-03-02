import java.io.File;

import ai.libs.hasco.serialization.ComponentLoader;
import ai.libs.hyperopt.impl.HASCOToPCSConverter;

public class HASCO2PCSConverterTest {
	private static final String REQUESTED_INTERFACE = "AbstractClassifier";

	public static void main(final String[] args) throws Exception {
		ComponentLoader cl = new ComponentLoader(new File("testrsc/meka/mlplan-meka.json"));
		HASCOToPCSConverter converter = new HASCOToPCSConverter(cl.getComponents(), REQUESTED_INTERFACE);
		converter.generatePCSFile(new File("test/out.pcs"));
//		System.out.println(HASCOToPCSConverter.paramActivationCondition("ns", "pName", "iface", "dom1,dom2,dom3"));
	}

}

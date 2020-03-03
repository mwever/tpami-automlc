import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.model.ComponentUtil;
import ai.libs.hasco.serialization.ComponentLoader;
import ai.libs.hyperopt.impl.optimizer.pcs.HASCOToPCSConverter;

public class HASCO2PCSConverterTest {
	private static final String REQUESTED_INTERFACE = "MLClassifier";

	public static void main(final String[] args) throws Exception {
		ComponentLoader cl = new ComponentLoader(new File("testrsc/meka/mlplan-meka.json"));
		HASCOToPCSConverter converter = new HASCOToPCSConverter(cl.getComponents(), REQUESTED_INTERFACE);
		converter.generatePCSFile(new File("test/out.pcs"));
		
		List<ComponentInstance> ciList = new ArrayList<>(ComponentUtil.getAllAlgorithmSelectionInstances(converter.getRequestedInterface(), converter.getComponents()));
		Random rand = new Random(42);
		ComponentInstance ci = ciList.get(rand.nextInt(ciList.size()));
		System.out.println(ci);
		Map<String, String> paramMap = ciToMap(ci);
		ComponentInstance ciRe = converter.getComponentInstanceFromMap(paramMap);
		System.out.println(ciRe);
		
		System.out.println(ci.toString().equals(ciRe.toString()));
		
//		System.out.println(HASCOToPCSConverter.paramActivationCondition("ns", "pName", "iface", "dom1,dom2,dom3"));
	}
	
	private static Map<String, String> ciToMap(final ComponentInstance ci) {
		Map<String, String> paramMap = new HashMap<>();
		ci.getParameterValues().entrySet().forEach(x -> paramMap.put(ci.getComponent().getName()+"."+x.getKey(), x.getValue()));
		ci.getSatisfactionOfRequiredInterfaces().entrySet().forEach(x -> paramMap.put(ci.getComponent().getName()+"."+x.getKey(), x.getValue().getComponent().getName()));
		ci.getSatisfactionOfRequiredInterfaces().values().stream().map(HASCO2PCSConverterTest::ciToMap).forEach(paramMap::putAll);
		return paramMap;
	}

}

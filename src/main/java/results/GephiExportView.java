package results;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentUtil;
import ai.libs.hasco.serialization.ComponentLoader;

public class GephiExportView {
	private static final boolean DAG = true;
	private static final boolean SINGLE_EDGE = false;
	private static final boolean WITHOUT_SLC = false;
	private static final boolean MLC_SLC = false; // true: MLC, false: SLC

	private static double nodeIdCounter = 0.0;
	private static double edgeIdCounter = 0.0;
	private static StringBuilder nodeBuilder = new StringBuilder();
	private static StringBuilder edgeBuilder = new StringBuilder();

	private static Map<String, Double> nodeCache = new HashMap<>();
	private static Map<String, Double> edgeCache = new HashMap<>();

	public static void main(final String[] args) throws IOException {
		String requestedInterface = MLC_SLC ? "MLClassifier" : "AbstractClassifier";
		ComponentLoader cl = new ComponentLoader(new File("testrsc/meka/mlplan-meka.json"));
		appendCIs(cl.getComponents(), requestedInterface, createNode("root"));

		System.out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<gexf xmlns:viz=\"http:///www.gexf.net/1.1draft/viz\" version=\"1.1\" xmlns=\"http://www.gexf.net/1.1draft\">\n" + "<meta lastmodifieddate=\"2010-03-03+23:44\">\n"
				+ "<creator>Gephi 0.7</creator>");
		System.out.println("</meta>");
		System.out.println("<graph defaultedgetype=\"undirected\" idtype=\"string\" type=\"static\">");
		System.out.println("<nodes count=\"" + ((int) nodeIdCounter) + "\">");
		System.out.println(nodeBuilder.toString());
		System.out.println("</nodes>");
		System.out.println("<edges count=\"" + ((int) edgeIdCounter) + "\">");
		System.out.print(edgeBuilder.toString());
		System.out.println("</edges>");
		System.out.println("</graph>");
		System.out.println("</gexf>");

		System.out.println(edgeIdCounter - ComponentUtil.getComponentsProvidingInterface(cl.getComponents(), requestedInterface).size());
	}

	private static void appendCIs(final Collection<Component> compCol, final String requiredInterface, final double parentNode) {
		for (Component comp : ComponentUtil.getComponentsProvidingInterface(compCol, requiredInterface)) {
			double childNode = createNode(comp.getName());
			createEdge(parentNode, childNode);
			for (Entry<String, String> reqI : comp.getRequiredInterfaces().entrySet()) {
				if (WITHOUT_SLC && reqI.getValue().equals("AbstractClassifier")) {
					continue;
				}
				appendCIs(compCol, reqI.getValue(), childNode);
			}
		}
	}

	private static double createEdge(final double source, final double target) {
		if (DAG && SINGLE_EDGE && edgeCache.containsKey(source + "#" + target)) {
			return edgeCache.get(source + "#" + target);
		}

		double edgeID = edgeIdCounter;
		edgeIdCounter += 1.0;
		edgeCache.put(source + "#" + target, edgeID);
		edgeBuilder.append("<edge id=\"").append(edgeID).append("\" source=\"").append(source).append("\" target=\"").append(target).append("\" />\n");
		return edgeID;
	}

	private static double createNode(final String label) {
		if (DAG && nodeCache.containsKey(label)) {
			return nodeCache.get(label);
		}

		double nodeID = nodeIdCounter;
		nodeIdCounter += 1.0;
		nodeCache.put(label, nodeID);
		nodeBuilder.append("<node id=\"").append(nodeID).append("\" label=\"").append(label).append("\" />\n");
		return nodeID;
	}

}

package benchmark.results.searchspace;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ai.libs.jaicore.components.model.Component;
import ai.libs.jaicore.components.model.ComponentUtil;
import ai.libs.jaicore.components.model.Interface;
import ai.libs.jaicore.components.serialization.ComponentLoader;
import benchmark.results.ResultsConfig;

/**
 * Exports dependency graphs defined by the searchspace description to GEPHI format.
 *
 * @author mwever
 *
 */
public class GephiExportView {

	private static final File BASE_FOLDER = new File("results/gephi-export/");

	private boolean singleEdge = false;
	private boolean withoutSLC = false;

	private double nodeIdCounter = 0.0;
	private double edgeIdCounter = 0.0;
	private static StringBuilder nodeBuilder = new StringBuilder();
	private static StringBuilder edgeBuilder = new StringBuilder();

	private String gephiGraphXML;

	private static Map<String, Double> nodeCache = new HashMap<>();
	private static Map<String, Double> edgeCache = new HashMap<>();

	public GephiExportView(final boolean singleEdge, final boolean withoutSLC, final boolean mlcSLC, final ComponentLoader cl) {
		this.singleEdge = singleEdge;
		this.withoutSLC = withoutSLC;

		String requestedInterface = mlcSLC ? "MLClassifier" : "AbstractClassifier";

		this.appendCIs(cl.getComponents(), requestedInterface, this.createNode("root"));
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<gexf xmlns:viz=\"http:///www.gexf.net/1.1draft/viz\" version=\"1.1\" xmlns=\"http://www.gexf.net/1.1draft\">\n" + "<meta lastmodifieddate=\"2010-03-03+23:44\">\n"
				+ "<creator>Gephi 0.7</creator>");
		sb.append("</meta>");
		sb.append("<graph defaultedgetype=\"undirected\" idtype=\"string\" type=\"static\">");
		sb.append("<nodes count=\"" + ((int) this.nodeIdCounter) + "\">");
		sb.append(nodeBuilder.toString());
		sb.append("</nodes>");
		sb.append("<edges count=\"" + ((int) this.edgeIdCounter) + "\">");
		sb.append(edgeBuilder.toString());
		sb.append("</edges>");
		sb.append("</graph>");
		sb.append("</gexf>");

		this.gephiGraphXML = sb.toString();
	}

	public String getGephiGraphXML() {
		return this.gephiGraphXML;
	}

	private void appendCIs(final Collection<Component> compCol, final String requiredInterface, final double parentNode) {
		for (Component comp : ComponentUtil.getComponentsProvidingInterface(compCol, requiredInterface)) {
			double childNode = this.createNode(comp.getName());
			this.createEdge(parentNode, childNode);
			for (Interface reqI : comp.getRequiredInterfaces()) {
				if (this.withoutSLC && reqI.getName().equals("AbstractClassifier")) {
					continue;
				}
				this.appendCIs(compCol, reqI.getName(), childNode);
			}
		}
	}

	private double createEdge(final double source, final double target) {
		if (this.singleEdge && edgeCache.containsKey(source + "#" + target)) {
			return edgeCache.get(source + "#" + target);
		}

		double edgeID = this.edgeIdCounter;
		this.edgeIdCounter += 1.0;
		edgeCache.put(source + "#" + target, edgeID);
		edgeBuilder.append("<edge id=\"").append(edgeID).append("\" source=\"").append(source).append("\" target=\"").append(target).append("\" />\n");
		return edgeID;
	}

	private double createNode(final String label) {
		if (nodeCache.containsKey(label)) {
			return nodeCache.get(label);
		}
		double nodeID = this.nodeIdCounter;
		this.nodeIdCounter += 1.0;
		nodeCache.put(label, nodeID);
		nodeBuilder.append("<node id=\"").append(nodeID).append("\" label=\"").append(label).append("\" />\n");
		return nodeID;
	}

	public static void main(final String[] args) throws IOException {
		BASE_FOLDER.mkdirs();
		ComponentLoader cl = new ComponentLoader(ResultsConfig.SEARCH_SPACE_ROOT_FILE);

		boolean[][] paramVector = { { true, true, false }, { true, true, true }, { true, false, true } };
		File[] outputFiles = { new File(BASE_FOLDER, "slc-only.gephi"), new File(BASE_FOLDER, "mlc-only.gephi"), new File(BASE_FOLDER, "mlc-complete.gephi") };

		for (int i = 0; i < paramVector.length; i++) {
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFiles[i]))) {
				bw.write(new GephiExportView(paramVector[i][0], paramVector[i][1], paramVector[i][2], cl).getGephiGraphXML());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}

package benchmark.results.searchspace;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.libs.jaicore.basic.ValueUtil;
import ai.libs.jaicore.components.model.CategoricalParameterDomain;
import ai.libs.jaicore.components.model.Component;
import ai.libs.jaicore.components.model.ComponentInstance;
import ai.libs.jaicore.components.model.ComponentUtil;
import ai.libs.jaicore.components.model.Interface;
import ai.libs.jaicore.components.model.NumericParameterDomain;
import ai.libs.jaicore.components.model.Parameter;
import ai.libs.jaicore.components.serialization.ComponentLoader;
import benchmark.results.ResultsConfig;

public class SearchSpaceDescription {

	private static final String DEFAULT_SETTING = "meka";
	private static final String DEFAULT_OUTPUT_FILE = "searchspace.html";

	private static final List<String> SORTING = Arrays.asList("MLClassifier", "MetaMLClassifier", "BasicMLClassifier", "AbstractClassifier", "MetaClassifier", "BaseClassifier", "K");

	private static String requiredInterface;

	public static void main(final String[] args) throws IOException {
		String setting = args.length > 0 ? args[0] : DEFAULT_SETTING;
		String outFile = args.length > 1 ? args[1] : DEFAULT_OUTPUT_FILE;

		Collection<Component> components;
		switch (setting) {
		case "meka":
			components = new ComponentLoader(ResultsConfig.MLC_SEARCH_SPACE_ROOT_FILE).getComponents();
			requiredInterface = ResultsConfig.MLC_REQUESTED_INTERFACE;
			break;
		case "weka":
			components = new ComponentLoader(ResultsConfig.SLC_SEARCH_SPACE_ROOT_FILE).getComponents();
			requiredInterface = ResultsConfig.SLC_REQUESTED_INTERFACE;
			break;
		default:
			throw new IllegalArgumentException("Unknown setting to generate search space description for.");
		}

		StringBuilder sb = new StringBuilder();
		sb.append(
				"<html>\n<head>\n<link rel=\"stylesheet\" href=\"https://stackpath.bootstrapcdn.com/bootstrap/4.5.0/css/bootstrap.min.css\" integrity=\"sha384-9aIt2nRpC12Uk9gS9baDl411NQApFmC26EwAOH8WgZl5MYYxFfc+NcPb1dKGj7Sk\" crossorigin=\"anonymous\">");
		sb.append("<style>.card {box-shadow: 0 0 5px 0 #333; padding: 10px 5px; text-align: center; background: #eef;} .y-spacer-25{height: 25px;} .y-spacer-50{height: 50px;}</style>\n" + "\n</head>\n<body>\n");
		sb.append("<span id=\"top\">&nbsp;</span>");
		sb.append("<div style=\"margin: 50px 200px;\">");

		sb.append("<h1>Search Space Description for ").append(setting).append("</h1>");
		sb.append("<div class=\"y-spacer-50\"></div>");
		sb.append("<h2>Search Space Overview</h2>");

		sb.append(searchSpaceStats(components));

		sb.append("<div class=\"y-spacer-50\"></div>");
		sb.append("<h2>Component Details</h2>");
		sb.append("<div class=\"y-spacer-25\"></div>");
		sb.append("<div class=\"row\"><div class=\"col-md-4\"><div style=\"overflow-x: scroll;\">");

		List<Component> sortedComponentList = new ArrayList<>(components);
		sortedComponentList.sort(new Comparator<Component>() {
			@Override
			public int compare(final Component o1, final Component o2) {
				int compare = Integer.compare(o1.getProvidedInterfaces().stream().mapToInt(x -> {
					try {
						return IntStream.range(0, SORTING.size()).filter(y -> x.equals(SORTING.get(y))).findFirst().getAsInt();
					} catch (Exception e) {
						return -1;
					}
				}).max().getAsInt(), o2.getProvidedInterfaces().stream().mapToInt(x -> {
					try {
						return IntStream.range(0, SORTING.size()).filter(y -> x.equals(SORTING.get(y))).findFirst().getAsInt();
					} catch (Exception e) {
						return -1;
					}
				}).max().getAsInt());
				System.out.println(o1.getName() + " " + o2.getName() + " " + compare);
				return compare;
			}
		});

		sb.append("<h3>Outline</h3>");
		sb.append("<ul>");
		sb.append(sortedComponentList.stream().map(x -> "<li><a href=\"#" + x.getName() + "\">" + x.getName() + "</a></li>").collect(Collectors.joining("\n")));
		sb.append("</ul>");
		sb.append("</div></div><div class=\"col-md-8\"><div style=\"overflow-x: scroll;\">");

		sortedComponentList.stream().map(x -> componentToHTML(x, components)).forEach(sb::append);

		sb.append("</div></div></div></div>");
		sb.append("</body>\n</html>\n");

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(ResultsConfig.RESULT_DIR, outFile)))) {
			bw.write(sb.toString());
		}
	}

	public static String componentToHTML(final Component c, final Collection<Component> components) {
		StringBuilder sb = new StringBuilder();

		sb.append("<div style=\"float: right;\">").append("<a href=\"#top\"><svg width=\"1em\" height=\"1em\" viewBox=\"0 0 16 16\" class=\"bi bi-arrow-up-square-fill\" fill=\"currentColor\" xmlns=\"http://www.w3.org/2000/svg\">\n").append(
				"<path fill-rule=\"evenodd\" d=\"M2 0a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V2a2 2 0 0 0-2-2H2zm3.354 8.354a.5.5 0 1 1-.708-.708l3-3a.5.5 0 0 1 .708 0l3 3a.5.5 0 0 1-.708.708L8.5 6.207V11a.5.5 0 0 1-1 0V6.207L5.354 8.354z\"/>\n")
				.append("</svg> Top</a>\n").append("</div>");
		sb.append("<h3 id=\"").append(c.getName()).append("\">").append(c.getName()).append("</h3>\n");

		sb.append("<div class=\"row\">");
		sb.append("<div class=\"col-md-6\">");
		sb.append("<table class=\"table table-striped\">");
		sb.append("<tr><th>Parameter</th><th>Description</th></tr>");

		if (c.getParameters().isEmpty()) {
			sb.append("<tr><td colspan=\"2\">None.</td></tr>");
		} else {
			for (Parameter p : c.getParameters()) {
				sb.append(String.format("<tr><td>%s</td><td>%s</td></tr>", p.getName(), parameterToString(p)));
			}
		}
		sb.append("</table>");
		sb.append("</div>");

		sb.append("<div class=\"col-md-6\">");
		sb.append("<table class=\"table table-striped\">");
		sb.append("<tr><th>Dependency</th><th>Compatible Components</th></tr>");

		if (c.getRequiredInterfaceIds().isEmpty()) {
			sb.append("<tr><td colspan=\"2\">None.</td></tr>");
		} else {
			for (Interface i : c.getRequiredInterfaces()) {
				sb.append(String.format("<tr><td>%s</td><td>%s</td></tr>", i.getId(),
						ComponentUtil.getComponentsProvidingInterface(components, i.getName()).stream().map(x -> "<a href=\"#" + x.getName() + "\">" + x.getName() + "</a>").collect(Collectors.joining(", "))));
			}
		}

		sb.append("</table>");
		sb.append("</div>");
		sb.append("</div>");

		return sb.toString();
	}

	public static String parameterToString(final Parameter p) {
		StringBuilder sb = new StringBuilder();

		if (p.getDefaultDomain() instanceof NumericParameterDomain) {
			NumericParameterDomain dom = (NumericParameterDomain) p.getDefaultDomain();
			if (dom.isInteger()) {
				sb.append("int; ");
			} else {
				sb.append("float; ");
			}

			sb.append("default: ").append(p.getDefaultValue()).append(", ");
			sb.append("min: ").append(dom.getMin()).append(", max: ").append(dom.getMax());
		} else {
			CategoricalParameterDomain dom = (CategoricalParameterDomain) p.getDefaultDomain();
			sb.append("categorical; default: <tt>").append(p.getDefaultValue()).append("</tt>, values: ");
			sb.append(Arrays.stream(dom.getValues()).map(x -> "<tt>" + x + "</tt>").collect(Collectors.joining(", ")));
		}
		return sb.toString();
	}

	public static String searchSpaceStats(final Collection<Component> components) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div class=\"row\">").append("\n");
		sb.append(statsBox("Total Components", components.size())).append("\n");
		sb.append(statsBox("Total Unparametrized Solution Candidates", ComponentUtil.getAllAlgorithmSelectionInstances(requiredInterface, components).size())).append("\n");
		sb.append(statsBox("Total Parameters", components.stream().mapToInt(x -> x.getParameters().size()).sum())).append("\n");
		sb.append(statsBox("Numeric Parameters", components.stream().mapToInt(x -> (int) x.getParameters().stream().filter(p -> p.getDefaultDomain() instanceof NumericParameterDomain).count()).sum())).append("\n");
		sb.append(statsBox("Categorical Parameters", components.stream().mapToInt(x -> (int) x.getParameters().stream().filter(p -> p.getDefaultDomain() instanceof CategoricalParameterDomain).count()).sum())).append("\n");
		sb.append(statsBox("Max Dependency Compliances",
				ValueUtil.round(components.stream().mapToInt(x -> x.getRequiredInterfaceNames().stream().mapToInt(i -> ComponentUtil.getComponentsProvidingInterface(components, i).size()).reduce(1, (a, b) -> a * b)).max().getAsInt(), 2)))
				.append("\n");
		sb.append(statsBox("Avg. Dependency Compliances", ValueUtil.round(
				(double) components.stream().mapToInt(x -> x.getRequiredInterfaceNames().stream().mapToInt(i -> ComponentUtil.getComponentsProvidingInterface(components, i).size()).reduce(1, (a, b) -> a * b)).sum() / components.size(), 2)))
				.append("\n");

		sb.append(statsBox("Max. Simultaneous Params", ComponentUtil.getAllAlgorithmSelectionInstances(requiredInterface, components).stream().mapToInt(SearchSpaceDescription::countParams).max().getAsInt())).append("\n");
		sb.append(statsBox("Avg. Simultaneous Params", ValueUtil.round(ComponentUtil.getAllAlgorithmSelectionInstances(requiredInterface, components).stream().mapToInt(SearchSpaceDescription::countParams).average().getAsDouble(), 2)))
				.append("\n");

		sb.append("</div>\n");

		sb.append("<div class=\"y-spacer-25\"></div>").append("\n");

		sb.append("<h3>Contained Types of Components</h3>\n");
		sb.append("<div class=\"row\">").append("\n");

		sb.append(statsBox("#Meta Multi-Label Classifiers", components.stream().filter(x -> x.getProvidedInterfaces().contains("MetaMLClassifier")).count())).append("\n");
		sb.append(statsBox("#Basic Multi-Label Classifiers", components.stream().filter(x -> x.getProvidedInterfaces().contains("BasicMLClassifier")).count())).append("\n");
		sb.append(statsBox("#Meta Single-Label Classifiers", components.stream().filter(x -> x.getProvidedInterfaces().contains("MetaClassifier")).count())).append("\n");
		sb.append(statsBox("#Basic Single-Label Classifiers", components.stream().filter(x -> x.getProvidedInterfaces().contains("BaseClassifier")).count())).append("\n");
		sb.append(statsBox("#SVM Kernels", components.stream().filter(x -> x.getProvidedInterfaces().contains("K")).count())).append("\n");
		sb.append("</div>").append("\n");
		return sb.toString();
	}

	public static String statsBox(final String name, final Object value) {
		return String.format("<div class=\"col-lg-2\" style=\"margin-bottom: 25px;\"><div class=\"card\"><b>%s</b><br>%s</div></div>", name, value);
	}

	public static int countParams(final ComponentInstance ci) {
		return ci.getComponent().getParameters().size() + ci.getSatisfactionOfRequiredInterfaces().values().stream().mapToInt(SearchSpaceDescription::countParams).sum();
	}
}

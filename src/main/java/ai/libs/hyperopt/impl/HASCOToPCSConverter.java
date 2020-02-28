package ai.libs.hyperopt.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jfree.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.hasco.model.CategoricalParameterDomain;
import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentUtil;
import ai.libs.hasco.model.Dependency;
import ai.libs.hasco.model.IParameterDomain;
import ai.libs.hasco.model.NumericParameterDomain;
import ai.libs.hasco.model.Parameter;
import ai.libs.hyperopt.api.IOptimizationTask;
import ai.libs.jaicore.basic.sets.Pair;

/**
 * For converting HASCO format to PCS format
 *
 * @author kadirayk
 *
 */
public class HASCOToPCSConverter {
	private static final Logger logger = LoggerFactory.getLogger(HASCOToPCSConverter.class);

	private static Map<String, List<String>> componentConditionals;

	private static Map<String, List<Component>> componentsByProvidedInterfaces = new HashMap<String, List<Component>>();
	private static List<String> interfaceLines = new ArrayList<>();

	// mapping from artifical names to original parameter names
	private static Map<String, String> dependendParameterMap;

	private static Set<String> conditionalParametersToRemove = new HashSet<>();

	public static Map<String, String> getParameterMapping() {
		return dependendParameterMap;
	}

	/**
	 * PCS Files will be generated for the components in the input, generated files
	 * will be stored in the given outputDir. A Single PCS file that contains all
	 * the required components will be generated
	 *
	 * @param input
	 * @param outputDir path to folder that should contain the generated pcs file
	 * @throws Exception
	 */
	public static String generatePCSFile(final IOptimizationTask<?> input, final File executionPath) throws Exception {
		dependendParameterMap = new HashMap<>();
		componentConditionals = new HashMap<>();
		Collection<Component> components = input.getComponents();
		String requestedInterface = input.getRequestedInterface();
		if (ComponentUtil.hasCycles(components, requestedInterface)) {
			throw new Exception("Component has cycles. Not converting to PCS");
		}

		resolveInterfaces(components, requestedInterface);
		toPCS(components, requestedInterface, new File(executionPath, input.getRequestedInterface()+".pcs"));
		return input.getRequestedInterface()+".pcs";
	}

	private static void resolveInterfaces(final Collection<Component> components, final String requestedInterface) {
		List<Component> componentsProvidingRequestedInterface = components.stream().filter(c -> c.getProvidedInterfaces().contains(requestedInterface)).collect(Collectors.toList());
		componentsByProvidedInterfaces.put(requestedInterface, componentsProvidingRequestedInterface);
		if (componentsProvidingRequestedInterface.isEmpty()) {
			return;
		}
		StringBuilder pcsLine = new StringBuilder();
		pcsLine.append(requestedInterface).append(" {");
		for (Component c : componentsProvidingRequestedInterface) {
			pcsLine.append(c.getName()).append(",");
		}
		pcsLine.replace(pcsLine.length() - 1, pcsLine.length(), "");
		pcsLine.append("}");

		// we select first component as the default one
		pcsLine.append("[").append(componentsProvidingRequestedInterface.get(0).getName()).append("]");
		interfaceLines.add(pcsLine.toString());
		for (Component c : componentsProvidingRequestedInterface) {
			if (!c.getRequiredInterfaces().isEmpty()) {
				for (String s : c.getRequiredInterfaces().values()) {
					resolveInterfaces(components, s);
				}
			}
		}

	}

	private static String validatePath(String outputDir) throws Exception {
		if (StringUtils.isEmpty(outputDir)) {
			throw new Exception("outputDir is empty");
		}
		// we expect that path should end with "/"
		if (!outputDir.substring(outputDir.length() - 1, outputDir.length()).equals(File.separator)) {
			outputDir += File.separator;
		}
		return outputDir;
	}

	private static void toPCS(final Collection<Component> components, final String requestedInterface, final File outputFile) {
		List<Component> componentsToGenerate = new ArrayList<Component>();
		componentsByProvidedInterfaces.values().forEach(e -> componentsToGenerate.addAll(e));

		StringBuilder singleFileParameters = new StringBuilder();
		StringBuilder singleFileConditionals = new StringBuilder();
		for (Component cmp : componentsToGenerate) {
			StringBuilder pcsContent = new StringBuilder();
//			int lastDot = cmp.getName().lastIndexOf(".");
//			String name = cmp.getName().substring(lastDot + 1);
			Set<Parameter> params = cmp.getParameters();
			Map<String, String> requiredInterfaces = cmp.getRequiredInterfaces();
			for (Map.Entry<String, String> e : requiredInterfaces.entrySet()) {
				String interfaceId = e.getKey();
				String interfaceName = e.getValue();
				String requiredInterfaceStr = handleRequiredInterfaces(cmp, interfaceId, interfaceName, components);
				pcsContent.append(requestedInterface).append(System.lineSeparator());
				singleFileParameters.append(requiredInterfaceStr).append(System.lineSeparator());
			}

			Collection<Dependency> dependencies = cmp.getDependencies();
			Map<String, Integer> dependedParameterCounts = new HashMap<>();
			for (Dependency dep : dependencies) {
				// conclusion and premise has so far always only 1 element
				Pair<Parameter, IParameterDomain> post = dep.getConclusion().iterator().next();
				if (params.contains(post.getX())) {
					Parameter param = params.stream().filter(p -> p.equals(post.getX())).findFirst().get();
					Integer val = dependedParameterCounts.get(param.getName());
					val = val == null ? 1 : ++val;
					dependedParameterCounts.put(param.getName(), val);
					String artificialName = "opt" + val + "-" + post.getX().getName();
					dependendParameterMap.put(artificialName, post.getX().getName());
					Parameter dependendParam = new Parameter(artificialName, post.getY(), post.getX().getDefaultValue());
					params.add(dependendParam);
				}

				Pair<Parameter, IParameterDomain> pre = dep.getPremise().iterator().next().iterator().next();

				String cond = handleDependencyConditional(cmp.getName(), post, pre, dependedParameterCounts);

				List<String> conditionals = componentConditionals.get(cmp.getName());
				if (conditionals == null) {
					conditionals = new ArrayList<>();
					conditionals.add(cond);
				} else {
					conditionals.add(cond);
				}
				componentConditionals.put(cmp.getName(), conditionals);
			}

			for (Parameter param : params) {
				if (dependedParameterCounts.get(param.getName()) == null) {
					// ignore the dependend parameter's original form
					if (param.getDefaultDomain() instanceof CategoricalParameterDomain) {
						String categoricalStr = handleCategorical(cmp.getName(), param);
						pcsContent.append(categoricalStr).append(System.lineSeparator());
						singleFileParameters.append(categoricalStr).append(System.lineSeparator());
					} else if (param.getDefaultDomain() instanceof NumericParameterDomain) {
						String numericStr = handleNumeric(cmp.getName(), param);
						pcsContent.append(numericStr).append(System.lineSeparator());
						singleFileParameters.append(numericStr).append(System.lineSeparator());
					}
				}
			}

			String conditionalStr = handleConditionals(cmp.getName());
			pcsContent.append(conditionalStr);
			singleFileConditionals.append(conditionalStr);

		}
		String finalParams = removeDuplicateLines(singleFileParameters);
		// String finalConditionals = removeDuplicateLines(singleFileConditionals);
		String finalConditionals = removeUnusedParameters(singleFileConditionals);
		finalParams += finalConditionals;
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
			bw.write(finalParams);
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	private static String removeUnusedParameters(final StringBuilder content) {
		String[] lines = content.toString().split(System.lineSeparator());
		Set<String> lineSet = new LinkedHashSet<>();
		for (String line : lines) {
			boolean containsAny = false;
			for (String param : conditionalParametersToRemove) {
				if (line.startsWith(param)) {
					containsAny = true;
					break;
				}
			}
			if (!containsAny) {
				lineSet.add(line);
			}
		}

		StringBuilder cleanContent = new StringBuilder();
		lineSet.forEach(l -> cleanContent.append(l).append(System.lineSeparator()));

		return cleanContent.toString();
	}

	private static String handleDependencyConditional(final String name, final Pair<Parameter, IParameterDomain> post, final Pair<Parameter, IParameterDomain> pre, final Map<String, Integer> dependedParameterCounts) {
		int lastDot = name.lastIndexOf(".");
		String compName = name.substring(0, lastDot);
		StringBuilder cond = new StringBuilder(name);
		String artificialName = "opt" + dependedParameterCounts.get(post.getX().getName()) + "-" + post.getX().getName();
		dependendParameterMap.put(artificialName, post.getX().getName());
		cond.append(".").append(artificialName).append("|");
		cond.append(name).append(".");
		cond.append(pre.getX().getName());
		if (pre.getY() instanceof CategoricalParameterDomain) {
			CategoricalParameterDomain domain = (CategoricalParameterDomain) pre.getY();
			cond.append(" in {");
			for (String val : domain.getValues()) {
				cond.append(val).append(",");
			}
			cond.replace(cond.length() - 1, cond.length(), "");
			cond.append("}");
		} else if (pre.getY() instanceof NumericParameterDomain) {
			NumericParameterDomain domain = (NumericParameterDomain) pre.getY();
			cond.append(" > ").append(domain.getMin());
			cond.append(" && ").append(pre.getX().getName()).append(" < ").append(domain.getMax());
		}
		return cond.toString();
	}

	private static String removeDuplicateLines(final StringBuilder content) {
		String[] lines = content.toString().split(System.lineSeparator());
		Set<String> lineSet = new LinkedHashSet<>();
		for (String line : lines) {
			lineSet.add(line);
		}

		StringBuilder cleanContent = new StringBuilder();
		lineSet.forEach(l -> cleanContent.append(l).append(System.lineSeparator()));

		return cleanContent.toString();
	}

	private static String handleConditionals(final String componentName) {
		StringBuilder str = new StringBuilder();
		str.append("Conditionals:").append(System.lineSeparator());
		List<String> lines = componentConditionals.get(componentName);
		if (lines != null) {
			for (String line : lines) {
				str.append(line);
				str.append(System.lineSeparator());
			}
		}
		return str.toString();
	}

	public static String nameSpaceInterface(final Component requiringComponent, final String interfaceName) {

		return requiringComponent.getName() + "." + interfaceName;

//		List<Component> components = componentsByProvidedInterfaces.get(interfaceName);
//		if(components==null || components.isEmpty()) {
//			Log.error("no components for interface:" + interfaceName);
//			return interfaceName;
//		}
//		List<String> currentNameSpaces = new ArrayList<>();
//		for (Component c : components) {
//			String name = c.getName();
//			String[] arr = name.split("\\.");
//			String nameSpaceOfDepth = arr[0];
//			for (int i = 1; i < arr.length - depth - 1; i++) {
//				nameSpaceOfDepth += "." + arr[i];
//			}
//			currentNameSpaces.add(nameSpaceOfDepth);
//		}
//
//		Map<String, Integer> componentsInSameNameSpace = new HashMap<>();
//		for (String s : currentNameSpaces) {
//			Integer count = componentsInSameNameSpace.getOrDefault(s, 0);
//			componentsInSameNameSpace.put(s, ++count);
//		}
//		if (components.size() == componentsInSameNameSpace.values().iterator().next()) {
//			// all components share the same name space, we can use this namespace
//			return componentsInSameNameSpace.keySet().iterator().next() + "." + interfaceName;
//		} else {
//			return nameSpaceInterface(interfaceName, ++depth);
//		}
	}

	private static String handleRequiredInterfaces(final Component requiringComponent, String interfaceId, final String interfaceNameToHandle, final Collection<Component> components) {
		interfaceId = nameSpaceInterface(requiringComponent, interfaceNameToHandle);
		List<Component> componentsProvidingTheInterface = components.stream().filter(c -> c.getProvidedInterfaces().contains(interfaceNameToHandle)).collect(Collectors.toList());

		StringBuilder pcsContent = new StringBuilder().append(interfaceId).append(" {");
		for (Component cmp : componentsProvidingTheInterface) {
			pcsContent.append(cmp.getName()).append(",");
		}
		pcsContent.replace(pcsContent.length() - 1, pcsContent.length(), "");
		pcsContent.append("}");

		// use the first element as the default one
		pcsContent.append("[").append(componentsProvidingTheInterface.get(0).getName()).append("]").append(System.lineSeparator());

		for (Component cmp : componentsProvidingTheInterface) {
			Map<String, String> requiredInterfaces = cmp.getRequiredInterfaces();
			for (Map.Entry<String, String> e : requiredInterfaces.entrySet()) {
				String subInterfaceId = e.getKey();
				String subInterfaceName = e.getValue();
				pcsContent.append(handleRequiredInterfaces(cmp, subInterfaceId, subInterfaceName, components));
				StringBuilder conditional = new StringBuilder(cmp.getName()).append(".");
				conditional.append(subInterfaceName).append("|").append(interfaceId) // append(compName).append(".")
						.append(" in {").append(cmp.getName()).append("}");
				List<String> conditionals = componentConditionals.get(requiringComponent.getName());
				if (conditionals == null) {
					conditionals = new ArrayList<>();
					conditionals.add(conditional.toString());
				} else {
					conditionals.add(conditional.toString());
				}
				componentConditionals.put(requiringComponent.getName(), conditionals);

			}

			Set<Parameter> params = cmp.getParameters();
			for (Parameter param : params) {
				if (param.getDefaultDomain() instanceof CategoricalParameterDomain) {
					pcsContent.append(handleCategorical(cmp.getName(), param)).append(System.lineSeparator());
				} else if (param.getDefaultDomain() instanceof NumericParameterDomain) {
					pcsContent.append(handleNumeric(cmp.getName(), param)).append(System.lineSeparator());
				}
				StringBuilder conditional = new StringBuilder(cmp.getName()).append(".");
				conditional.append(param.getName()).append("|").append(interfaceId) // append(compName).append(".")
						.append(" in {").append(cmp.getName()).append("}");
				List<String> conditionals = componentConditionals.get(requiringComponent.getName());
				if (conditionals == null) {
					conditionals = new ArrayList<>();
					conditionals.add(conditional.toString());
				} else {
					conditionals.add(conditional.toString());
				}
				componentConditionals.put(requiringComponent.getName(), conditionals);
			}
		}

		return pcsContent.toString();
	}

	private static String handleNumeric(final String componentName, final Parameter param) {
		StringBuilder pcsLine = new StringBuilder(componentName).append(".");
		String defaultValue = null;
		NumericParameterDomain domain = (NumericParameterDomain) param.getDefaultDomain();
		String max = null;
		String min = null;
		boolean isLogSpace = false;
		if (domain.isInteger()) {
			Integer minVal = ((Double) domain.getMin()).intValue();
			Integer maxVal = ((Double) domain.getMax()).intValue();
			if (minVal.equals(maxVal)) {
//				maxVal += 1;
				// if a numeric param has min and max values as same then it is ignored in
				// hasco, so don't add it to pcs file
				conditionalParametersToRemove.add(componentName + "." + param.getName());
				return "";
			}
			max = String.valueOf(maxVal);
			min = String.valueOf(minVal);
			Double defVal = (Double) param.getDefaultValue();
			defaultValue = String.valueOf(defVal);
			if (defVal < minVal || defVal > maxVal) {
				Log.error("default value:" + defVal + " for " + param.getName() + " is not within range!! replacing it with minValue");
				defaultValue = min;
			}
		} else {
			Double minVal = domain.getMin();
			Double maxVal = domain.getMax();
			if (minVal.equals(maxVal)) {
				// if a numeric param has min and max values as same then it is ignored in
				// hasco, so don't add it to pcs file
				componentConditionals.remove(componentName);
				return "";
			}
			if (minVal != 0) {
				isLogSpace = true;
			}
			Double defVal = (Double) param.getDefaultValue();
			defaultValue = String.valueOf(defVal);
			if (defVal < minVal || defVal > maxVal) {
				Log.error("default value:" + defVal + " for " + param.getName() + " is not within range!! replacing it with minValue");
				defaultValue = min;
			}
			max = String.valueOf(maxVal);
			min = String.valueOf(minVal);
		}

		String name = param.getName();
		pcsLine.append(name).append(" ");

		pcsLine.append("[").append(min).append(",").append(max).append("] ");

		pcsLine.append("[").append(defaultValue).append("]");
		if (domain.isInteger()) {
			pcsLine.append("i");
		} else if (isLogSpace) {
			pcsLine.append("l");
		}

		return pcsLine.toString();
	}

	private static String handleCategorical(final String componentName, final Parameter param) {
		StringBuilder pcsLine = new StringBuilder(componentName).append(".");
		String defaultValue = param.getDefaultValue().toString();
		CategoricalParameterDomain domain = (CategoricalParameterDomain) param.getDefaultDomain();
		String[] values = domain.getValues();
		String name = param.getName();
		pcsLine.append(name).append(" ");

		boolean isDefaultValueContainedInValues = false;
		pcsLine.append("{");
		for (String val : values) {
			pcsLine.append(val).append(",");
			if (val.equals(defaultValue)) {
				isDefaultValueContainedInValues = true;
			}
		}
		if (!isDefaultValueContainedInValues) {
			Log.error("Default value must be contained in categorical values for component:" + componentName);
			// TODO: should we get 0th element?
			defaultValue = values[0];
		}
		pcsLine.replace(pcsLine.length() - 1, pcsLine.length(), "");
		pcsLine.append("}");

		pcsLine.append("[").append(defaultValue).append("]");
		return pcsLine.toString();
	}

}

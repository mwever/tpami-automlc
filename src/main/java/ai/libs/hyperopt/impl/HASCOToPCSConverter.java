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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import ai.libs.jaicore.basic.sets.Pair;

/**
 * For converting HASCO format to PCS format
 *
 * @author kadirayk
 *
 */
public class HASCOToPCSConverter {
	private static final Logger logger = LoggerFactory.getLogger(HASCOToPCSConverter.class);
	private static final String ROOT_COMP_NAME = "root";
	private static final String ROOT_COMP_REQI = "R";

	private Map<String, List<String>> componentConditionals;
	private Set<String> conditionalParametersToRemove = new HashSet<>();
	private Map<String, Collection<Component>> componentsByProvidedInterfaces = new HashMap<>();
//	private List<String> interfaceLines = new ArrayList<>();
	// mapping from artifical names to original parameter names
	private Map<String, String> dependendParameterMap;

	private final boolean rootRequired;
	private final Collection<Component> components;
	private final String requestedInterface;

	public HASCOToPCSConverter(final Collection<Component> components, final String requestedInterface) {
		this.components = new HashSet<>(getComponentsWithProvidedInterface(components, requestedInterface));
		this.rootRequired = this.components.size() > 1;
		List<Component> currentComponents = new LinkedList<>(this.components);
		Set<String> resolvedInterfaces = new HashSet<>();
		resolvedInterfaces.add(requestedInterface);

		while (!currentComponents.isEmpty()) {
			Component currentComponent = currentComponents.remove(0);
			this.components.add(currentComponent);
			currentComponent.getRequiredInterfaces().entrySet().stream().filter(x -> resolvedInterfaces.add(x.getValue())).forEach(x -> currentComponents.addAll(getComponentsWithProvidedInterface(components, x.getValue())));
		}

		if (this.rootRequired) {
			logger.debug("We need to create a new root component as there are multiple root components in the search space.");
			if (resolvedInterfaces.contains(ROOT_COMP_NAME)) {
				logger.warn("The components collection already contains components with provided interface >{}<", ROOT_COMP_NAME);
			}
			Component root = new Component(ROOT_COMP_NAME);
			root.addRequiredInterface(ROOT_COMP_REQI, requestedInterface);
			this.components.add(root);
			this.requestedInterface = ROOT_COMP_NAME;
		} else {
			this.requestedInterface = requestedInterface;
		}
	}

	private static final Collection<Component> getComponentsWithProvidedInterface(final Collection<Component> components, final String interfaceName) {
		return components.stream().filter(x -> x.getProvidedInterfaces().contains(interfaceName)).collect(Collectors.toList());
	}

	public Map<String, String> getParameterMapping() {
		return this.dependendParameterMap;
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
	public void generatePCSFile(final File outputFile) throws Exception {
		logger.debug("Initialize maps for caching");
		this.dependendParameterMap = new HashMap<>();
		this.componentConditionals = new HashMap<>();
		if (ComponentUtil.hasCycles(this.components, this.requestedInterface)) {
			throw new Exception("Component has cycles. Not converting to PCS");
		}
		this.toPCS(outputFile);
	}

	private String requestedInterfaceToPCSString(final Collection<Component> matchingComponents, final String requestedInterfaceParamName) {
		// The interface to be resolved.
		StringBuilder sb = new StringBuilder(requestedInterfaceParamName);
		// set of component names providing the requested interface as the domain of a categorical parameter
		sb.append(" {").append(matchingComponents.stream().map(x -> x.getName()).collect(Collectors.joining(","))).append("}");
		// default value for this categorical parameter: simply the first component from the collection
		return sb.append("[").append(matchingComponents.iterator().next().getName()).append("]").toString();
	}

	private void toPCS(final File outputFile) {
		StringBuilder singleFileParameters = new StringBuilder();
		StringBuilder singleFileConditionals = new StringBuilder();
		for (Component cmp : this.components) {
			StringBuilder pcsContent = new StringBuilder();
//			int lastDot = cmp.getName().lastIndexOf(".");
//			String name = cmp.getName().substring(lastDot + 1);
			Set<Parameter> params = cmp.getParameters();
			Map<String, String> requiredInterfaces = cmp.getRequiredInterfaces();
			for (Map.Entry<String, String> e : requiredInterfaces.entrySet()) {
				String interfaceId = e.getKey();
				String interfaceName = e.getValue();
				String requiredInterfaceStr = this.handleRequiredInterfaces(cmp, interfaceId, interfaceName, this.components);
				pcsContent.append(this.requestedInterface).append(System.lineSeparator());
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
					this.dependendParameterMap.put(artificialName, post.getX().getName());
					Parameter dependendParam = new Parameter(artificialName, post.getY(), post.getX().getDefaultValue());
					params.add(dependendParam);
				}

				Pair<Parameter, IParameterDomain> pre = dep.getPremise().iterator().next().iterator().next();

				String cond = this.handleDependencyConditional(cmp.getName(), post, pre, dependedParameterCounts);

				List<String> conditionals = this.componentConditionals.get(cmp.getName());
				if (conditionals == null) {
					conditionals = new ArrayList<>();
					conditionals.add(cond);
				} else {
					conditionals.add(cond);
				}
				this.componentConditionals.put(cmp.getName(), conditionals);
			}

			for (Parameter param : params) {
				if (dependedParameterCounts.get(param.getName()) == null) {
					// ignore the dependend parameter's original form
					if (param.getDefaultDomain() instanceof CategoricalParameterDomain) {
						String categoricalStr = this.handleCategorical(cmp.getName(), param);
						pcsContent.append(categoricalStr).append(System.lineSeparator());
						singleFileParameters.append(categoricalStr).append(System.lineSeparator());
					} else if (param.getDefaultDomain() instanceof NumericParameterDomain) {
						String numericStr = this.handleNumeric(cmp.getName(), param);
						pcsContent.append(numericStr).append(System.lineSeparator());
						singleFileParameters.append(numericStr).append(System.lineSeparator());
					}
				}
			}

			String conditionalStr = this.handleConditionals(cmp.getName());
			pcsContent.append(conditionalStr);
			singleFileConditionals.append(conditionalStr);

		}
		String finalParams = this.removeDuplicateLines(singleFileParameters);
		// String finalConditionals = removeDuplicateLines(singleFileConditionals);
		String finalConditionals = this.removeUnusedParameters(singleFileConditionals);
		finalParams += finalConditionals;
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
			bw.write(finalParams);
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	private String removeUnusedParameters(final StringBuilder content) {
		String[] lines = content.toString().split(System.lineSeparator());
		Set<String> lineSet = new LinkedHashSet<>();
		for (String line : lines) {
			boolean containsAny = false;
			for (String param : this.conditionalParametersToRemove) {
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

	private String handleDependencyConditional(final String name, final Pair<Parameter, IParameterDomain> post, final Pair<Parameter, IParameterDomain> pre, final Map<String, Integer> dependedParameterCounts) {
		int lastDot = name.lastIndexOf(".");
		String compName = name.substring(0, lastDot);
		StringBuilder cond = new StringBuilder(name);
		String artificialName = "opt" + dependedParameterCounts.get(post.getX().getName()) + "-" + post.getX().getName();
		this.dependendParameterMap.put(artificialName, post.getX().getName());
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

	private String removeDuplicateLines(final StringBuilder content) {
		String[] lines = content.toString().split(System.lineSeparator());
		Set<String> lineSet = new LinkedHashSet<>();
		for (String line : lines) {
			lineSet.add(line);
		}

		StringBuilder cleanContent = new StringBuilder();
		lineSet.forEach(l -> cleanContent.append(l).append(System.lineSeparator()));

		return cleanContent.toString();
	}

	private String handleConditionals(final String componentName) {
		StringBuilder str = new StringBuilder();
		str.append("Conditionals:").append(System.lineSeparator());
		List<String> lines = this.componentConditionals.get(componentName);
		if (lines != null) {
			for (String line : lines) {
				str.append(line);
				str.append(System.lineSeparator());
			}
		}
		return str.toString();
	}

	public String nameSpaceInterface(final Component requiringComponent, final String interfaceName) {
		return requiringComponent.getName() + "." + interfaceName;
	}

	public static String paramActivationCondition(final String paramNamespace, final String paramName, final String iface, final String domain) {
		return String.format("%s.%s|%s in {%s}", paramNamespace, paramName, iface, domain);
	}

	private String handleRequiredInterfaces(final Component requiringComponent, final String interfaceId, final String interfaceNameToHandle, final Collection<Component> components) {
		System.out.println("Calling handleRequiredInterfaces with interfaceId " + interfaceId + " and interfaceNameToHandle " + interfaceNameToHandle);
		String interfaceParamName = this.nameSpaceInterface(requiringComponent, interfaceId);
		System.out.println("Interface parameter name: " + interfaceParamName);

		List<Component> componentsProvidingTheInterface = components.stream().filter(c -> c.getProvidedInterfaces().contains(interfaceNameToHandle)).collect(Collectors.toList());

		StringBuilder pcsContent = new StringBuilder(this.requestedInterfaceToPCSString(getComponentsWithProvidedInterface(components, interfaceNameToHandle), interfaceParamName)).append("\n");
		for (Component cmp : componentsProvidingTheInterface) {
			Map<String, String> requiredInterfaces = cmp.getRequiredInterfaces();
			for (Map.Entry<String, String> e : requiredInterfaces.entrySet()) {
				pcsContent.append(this.handleRequiredInterfaces(cmp, e.getKey(), e.getValue(), components));
				this.componentConditionals.computeIfAbsent(requiringComponent.getName(), t -> new ArrayList<>()).add(paramActivationCondition(cmp.getName(), e.getValue(), interfaceId, cmp.getName()));
			}

			Set<Parameter> params = cmp.getParameters();
			for (Parameter param : params) {
				if (param.getDefaultDomain() instanceof CategoricalParameterDomain) {
					pcsContent.append(this.handleCategorical(cmp.getName(), param)).append(System.lineSeparator());
				} else if (param.getDefaultDomain() instanceof NumericParameterDomain) {
					pcsContent.append(this.handleNumeric(cmp.getName(), param)).append(System.lineSeparator());
				} else {
					throw new IllegalArgumentException("Unsupported parameter type.");
				}
				this.componentConditionals.computeIfAbsent(requiringComponent.getName(), t -> new ArrayList<>()).add(paramActivationCondition(cmp.getName(), param.getName(), interfaceParamName, cmp.getName()));
			}
		}

		return pcsContent.toString();
	}

	private String handleNumeric(final String componentName, final Parameter param) {
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
				this.conditionalParametersToRemove.add(componentName + "." + param.getName());
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
				this.componentConditionals.remove(componentName);
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

	private String handleCategorical(final String componentName, final Parameter param) {
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

package ai.libs.hyperopt.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import ai.libs.jaicore.basic.sets.SetUtil;

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

	private String getCategoricalPCSParam(final String paramName, final Collection<String> values, final String defaultValue) {
		return String.format("%s categorical {%s} [%s]", paramName, "'"+SetUtil.implode(values, "','")+"'", defaultValue);
	}

	private void toPCS(final File outputFile) {
		StringBuilder singleFileParameters = new StringBuilder();
		StringBuilder singleFileConditionals = new StringBuilder();

		Map<String, Set<String>> constraints = new HashMap<>();
		Set<String> constraintsToRemove = new HashSet<>();
		for (Component cmp : this.components) {
			for (Entry<String, String> reqI : cmp.getRequiredInterfaces().entrySet()) {
				Collection<Component> subCompList = getComponentsWithProvidedInterface(this.components, reqI.getValue());
				String reqIString = this.getCategoricalPCSParam(cmp.getName() + "." + reqI.getKey(), subCompList.stream().map(x -> x.getName()).collect(Collectors.toList()), subCompList.iterator().next().getName());
				// add parameter definition for required interface
				singleFileParameters.append(reqIString).append(System.lineSeparator());

				// Add constraints for activation of sub-components
				for (Component sc : subCompList) {
					String constraintForSC = String.format("%s in {%s}", cmp.getName() + "." + reqI.getKey(), sc.getName());

					// add constraints for required interfaces
					for (Entry<String, String> scri : sc.getRequiredInterfaces().entrySet()) {
						constraints.computeIfAbsent(sc.getName() + "." + scri.getKey(), t -> new HashSet<>()).add(constraintForSC);
					}

					// add constraints for parameters
					for (Parameter param : sc.getParameters()) {
						constraints.computeIfAbsent(sc.getName() + "." + param.getName(), t -> new HashSet<>()).add(constraintForSC);
					}
				}
			}

			for (Parameter param : cmp.getParameters()) {
				if (param.getDefaultDomain() instanceof CategoricalParameterDomain) {
					String categoricalStr = this.handleCategorical(cmp.getName(), param);
					if (categoricalStr != null && !categoricalStr.isEmpty()) {
						singleFileParameters.append(categoricalStr).append(System.lineSeparator());
					} else {
						constraintsToRemove.add(cmp.getName()+"."+param.getName());
					}
				} else if (param.getDefaultDomain() instanceof NumericParameterDomain) {
					String numericStr = this.handleNumeric(cmp.getName(), param);
					if (numericStr != null && !numericStr.isEmpty()) {
						singleFileParameters.append(numericStr).append(System.lineSeparator());
					} else {
						constraintsToRemove.add(cmp.getName()+"."+param.getName());
					}
				}
			}

//			Set<Parameter> params = cmp.getParameters();
//			Map<String, Integer> dependedParameterCounts = new HashMap<>();
//			for (Dependency dep : cmp.getDependencies()) {
//				// conclusion and premise has so far always only 1 element
//				Pair<Parameter, IParameterDomain> post = dep.getConclusion().iterator().next();
//				if (params.contains(post.getX())) {
//					Parameter param = params.stream().filter(p -> p.equals(post.getX())).findFirst().get();
//					Integer val = dependedParameterCounts.get(param.getName());
//					val = val == null ? 1 : ++val;
//					dependedParameterCounts.put(param.getName(), val);
//					String artificialName = "opt" + val + "-" + post.getX().getName();
//					this.dependendParameterMap.put(artificialName, post.getX().getName());
//					Parameter dependendParam = new Parameter(artificialName, post.getY(), post.getX().getDefaultValue());
//					params.add(dependendParam);
//				}
//
//				Pair<Parameter, IParameterDomain> pre = dep.getPremise().iterator().next().iterator().next();
//
//				String cond = this.handleDependencyConditional(cmp.getName(), post, pre, dependedParameterCounts);
//
//				List<String> conditionals = this.componentConditionals.get(cmp.getName());
//				if (conditionals == null) {
//					conditionals = new ArrayList<>();
//					conditionals.add(cond);
//				} else {
//					conditionals.add(cond);
//				}
//				this.componentConditionals.put(cmp.getName(), conditionals);
//			}
		}

		constraintsToRemove.stream().forEach(constraints::remove);
		for (Entry<String, Set<String>> condition : constraints.entrySet()) {
			singleFileConditionals.append(String.format("%s | %s", condition.getKey(), SetUtil.implode(condition.getValue(), "||"))).append(System.lineSeparator());
		}

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
			bw.write(singleFileParameters.toString());
			bw.write("Conditionals:" + System.lineSeparator());
			bw.write(singleFileConditionals.toString());
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
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

	public String nameSpaceInterface(final Component requiringComponent, final String interfaceName) {
		return requiringComponent.getName() + "." + interfaceName;
	}

	public static String paramActivationCondition(final String paramNamespace, final String paramName, final String iface, final String domain) {
		return String.format("%s.%s|%s in {%s}", paramNamespace, paramName, iface, domain);
	}

	private String handleNumeric(final String componentName, final Parameter param) {
		String defaultValue = null;
		NumericParameterDomain domain = (NumericParameterDomain) param.getDefaultDomain();
		String max = null;
		String min = null;
		boolean isLogSpace = false;
		if (domain.isInteger()) {
			Integer minVal = ((Double) domain.getMin()).intValue();
			Integer maxVal = ((Double) domain.getMax()).intValue();
			if (minVal.equals(maxVal)) {
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
				logger.error("default value:" + defVal + " for " + param.getName() + " is not within range!! replacing it with minValue");
				defaultValue = min;
			}
		} else {
			Double minVal = domain.getMin();
			Double maxVal = domain.getMax();
			if (minVal.equals(maxVal)) {
				return "";
			}
			if (minVal != 0) {
				isLogSpace = true;
			}
			Double defVal = (Double) param.getDefaultValue();
			defaultValue = String.valueOf(defVal);
			if (defVal < minVal || defVal > maxVal) {
				logger.error("default value:" + defVal + " for " + param.getName() + " is not within range!! replacing it with minValue");
				defaultValue = min;
			}
			max = String.valueOf(maxVal);
			min = String.valueOf(minVal);
		}

		String paramType = domain.isInteger() ? "integer" : "real";
		String logSpace = isLogSpace ? " log" : "";
		return String.format("%s.%s %s [%s,%s] [%s]%s", componentName, param.getName(), paramType, min, max, defaultValue, logSpace);
	}

	private String handleCategorical(final String componentName, final Parameter param) {
		String defaultValue = param.getDefaultValue().toString();
		String[] values = ((CategoricalParameterDomain) param.getDefaultDomain()).getValues();
		boolean isDefaultValueContainedInValues = false;
		for (String val : values) {
			if (val.equals(defaultValue)) {
				isDefaultValueContainedInValues = true;
			}
		}
		if (!isDefaultValueContainedInValues) {
			Log.error("Default value must be contained in categorical values for component:" + componentName);
			defaultValue = values[0];
		}
		return String.format("%s.%s categorical {%s} [%s]", componentName, param.getName(),SetUtil.implode(Arrays.stream(values).map(x -> x.replaceAll(" ", "_")).collect(Collectors.toList()), ","), defaultValue);
	}

}

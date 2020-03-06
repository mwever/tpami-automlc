package ai.libs.hyperopt.impl.optimizer.cfg.ggp;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import ai.libs.hasco.model.CategoricalParameterDomain;
import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.model.NumericParameterDomain;
import ai.libs.hasco.model.Parameter;
import ai.libs.hyperopt.impl.optimizer.pcs.HASCOToPCSConverter;

public class CFGConverter {

	private final Collection<Component> components;
	private final String requestedInterface;

	public CFGConverter(final Collection<Component> components, final String requestedInterface) {
		this.components = components;
		this.requestedInterface = requestedInterface;
	}

	public String toGrammar() {
		StringBuilder sb = new StringBuilder();
		Collection<Component> matchingComponents = HASCOToPCSConverter.getComponentsWithProvidedInterface(this.components, this.requestedInterface);
		Map<String, String> productions = new HashMap<>();
		sb.append("<START> ::= ").append(this.componentsToOrListOfNonTerminals(matchingComponents)).append("\n");
		for (Component component : matchingComponents) {
			this.addComponentProductions(this.components, component, productions);
		}
		productions.values().stream().forEach(sb::append);
		return sb.toString();
	}

	private String componentsToOrListOfNonTerminals(final Collection<Component> components) {
		return components.stream().map(x -> "<" + x.getName() + ">").collect(Collectors.joining(" | "));
	}

	private void addComponentProductions(final Collection<Component> components, final Component component, final Map<String, String> productions) {
		StringBuilder compProduction = new StringBuilder();
		String compNT = "<" + component.getName() + ">";
		if (productions.containsKey(compNT)) {
			return;
		}
		compProduction.append(compNT).append(" ::= ").append(component.getName());

		for (Parameter param : component.getParameters()) {
			String nsParam = component.getName() + "." + param.getName();
			String paramNT = "<" + nsParam + ">";
			compProduction.append(" ").append(nsParam).append(" ").append(paramNT);

			if (param.getDefaultDomain() instanceof NumericParameterDomain) {
				NumericParameterDomain dom = (NumericParameterDomain) param.getDefaultDomain();
				if (dom.isInteger()) {
					productions.put(paramNT, paramNT + " ::= RANDINT_TYPE0(" + (int) dom.getMin() + "," + (int) dom.getMax() + ")\n");
				} else {
					productions.put(paramNT, paramNT + " ::= RANDFLOAT(" + dom.getMin() + "," + dom.getMax() + ")\n");
				}
			} else if (param.getDefaultDomain() instanceof CategoricalParameterDomain) {
				CategoricalParameterDomain dom = (CategoricalParameterDomain) param.getDefaultDomain();
				productions.put(paramNT, paramNT + " ::= " + Arrays.stream(dom.getValues()).collect(Collectors.joining(" | ")) + "\n");
			}
		}

		for (Entry<String, String> requiredInterface : component.getRequiredInterfaces().entrySet()) {
			String nsI = component.getName() + "." + requiredInterface.getKey();
			String reqINT = "<" + requiredInterface.getValue() + ">";
			compProduction.append(" ").append(nsI).append(" ").append(reqINT);
			if (!productions.containsKey(reqINT)) {
				Collection<Component> componentsMatching = HASCOToPCSConverter.getComponentsWithProvidedInterface(components, requiredInterface.getValue());
				productions.put(reqINT, reqINT + " ::= " + this.componentsToOrListOfNonTerminals(componentsMatching) + "\n");
				componentsMatching.stream().forEach(c -> this.addComponentProductions(components, c, productions));
			}
		}
		compProduction.append("\n");
		productions.put(compNT, compProduction.toString());
	}

	public ComponentInstance grammarStringToComponentInstance(final String grammarString) {
		String[] tokens = grammarString.split(" ");
		Map<String, String> paramValues = new HashMap<>();
		for (int i = 1; i < tokens.length; i = i + 2) {
			paramValues.put(tokens[i], tokens[i + 1]);
		}
		return this.buildComponentInstanceFromMap(tokens[0], paramValues);
	}

	private ComponentInstance buildComponentInstanceFromMap(final String componentName, final Map<String, String> values) {
		Map<String, String> parameters = new HashMap<>();
		Map<String, ComponentInstance> reqIs = new HashMap<>();
		ComponentInstance root = new ComponentInstance(HASCOToPCSConverter.getComponentWithName(this.components, componentName), parameters, reqIs);
		// reconstruct required interfaces
		for (Entry<String, String> reqI : root.getComponent().getRequiredInterfaces().entrySet()) {
			root.getSatisfactionOfRequiredInterfaces().put(reqI.getKey(), this.buildComponentInstanceFromMap(values.get(componentName + "." + reqI.getKey()), values));
		}
		// reconstruct param values
		for (Parameter param : root.getComponent().getParameters()) {
			root.getParameterValues().put(param.getName(), values.get(componentName + "." + param.getName()));
		}
		return root;
	}

}

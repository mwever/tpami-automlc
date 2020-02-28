package ai.libs.hyperopt.util;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;

public class ComponentInstanceAdapter {

	private static final String L_COMPONENT = "component";
	private static final String L_NAME = "name";
	private static final String L_PARAM_VALUES = "parameterValues";
	private static final String L_SAT_REQ_IFACE = "satisfactionOfRequiredInterfaces";

	private Collection<Component> components;

	public ComponentInstanceAdapter(final Collection<Component> components) {
		this.components = components;
	}

	public ComponentInstanceAdapter() {
		this(new LinkedList<>());
	}

	public String componentInstanceToString(final ComponentInstance ci) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(this.componentInstanceToMap(ci));
	}

	public Map<String, Object> componentInstanceToMap(final ComponentInstance ci) {
		Map<String, Object> ciMap = new HashMap<>();
		ciMap.put(L_COMPONENT, this.componentToString(ci.getComponent()));
		ciMap.put(L_PARAM_VALUES, ci.getParameterValues());

		Map<String, Object> satisfactionOfRequiredInterfaces = new HashMap<>();
		ci.getSatisfactionOfRequiredInterfaces().entrySet().stream().forEach(x -> satisfactionOfRequiredInterfaces.put(x.getKey(), this.componentInstanceToMap(x.getValue())));
		ciMap.put(L_SAT_REQ_IFACE, satisfactionOfRequiredInterfaces);

		return ciMap;
	}

	private Map<String, Object> componentToString(final Component comp) {
		Map<String, Object> componentMap = new HashMap<>();
		componentMap.put(L_NAME, comp.getName());
		return componentMap;
	}

	public ComponentInstance stringToComponentInstance(final String ciString) throws IOException {
		JsonNode root = new ObjectMapper().readTree(ciString);
		return this.readComponentInstanceFromJson(root);
	}

	private ComponentInstance readComponentInstanceFromJson(final JsonNode node) throws JsonProcessingException {
		String componentName = node.get(L_COMPONENT).get(L_NAME).asText();
		Component component = this.components.stream().filter(x -> x.getName().equals(componentName)).findAny().get();

		Map<String, String> parameterValues = new HashMap<>();
		Iterator<String> parameterValueIt = node.get(L_PARAM_VALUES).fieldNames();
		while (parameterValueIt.hasNext()) {
			String fieldName = parameterValueIt.next();
			parameterValues.put(fieldName, node.get(L_PARAM_VALUES).get(fieldName).asText());
		}

		Map<String, ComponentInstance> satisfactionOfRequiredInterfaces = new HashMap<>();
		if (node.get(L_SAT_REQ_IFACE) != null) {
			Iterator<String> satReqIfaceIt = node.get(L_SAT_REQ_IFACE).fieldNames();
			while (satReqIfaceIt.hasNext()) {
				String ifaceName = satReqIfaceIt.next();
				satisfactionOfRequiredInterfaces.put(ifaceName, this.readComponentInstanceFromJson(node.get(L_SAT_REQ_IFACE).get(ifaceName)));
			}
		}

		return new ComponentInstance(component, parameterValues, satisfactionOfRequiredInterfaces);
	}

}

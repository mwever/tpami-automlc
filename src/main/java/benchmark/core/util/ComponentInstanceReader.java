package benchmark.core.util;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import ai.libs.jaicore.components.model.Component;
import ai.libs.jaicore.components.model.ComponentInstance;

public class ComponentInstanceReader extends StdDeserializer<ComponentInstance> {

	/**
	 *
	 */
	private static final long serialVersionUID = 4216559441244072999L;

	private transient Collection<Component> possibleComponents; // the idea is not to serialize the deserializer, so this can be transient

	public ComponentInstanceReader(final Collection<Component> possibleComponents) {
		super(ComponentInstance.class);
		this.possibleComponents = possibleComponents;
	}

	public ComponentInstance readFromJson(final String json) throws IOException {
		return this.readAsTree(new ObjectMapper().readTree(json));
	}

	@SuppressWarnings("unchecked")
	public ComponentInstance readAsTree(final TreeNode p) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		// read the parameter values
		Map<String, String> parameterValues = mapper.treeToValue(p.get("params"), HashMap.class);
		// read the component

		String componentName = p.get("component").toString().replaceAll("\"", "");

		Component component = this.possibleComponents.stream().filter(c -> c.getName().equals(componentName)).findFirst().orElseThrow(NoSuchElementException::new);

		Map<String, ComponentInstance> satisfactionOfRequiredInterfaces = new HashMap<>();
		// recursively resolve the requiredInterfaces
		TreeNode n = p.get("requiredInterfaces");
		Iterator<String> fields = n.fieldNames();
		while (fields.hasNext()) {
			String key = fields.next();
			satisfactionOfRequiredInterfaces.put(key, this.readAsTree(n.get(key)));
		}
		return new ComponentInstance(component, parameterValues, satisfactionOfRequiredInterfaces);
	}

	@Override
	public ComponentInstance deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
		return this.readAsTree(p.readValueAsTree());
	}

}
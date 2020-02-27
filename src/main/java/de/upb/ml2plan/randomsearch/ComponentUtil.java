package de.upb.ml2plan.randomsearch;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.hasco.model.BooleanParameterDomain;
import ai.libs.hasco.model.CategoricalParameterDomain;
import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.model.ComponentInstanceUtil;
import ai.libs.hasco.model.NumericParameterDomain;
import ai.libs.hasco.model.Parameter;
import ai.libs.jaicore.basic.kvstore.KVStore;
import ai.libs.jaicore.basic.sets.SetUtil;

/**
 * The ComponentUtil class can be used to deal with Components in a convenient way.
 * For instance, for a given component (type) it can be used to return a parameterized ComponentInstance.
 *
 * @author wever
 */
public class ComponentUtil {

	private static final Logger logger = LoggerFactory.getLogger(ComponentUtil.class);

	private ComponentUtil() {
		/* Intentionally left blank to prevent instantiation of this class. */
	}

	/**
	 * This procedure returns a ComponentInstance of the given Component with default parameterization.
	 * Note that required interfaces are not resolved.
	 *
	 * @param component The component for which a random parameterization is to be returned.
	 * @return An instantiation of the component with default parameterization.
	 */
	public static ComponentInstance defaultParameterizationOfComponent(final Component component) {
		Map<String, String> parameterValues = new HashMap<>();
		for (Parameter p : component.getParameters()) {
			parameterValues.put(p.getName(), p.getDefaultValue() + "");
		}
		return componentInstanceWithNoRequiredInterfaces(component, parameterValues);
	}

	/**
	 * This procedure returns a valid random parameterization of a given component. Random decisions are made with the help of the given Random object.
	 * Note that required interfaces are not resolved.
	 *
	 * @param component The component for which a random parameterization is to be returned.
	 * @param rand The Random instance for making the random decisions.
	 * @return An instantiation of the component with valid random parameterization.
	 */
	public static ComponentInstance randomParameterizationOfComponent(final Component component, final Random rand) {
		ComponentInstance ci;
		do {
			Map<String, String> parameterValues = new HashMap<>();
			for (Parameter p : component.getParameters()) {
				if (p.getDefaultDomain() instanceof CategoricalParameterDomain) {
					String[] values = ((CategoricalParameterDomain) p.getDefaultDomain()).getValues();
					parameterValues.put(p.getName(), values[rand.nextInt(values.length)]);
				} else {
					NumericParameterDomain numDomain = (NumericParameterDomain) p.getDefaultDomain();
					if (numDomain.isInteger()) {
						if ((int) (numDomain.getMax() - numDomain.getMin()) > 0) {
							parameterValues.put(p.getName(), ((int) (rand.nextInt((int) (numDomain.getMax() - numDomain.getMin())) + numDomain.getMin())) + "");
						} else {
							if (p.getDefaultValue() instanceof Double) {
								parameterValues.put(p.getName(), ((int) ((Double) p.getDefaultValue()).doubleValue()) + "");
							} else {
								parameterValues.put(p.getName(), (int) p.getDefaultValue() + "");
							}
						}
					} else {
						parameterValues.put(p.getName(), (rand.nextDouble() * (numDomain.getMax() - numDomain.getMin()) + numDomain.getMin()) + "");
					}
				}
			}
			ci = componentInstanceWithNoRequiredInterfaces(component, parameterValues);
		} while (!ComponentInstanceUtil.isValidComponentInstantiation(ci));
		return ci;
	}

	private static ComponentInstance componentInstanceWithNoRequiredInterfaces(final Component component, final Map<String, String> parameterValues) {
		return new ComponentInstance(component, parameterValues, new HashMap<>());
	}

	/**
	 * Searches and returns all components within a collection of components that provide a specific interface.
	 *
	 * @param components The collection of components to search in.
	 * @param providedInterface The interface of interest.
	 * @return A sub-collection of components all of which provide the requested providedInterface.
	 */
	public static Collection<Component> getComponentsProvidingInterface(final Collection<Component> components, final String providedInterface) {
		return components.stream().filter(x -> x.getProvidedInterfaces().contains(providedInterface)).collect(Collectors.toList());
	}

	/**
	 * Enumerates all possible component instances for a specific root component and a collection of components for resolving required interfaces.
	 * Hyperparameters are set to the default value.
	 *
	 * @param rootComponent The component to be considered the root.
	 * @param components The collection fo components that is used for resolving required interfaces recursively.
	 * @return A collection of component instances of the given root component with all possible algorithm choices.
	 */
	public static Collection<ComponentInstance> getAllAlgorithmSelectionInstances(final Component rootComponent, final Collection<Component> components) {
		Collection<ComponentInstance> instanceList = new LinkedList<>();
		instanceList.add(ComponentUtil.defaultParameterizationOfComponent(rootComponent));

		for (Entry<String, String> requiredInterface : rootComponent.getRequiredInterfaces().entrySet()) {
			List<ComponentInstance> tempList = new LinkedList<>();

			Collection<Component> possiblePlugins = ComponentUtil.getComponentsProvidingInterface(components, requiredInterface.getValue());
			for (ComponentInstance ci : instanceList) {
				for (Component possiblePlugin : possiblePlugins) {
					for (ComponentInstance reqICI : getAllAlgorithmSelectionInstances(possiblePlugin, components)) {
						ComponentInstance copyOfCI = new ComponentInstance(ci.getComponent(), new HashMap<>(ci.getParameterValues()), new HashMap<>(ci.getSatisfactionOfRequiredInterfaces()));
						copyOfCI.getSatisfactionOfRequiredInterfaces().put(requiredInterface.getKey(), reqICI);
						tempList.add(copyOfCI);
					}
				}
			}

			instanceList.clear();
			instanceList.addAll(tempList);
		}

		return instanceList;
	}

	/**
	 * Enumerates all possible component instances for a specific root component and a collection of components for resolving required interfaces.
	 * Hyperparameters are set to the default value.
	 *
	 * @param requiredInterface The interface required to be provided by the root components.
	 * @param components The collection fo components that is used for resolving required interfaces recursively.
	 * @return A collection of component instances of the given root component with all possible algorithm choices.
	 */
	public static Collection<ComponentInstance> getAllAlgorithmSelectionInstances(final String requiredInterface, final Collection<Component> components) {
		Collection<ComponentInstance> instanceList = new LinkedList<>();
		components.stream().filter(x -> x.getProvidedInterfaces().contains(requiredInterface)).map(x -> getAllAlgorithmSelectionInstances(x, components)).forEach(instanceList::addAll);
		return instanceList;
	}

	public static int getNumberOfUnparametrizedCompositions(final Collection<Component> components, final String requiredInterface) {
		if (hasCycles(components, requiredInterface)) {
			return -1;
		}

		Collection<Component> candidates = components.stream().filter(c -> c.getProvidedInterfaces().contains(requiredInterface)).collect(Collectors.toList());
		int numCandidates = 0;
		for (Component candidate : candidates) {
			int waysToResolveComponent = 0;
			if (candidate.getRequiredInterfaces().isEmpty()) {
				waysToResolveComponent = 1;
			} else {
				for (String req : candidate.getRequiredInterfaces().keySet()) {
					int subSolutionsForThisInterface = getNumberOfUnparametrizedCompositions(components, candidate.getRequiredInterfaces().get(req));
					if (waysToResolveComponent > 0) {
						waysToResolveComponent *= subSolutionsForThisInterface;
					} else {
						waysToResolveComponent = subSolutionsForThisInterface;
					}
				}
			}
			numCandidates += waysToResolveComponent;
		}
		return numCandidates;
	}

	public ComponentInstance getRandomParametrization(final ComponentInstance componentInstance, final Random rand) {
		ComponentInstance randomParametrization = randomParameterizationOfComponent(componentInstance.getComponent(), rand);
		componentInstance.getSatisfactionOfRequiredInterfaces().entrySet().forEach(x -> randomParametrization.getSatisfactionOfRequiredInterfaces().put(x.getKey(), this.getRandomParametrization(x.getValue(), rand)));
		return randomParametrization;
	}

	public static boolean hasCycles(final Collection<Component> components, final String requiredInterface) {
		return hasCycles(components, requiredInterface, new LinkedList<>());
	}

	private static boolean hasCycles(final Collection<Component> components, final String requiredInterface, final List<String> componentList) {
		Collection<Component> candidates = components.stream().filter(c -> c.getProvidedInterfaces().contains(requiredInterface)).collect(Collectors.toList());

		for (Component c : candidates) {
			if (componentList.contains(c.getName())) {
				return true;
			}

			List<String> componentListCopy = new LinkedList<>(componentList);
			componentListCopy.add(c.getName());

			for (String subRequiredInterface : c.getRequiredInterfaces().values()) {
				if (hasCycles(components, subRequiredInterface, componentListCopy)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isDefaultConfiguration(final ComponentInstance instance) {
		for (Parameter p : instance.getParametersThatHaveBeenSetExplicitly()) {
			if (p.isNumeric()) {
				List<String> intervalAsList = SetUtil.unserializeList(instance.getParameterValue(p));
				double defaultValue = Double.parseDouble(p.getDefaultValue().toString());
				boolean isCompatibleWithDefaultValue = defaultValue >= Double.parseDouble(intervalAsList.get(0)) && defaultValue <= Double.parseDouble(intervalAsList.get(1));
				if (!isCompatibleWithDefaultValue) {
					logger.info("{} has value {}, which does not subsume the default value {}", p.getName(), instance.getParameterValue(p), defaultValue);
					return false;
				} else {
					logger.info("{} has value {}, which IS COMPATIBLE with the default value {}", p.getName(), instance.getParameterValue(p), defaultValue);
				}
			} else {
				if (!instance.getParameterValue(p).equals(p.getDefaultValue().toString())) {
					logger.info("{} has value {}, which is not the default {}", p.getName(), instance.getParameterValue(p), p.getDefaultValue());
					return false;
				}
			}
		}
		for (ComponentInstance child : instance.getSatisfactionOfRequiredInterfaces().values()) {
			if (!isDefaultConfiguration(child)) {
				return false;
			}
		}
		return true;
	}

	public static KVStore getStatsForComponents(final Collection<Component> components) {
		KVStore stats = new KVStore();
		int numComponents = 0;
		int numNumericParams = 0;
		int numIntParams = 0;
		int numDoubleParams = 0;
		int numCatParams = 0;
		int numBoolParams = 0;
		int otherParams = 0;

		for (Component c : components) {
			numComponents++;

			for (Parameter p : c.getParameters()) {
				if (p.getDefaultDomain() instanceof CategoricalParameterDomain) {
					numCatParams++;
					if (p.getDefaultDomain() instanceof BooleanParameterDomain) {
						numBoolParams++;
					}
				} else if (p.getDefaultDomain() instanceof NumericParameterDomain) {
					numNumericParams++;
					if (((NumericParameterDomain) p.getDefaultDomain()).isInteger()) {
						numIntParams++;
					} else {
						numDoubleParams++;
					}
				} else {
					otherParams++;
				}
			}
		}

		stats.put("nComponents", numComponents);
		stats.put("nNumericParameters", numNumericParams);
		stats.put("nIntegerParameters", numIntParams);
		stats.put("nContinuousParameters", numDoubleParams);
		stats.put("nCategoricalParameters", numCatParams);
		stats.put("nBooleanParameters", numBoolParams);
		stats.put("nOtherParameters", otherParams);

		return stats;
	}

	/**
	 * Returns a collection of components that is relevant to resolve all recursive dependency when the request concerns a component with the provided required interface.
	 * @param components A collection of component to search for relevant components.
	 * @param requiredInterface The requested required interface.
	 * @return The collection of affected components when requesting the given required interface.
	 */
	public static Collection<Component> getAffectedComponents(final Collection<Component> components, final String requiredInterface) {
		Collection<Component> affectedComponents = new HashSet<>(ComponentUtil.getComponentsProvidingInterface(components, requiredInterface));
		if (affectedComponents.isEmpty()) {
			throw new IllegalArgumentException("Could not resolve the requiredInterface " + requiredInterface);
		}
		Set<Component> recursiveResolvedComps = new HashSet<>();
		affectedComponents.forEach(x -> x.getRequiredInterfaces().values().stream().map(interfaceName -> getAffectedComponents(components, interfaceName)).forEach(recursiveResolvedComps::addAll));
		affectedComponents.addAll(recursiveResolvedComps);
		return affectedComponents;
	}
}

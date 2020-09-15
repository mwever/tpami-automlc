package benchmark.results.searchspace;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ai.libs.jaicore.components.model.Component;
import ai.libs.jaicore.components.serialization.ComponentLoader;
import benchmark.results.ResultsConfig;
import benchmark.results.ResultsUtil;

public class SearchSpaceAlgorithmList {

	private static final List<String> MAP_KEYS = Arrays.asList("meka_meta", "meka_base", "weka_meta", "weka_base", "weka_kernel");

	public static void main(final String[] args) throws IOException {
		ComponentLoader cl = new ComponentLoader(ResultsConfig.SEARCH_SPACE_ROOT_FILE);
		Collection<Component> component = cl.getComponents();

		Map<String, List<String>> algoSorting = new HashMap<>();
		for (Component c : component) {
			final String mapKey;
			if (ResultsUtil.isMekaMeta(c.getName())) {
				mapKey = MAP_KEYS.get(0);
			} else if (ResultsUtil.isMekaBase(c.getName())) {
				mapKey = MAP_KEYS.get(1);
			} else if (ResultsUtil.isWekaMeta(c.getName())) {
				mapKey = MAP_KEYS.get(2);
			} else if (ResultsUtil.isWekaBase(c.getName())) {
				mapKey = MAP_KEYS.get(3);
			} else if (ResultsUtil.isWekaKernel(c.getName())) {
				mapKey = MAP_KEYS.get(4);
			} else {
				throw new IllegalStateException("Unexpected component type for component " + c.getName());
			}
			algoSorting.computeIfAbsent(mapKey, t -> new ArrayList<>()).add(c.getName().substring(c.getName().lastIndexOf('.') + 1));
		}

		StringBuilder sb = new StringBuilder();
		for (String mapKey : MAP_KEYS) {
			List<String> algos = algoSorting.get(mapKey);
			sb.append(mapKey).append("\n");
			sb.append(algos.stream().map(x -> x + (!ResultsUtil.shortenAlgoName(x).equals(x) ? " (" + ResultsUtil.shortenAlgoName(x) + ")" : "")).collect(Collectors.joining(", "))).append("\n");
		}

		ResultsConfig.RESULT_DIR.mkdirs();
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(ResultsConfig.RESULT_DIR, "searchspace-algorithms-in-space.txt")))) {
			bw.write(sb.toString());
		}
	}
}

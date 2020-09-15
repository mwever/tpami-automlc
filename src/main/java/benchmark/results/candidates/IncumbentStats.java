package benchmark.results.candidates;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.api4.java.datastructure.kvstore.IKVStore;

import ai.libs.jaicore.basic.FileUtil;
import ai.libs.jaicore.basic.ValueUtil;
import ai.libs.jaicore.basic.kvstore.KVStoreCollection;
import ai.libs.jaicore.basic.kvstore.KVStoreCollectionOneLayerPartition;
import ai.libs.jaicore.basic.kvstore.KVStoreUtil;
import benchmark.results.ResultsConfig;
import benchmark.results.ResultsUtil;

public class IncumbentStats {

	private static final boolean SHOW_NONES = true;
	private static final double THRESHOLD = 0.05;
	private static final List<String> CHARTS = Arrays.asList("meka_meta", "meka_base", "weka_meta", "weka_base", "weka_kernel");

	private static final StringBuilder sb = new StringBuilder();

	public static void main(final String[] args) throws IOException {
		KVStoreCollection datacol = KVStoreUtil.readFromCSVDataWithHeader(FileUtil.readFileAsList(ResultsConfig.INCUMBENT_DATA_FILE), new HashMap<>(), ";");

		Set<String> includedAlgorithms = new HashSet<>();
		for (IKVStore store : datacol) {
			includedAlgorithms.addAll(getAlgoNames(store.getAsString("incumbent")));
		}
		List<String> algorithmSorting = new ArrayList<>(includedAlgorithms);
		Collections.sort(algorithmSorting);

		Map<String, List<String>> algorithmPartition = new HashMap<>();
		algorithmPartition.put("meka_meta", algorithmSorting.stream().filter(ResultsUtil::isMekaMeta).collect(Collectors.toList()));
		algorithmPartition.put("meka_base", algorithmSorting.stream().filter(ResultsUtil::isMekaBase).collect(Collectors.toList()));
		algorithmPartition.put("weka_meta", algorithmSorting.stream().filter(ResultsUtil::isWekaMeta).collect(Collectors.toList()));
		algorithmPartition.put("weka_base", algorithmSorting.stream().filter(ResultsUtil::isWekaBase).collect(Collectors.toList()));
		algorithmPartition.put("weka_kernel", algorithmSorting.stream().filter(ResultsUtil::isWekaKernel).collect(Collectors.toList()));

		algorithmPartition.entrySet().stream().map(x -> "columns_" + x.getKey() + "=['" + x.getValue().stream().map(y -> y.substring(y.lastIndexOf('.') + 1)).map(ResultsUtil::shortenAlgoName).collect(Collectors.joining("','")) + "']\n")
				.forEach(sb::append);

		getApproachWiseAlgorithmFrequencies(datacol, algorithmPartition);

		for (Entry<String, KVStoreCollection> rootEntry : new KVStoreCollectionOneLayerPartition("measure", datacol)) {
			sb.append(rootEntry.getKey()).append("\n");
			getApproachWiseAlgorithmFrequencies(rootEntry.getValue(), algorithmPartition);
		}

		ResultsConfig.RESULT_DIR.mkdirs();
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(ResultsConfig.RESULT_DIR, "incumbent-frequencies.txt")))) {
			bw.write(sb.toString());
		}
	}

	private static void getApproachWiseAlgorithmFrequencies(final KVStoreCollection col, final Map<String, List<String>> algorithmPartition) {
		KVStoreCollectionOneLayerPartition approachPart = new KVStoreCollectionOneLayerPartition("approach", col);
		Map<String, List<String>> approachWiseMap = partitionToListMap(approachPart);
		Map<String, Double> approachNMap = new HashMap<>();
		approachPart.entrySet().stream().forEach(x -> approachNMap.put(x.getKey(), (double) x.getValue().size()));

		Map<String, Map<String, Double>> approachWiseFrequencyMap = countFrequencies(approachWiseMap, approachNMap);
		for (Entry<String, Map<String, Double>> entry : approachWiseFrequencyMap.entrySet()) {
			sb.append(entry.getKey()).append("\n");

			for (String chart : CHARTS) {
				// System.out.println(chart + ": " + algorithmPartition.get(chart).stream().mapToDouble(x -> entry.getValue().computeIfAbsent(x, t -> 0.0)).sum());
				String chartVector = algorithmPartition.get(chart).stream().map(x -> ((SHOW_NONES || !x.contains("None")) && entry.getValue().computeIfAbsent(x, t -> 0.0) > THRESHOLD) ? entry.getValue().get(x) + "" : "0.0")
						.collect(Collectors.joining(","));
				sb.append(String.format("%s=[%s]", chart, chartVector)).append("\n");
			}
			sb.append("\n");
		}
	}

	private static String mapAlgoNameToOutput(final String name, final Map<String, Double> relFrequencyMap) {
		return ValueUtil.round(relFrequencyMap.get(name), 4) + "";
	}

	private static Map<String, List<String>> partitionToListMap(final KVStoreCollectionOneLayerPartition partition) {
		Map<String, List<String>> partitionMap = new HashMap<>();
		for (Entry<String, KVStoreCollection> entry : partition) {
			for (IKVStore store : entry.getValue()) {
				List<String> algoNames = getAlgoNames(store.getAsString("incumbent"));
				partitionMap.computeIfAbsent(entry.getKey(), t -> new ArrayList<>()).addAll(algoNames);
			}
		}
		return partitionMap;
	}

	private static List<String> getAlgoNames(final String incumbentString) {
		List<String> algos = new ArrayList<>(10);
		String[] algoSplit = incumbentString.split(" - ");

		for (String algo : algoSplit) {
			if (algo.trim().equals("")) {
				continue;
			}
			algos.add(algo.split("\\(")[0].trim());
		}

		if (algos.stream().filter(ResultsUtil::isMekaMeta).count() < 1) {
			algos.add("meka.z.meta.None");
		}

		if (algos.stream().filter(ResultsUtil::isMekaBase).count() < 1) {
			algos.add("meka.z.None");
		}

		if (algos.stream().filter(ResultsUtil::isWekaMeta).count() < 1) {
			algos.add("weka.z.meta.None");
		}

		if (algos.stream().filter(ResultsUtil::isWekaBase).count() < 1) {
			algos.add("weka.z.None");
		}

		if (algos.stream().filter(ResultsUtil::isWekaKernel).count() < 1) {
			algos.add("weka.supportVector.None");
		}

		return algos;
	}

	private static final Map<String, Map<String, Double>> countFrequencies(final Map<String, List<String>> algosMap, final Map<String, Double> normalization) {
		Map<String, Map<String, AtomicInteger>> counterMap = new HashMap<>();
		for (Entry<String, List<String>> entry : algosMap.entrySet()) {
			Map<String, AtomicInteger> wrappedMap = counterMap.computeIfAbsent(entry.getKey(), t -> new HashMap<>());
			entry.getValue().stream().forEach(x -> wrappedMap.computeIfAbsent(x, t -> new AtomicInteger()).incrementAndGet());
		}

		Map<String, Map<String, Double>> resultMap = new HashMap<>();
		counterMap.entrySet().stream().forEach(x -> x.getValue().entrySet().stream()
				.forEach(y -> resultMap.computeIfAbsent(x.getKey(), t -> new HashMap<>()).put(y.getKey(), y.getValue().get() / (normalization.containsKey(x.getKey()) ? normalization.get(x.getKey()) : 1.0))));
		return resultMap;
	}

}

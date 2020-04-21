package results;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.libs.jaicore.basic.Maps;
import ai.libs.jaicore.basic.ValueUtil;
import ai.libs.jaicore.basic.kvstore.KVStore;
import ai.libs.jaicore.basic.kvstore.KVStoreCollection;
import ai.libs.jaicore.basic.kvstore.KVStoreUtil;
import meka.core.MLUtils;
import weka.core.Instances;

public class DatasetProperties {

	private static final File DATASET_FOLDER = new File("original_datasets/");

	public static void main(final String[] args) throws Exception {
		KVStoreCollection col = new KVStoreCollection();
		int labelCounter = 0;

		for (File file : DATASET_FOLDER.listFiles()) {
			String datasetName = file.getName().substring(0, file.getName().length() - 5);
			System.out.println("Read dataset " + datasetName);
			KVStore store = new KVStore();
			store.put("dataset", datasetName);

			Instances data = new Instances(new FileReader(file));
			MLUtils.prepareData(data);

			KVStore instances = new KVStore(store.toString());
			instances.put("type", "#I");
			instances.put("value", data.numInstances());
			col.add(instances);

			KVStore labels = new KVStore(store.toString());
			labels.put("type", "#L");
			labels.put("value", data.classIndex());
			col.add(labels);

			labelCounter += data.classIndex();

			KVStore card = new KVStore(store.toString());
			card.put("type", "card.");
			card.put("value", ValueUtil.valueToString(avgLabelCardinality(data), 2));
			col.add(card);

			KVStore ulc = new KVStore(store.toString());
			ulc.put("type", "ULC");
			ulc.put("value", ValueUtil.valueToString(uniqueLabelCombinations(data), 2));
			col.add(ulc);

			KVStore labelToInstanceRatio = new KVStore(store.toString());
			labelToInstanceRatio.put("type", "L2IR");
			labelToInstanceRatio.put("value", ValueUtil.valueToString(labelToInstanceRatio(data), 4));
			col.add(labelToInstanceRatio);
		}

		System.out.println(labelCounter);
		System.out.println(KVStoreUtil.kvStoreCollectionToLaTeXTable(col, "dataset", "type", "value"));
	}

	private static double avgLabelCardinality(final Instances data) {
		return data.stream().mapToDouble(x -> IntStream.range(0, data.classIndex()).mapToDouble(y -> x.value(y)).sum()).average().getAsDouble();
	}

	private static double instancesWithUniqueLabelCombinations(final Instances data) {
		Map<String, Integer> counterMap = new HashMap<>();
		data.stream().map(x -> IntStream.range(0, data.classIndex()).mapToObj(y -> x.value(y) + "").collect(Collectors.joining("-"))).forEach(x -> Maps.increaseCounterInMap(counterMap, x));
		return counterMap.entrySet().stream().filter(x -> x.getValue() == 1).mapToDouble(x -> 1.0).sum() / data.size();
	}

	private static double uniqueLabelCombinations(final Instances data) {
		return data.stream().map(x -> IntStream.range(0, data.classIndex()).mapToObj(y -> x.value(y) + "").collect(Collectors.joining("-"))).collect(Collectors.toSet()).size();
	}

	private static double labelToInstanceRatio(final Instances data) {
		return (double) data.classIndex() / data.size();
	}

}

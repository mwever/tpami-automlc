package results;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.api4.java.datastructure.kvstore.IKVStore;

import ai.libs.jaicore.basic.Maps;
import ai.libs.jaicore.basic.kvstore.KVStoreCollection;
import ai.libs.jaicore.basic.kvstore.KVStoreCollectionOneLayerPartition;
import ai.libs.jaicore.basic.kvstore.KVStoreUtil;
import ai.libs.jaicore.basic.kvstore.Table;
import ai.libs.jaicore.db.sql.SQLAdapter;

public class SelectedClassifiers {

	private static final String[] measure = { "FMacroAvgD", "FMacroAvgL", "FMicroAvg" };
	private static final int measureIx = 1;

	public static void main(final String[] args) throws SQLException {
		SQLAdapter adapter = new SQLAdapter("isys-otfml.cs.upb.de", "results", "Hallo333!", "conference_tpami_mlc");

		KVStoreCollection allCol = KVStoreUtil.readFromMySQLQuery(adapter, "SELECT * FROM test_eval WHERE exception='lastCandidate'", new HashMap<>());
		Set<String> allKeys = allCol.stream().map(x -> x.getAsString("abstract_description").split("-")[0].split("\\(")[0]).map(x -> x.substring(x.lastIndexOf('.') + 1)).collect(Collectors.toSet());
		System.out.println(allKeys);
		System.out.println(allKeys.size());

		KVStoreCollection col = KVStoreUtil.readFromMySQLQuery(adapter, "SELECT * FROM test_eval WHERE exception='lastCandidate' AND measure='" + measure[measureIx] + "'", new HashMap<>());

		KVStoreCollectionOneLayerPartition partition = new KVStoreCollectionOneLayerPartition("thread_id", col);

		Table<Integer> table = new Table<>();
		for (Entry<String, KVStoreCollection> entry : partition.getData().entrySet()) {
			System.out.println(entry.getKey());
			Map<String, Integer> classifierCounterMap = new HashMap<>();
			allKeys.stream().forEach(x -> classifierCounterMap.put(x, 0));
			Map<Integer, Integer> depthCounterMap = new HashMap<>();

			for (IKVStore store : entry.getValue()) {
				Integer depth = store.getAsString("abstract_description").split("-").length - 1;
				Maps.increaseCounterInMap(depthCounterMap, depth);
				String mlClassifier = store.getAsString("abstract_description").split("-")[0].split("\\(")[0];
				Maps.increaseCounterInMap(classifierCounterMap, mlClassifier);
			}

			List<Entry<String, Integer>> entryList = new ArrayList<>(classifierCounterMap.entrySet());
			entryList.sort(new Comparator<Entry<String, Integer>>() {
				@Override
				public int compare(final Entry<String, Integer> o1, final Entry<String, Integer> o2) {
					return o1.getKey().compareTo(o2.getKey());
				}
			});

			for (Entry<String, Integer> counterEntry : entryList) {
				table.add(counterEntry.getKey().substring(counterEntry.getKey().lastIndexOf('.') + 1), entry.getKey(), counterEntry.getValue());
			}
		}

		System.out.println(table.toCSV("\t", "0"));

	}

}

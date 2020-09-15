package benchmark.results.candidates;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.api4.java.datastructure.kvstore.IKVStore;

import ai.libs.jaicore.basic.kvstore.KVStoreCollection;
import ai.libs.jaicore.basic.kvstore.KVStoreCollectionOneLayerPartition;
import ai.libs.jaicore.basic.kvstore.KVStoreCollectionTwoLayerPartition;
import ai.libs.jaicore.basic.kvstore.KVStoreUtil;
import benchmark.results.ResultsConfig;

public class BestIncumbentsPerDatasetSplit {

	public static void main(final String[] args) throws IOException {
		KVStoreCollection col = KVStoreUtil.readFromCSVWithHeader(ResultsConfig.INCUMBENT_DATA_FILE, new HashMap<>());

		for (Entry<String, KVStoreCollection> measureEntry : new KVStoreCollectionOneLayerPartition("measure", col)) {
			System.out.println(measureEntry.getKey());

			for (Entry<String, Map<String, KVStoreCollection>> partitionEntry : new KVStoreCollectionTwoLayerPartition("dataset", "split", measureEntry.getValue())) {
				System.out.println(partitionEntry.getKey());
				KVStoreCollection selection = new KVStoreCollection();
				for (Entry<String, KVStoreCollection> splitEntry : partitionEntry.getValue().entrySet()) {
					IKVStore bestScoreStore = null;
					for (IKVStore store : splitEntry.getValue()) {
						if (bestScoreStore == null || store.getAsDouble("finalScore") > bestScoreStore.getAsDouble("finalScore")) {
							bestScoreStore = store;
						}
					}
					selection.add(bestScoreStore);
				}

				System.out.println(selection);
			}
		}

	}

}

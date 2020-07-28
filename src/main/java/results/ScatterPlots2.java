package results;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.api4.java.datastructure.kvstore.IKVStore;

import ai.libs.jaicore.basic.kvstore.KVStoreCollection;
import ai.libs.jaicore.basic.kvstore.KVStoreCollectionTwoLayerPartition;
import ai.libs.jaicore.basic.kvstore.KVStoreSequentialComparator;
import ai.libs.jaicore.basic.kvstore.KVStoreUtil;

public class ScatterPlots2 {
	private static final String L_SETTING = "ASlib Scenario";
	private static final String L_APPROACH = "Algorithm Selector";
	private static final String L_SCORE = "normalized";

	private static final List<String> OWN_APPROACHES = Arrays.asList("ExpectationSurvivalForest", "AutoSurvivalForest", "GridSearchSurvivalForest");

	public static void main(final String[] args) throws SQLException, IOException {
		KVStoreCollection col = KVStoreUtil.readFromCSVWithHeader(new File("saas.csv"), new HashMap<>(), ",");
		col.sort(new KVStoreSequentialComparator(L_APPROACH, L_SETTING));

		List<String> approach = Arrays.asList("SUNNY*like", "PerAlgorithmRegressor", "SATzilla'11*like", "ISAC*like", "MultiClassSelector");
		List<String> imputation = Arrays.asList("Runtime", "Runtime", "Runtime", "PAR10", "PAR10");
		approach.stream().forEach(col::removeAny); // clean former file
		KVStoreCollection colBaselines = KVStoreUtil.readFromCSVWithHeader(new File("setting_comparison_normalized_par10.csv"), new HashMap<>(), ",");
		for (IKVStore store : colBaselines) {
			int indexOfApproach = approach.indexOf(store.getAsString(L_APPROACH));
			store.put(L_SCORE, store.get(imputation.get(indexOfApproach)));
			col.add(store);
		}

		List<Map<String, String>> selections = new ArrayList<>();

		for (String currentApproach : OWN_APPROACHES) {
			KVStoreCollectionTwoLayerPartition partition = new KVStoreCollectionTwoLayerPartition(L_APPROACH, L_SETTING, col);
			OWN_APPROACHES.stream().filter(x -> !x.equals(currentApproach)).forEach(partition.getData()::remove);
			List<String> algoList = new ArrayList<>(partition.getData().keySet());
			algoList.remove(currentApproach);
			StringBuilder sb = new StringBuilder();

			System.out.println(currentApproach);
			Map<String, KVStoreCollection> approachData = partition.getData().get(currentApproach);

			for (String competitor : algoList) {
				Map<String, KVStoreCollection> competitorData = partition.getData().get(competitor);
				for (String dataset : competitorData.keySet()) {
					KVStoreCollection apprCol = approachData.get(dataset);
					KVStoreCollection compCol = competitorData.get(dataset);
					if (!compCol.isEmpty() && !apprCol.isEmpty()) {
						sb.append(apprCol.get(0).getAsDouble(L_SCORE)).append("  ").append(compCol.get(0).getAsDouble(L_SCORE)).append("  ").append(competitor.toUpperCase()).append("\n");
					}

				}
			}
			System.out.println(sb.toString());
		}

	}

}

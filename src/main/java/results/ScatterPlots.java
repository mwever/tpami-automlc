package results;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.libs.jaicore.basic.kvstore.KVStoreCollection;
import ai.libs.jaicore.basic.kvstore.KVStoreCollection.EGroupMethod;
import ai.libs.jaicore.basic.kvstore.KVStoreCollectionTwoLayerPartition;
import ai.libs.jaicore.basic.kvstore.KVStoreSequentialComparator;
import ai.libs.jaicore.basic.kvstore.KVStoreUtil;
import ai.libs.jaicore.db.sql.SQLAdapter;

public class ScatterPlots {

	private static final String[] measures = { "FMacroAvgD", "FMacroAvgL", "FMicroAvg" };
	private static final int measureIx = 2;
	private static final Map<String, String> replacements = new HashMap<>();

	private static final String APPROACH = "bf";

	public static void main(final String[] args) throws SQLException {

		SQLAdapter adapter = new SQLAdapter("isys-otfml.cs.upb.de", "results", "Hallo333!", "conference_tpami_mlc");

		KVStoreCollection col = KVStoreUtil.readFromMySQLQuery(adapter, "SELECT * FROM testeval_jobs WHERE finalScore IS NOT NULL && measure='" + measures[measureIx] + "'", new HashMap<>());

		// load smac
//		KVStoreCollection colSMAC = KVStoreUtil.readFromMySQLQuery(adapter, "SELECT * FROM smac_clusterjobs WHERE finalScore IS NOT NULL && measure='" + measures[measureIx] + "'", new HashMap<>());
//		col.addAll(colSMAC);

		// load hb and bohb
//		KVStoreCollection hbLikeCol = KVStoreUtil.readFromMySQLQuery(adapter, "SELECT * FROM hblike_clusterjobs WHERE finalScore IS NOT NULL && measure='" + measures[measureIx] + "'", new HashMap<>());
//		col.addAll(hbLikeCol);

		Map<String, EGroupMethod> grouping = new HashMap<>();
		grouping.put("finalScore", EGroupMethod.AVG);
		KVStoreCollection grouped = col.group(new String[] { "algorithm", "measure", "dataset" }, grouping);

		grouped.sort(new KVStoreSequentialComparator("algorithm", "dataset"));

		KVStoreCollectionTwoLayerPartition partition = new KVStoreCollectionTwoLayerPartition("algorithm", "dataset", grouped);

		List<String> algoList = new ArrayList<>(partition.getData().keySet());
		algoList.remove(APPROACH);

		StringBuilder sb = new StringBuilder();

		Map<String, KVStoreCollection> approachData = partition.getData().get(APPROACH);
		for (String competitor : algoList) {
			Map<String, KVStoreCollection> competitorData = partition.getData().get(competitor);
			for (String dataset : competitorData.keySet()) {
				KVStoreCollection compCol = competitorData.get(dataset);
				KVStoreCollection apprCol = approachData.get(dataset);
				if (!compCol.isEmpty() && !apprCol.isEmpty()) {
					sb.append(apprCol.get(0).getAsDouble("finalScore")).append("  ").append(compCol.get(0).getAsDouble("finalScore")).append("  ").append(competitor.toUpperCase()).append("\n");
				}
			}
		}

		System.out.println(sb.toString());

	}

}

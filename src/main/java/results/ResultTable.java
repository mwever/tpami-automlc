package results;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.api4.java.datastructure.kvstore.IKVStore;

import ai.libs.jaicore.basic.ValueUtil;
import ai.libs.jaicore.basic.kvstore.KVStoreCollection;
import ai.libs.jaicore.basic.kvstore.KVStoreCollection.EGroupMethod;
import ai.libs.jaicore.basic.kvstore.KVStoreSequentialComparator;
import ai.libs.jaicore.basic.kvstore.KVStoreStatisticsUtil;
import ai.libs.jaicore.basic.kvstore.KVStoreUtil;
import ai.libs.jaicore.db.sql.SQLAdapter;

public class ResultTable {

	private static final String[] measures = { "FMacroAvgD", "FMacroAvgL", "FMicroAvg" };
	private static final int measureIx = 0;
	private static final Map<String, String> replacements = new HashMap<>();

	private static void loadReplacements() {
		replacements.put("smac", "0_smac");
		replacements.put("hb", "1_hb");
		replacements.put("bohb", "2_bohb");
		replacements.put("ggp", "3_ggp");
		replacements.put("bf", "4_bf");
		replacements.put("random", "5_random");
	}

	public static void main(final String[] args) throws SQLException {
		loadReplacements();

		SQLAdapter adapter = new SQLAdapter("isys-otfml.cs.upb.de", "results", "Hallo333!", "conference_tpami_mlc");

		KVStoreCollection col = KVStoreUtil.readFromMySQLQuery(adapter, "SELECT * FROM automlc_clusterjobs WHERE finalScore IS NOT NULL && measure='" + measures[measureIx] + "'", new HashMap<>());

		// load smac
		KVStoreCollection colSMAC = KVStoreUtil.readFromMySQLQuery(adapter, "SELECT * FROM smac_clusterjobs WHERE finalScore IS NOT NULL && measure='" + measures[measureIx] + "'", new HashMap<>());
		col.addAll(colSMAC);

		// load hb and bohb
		KVStoreCollection hbLikeCol = KVStoreUtil.readFromMySQLQuery(adapter, "SELECT * FROM hblike_clusterjobs WHERE finalScore IS NOT NULL && measure='" + measures[measureIx] + "'", new HashMap<>());
		col.addAll(hbLikeCol);

		Map<String, EGroupMethod> grouping = new HashMap<>();
		grouping.put("finalScore", EGroupMethod.AVG);
		KVStoreCollection grouped = col.group(new String[] { "algorithm", "measure", "dataset" }, grouping);

		System.out.println(grouped.get(0));

		grouped.stream().forEach(x -> x.put("finalScore", -1 * x.getAsDouble("finalScore")));
		KVStoreStatisticsUtil.best(grouped, "dataset", "algorithm", "finalScore");
		grouped.stream().forEach(x -> x.put("finalScore", -1 * x.getAsDouble("finalScore")));

		for (IKVStore store : grouped) {
			String replacedAlgoName = replacements.get(store.getAsString("algorithm"));
			if (replacedAlgoName == null) {
				System.out.println(store.getAsString("algorithm") + " " + store + " " + replacements);
			}
			store.put("algorithm", replacedAlgoName);
			store.put("entry", ValueUtil.valueToString(store.getAsDouble("finalScore"), 2) + "$\\pm$" + ValueUtil.valueToString(store.getAsDouble("finalScore_stdDev"), 2));
			if (store.getAsBoolean("best")) {
				store.put("entry", "\\underline{" + store.getAsString("entry") + "}");
			}
		}

		grouped.sort(new KVStoreSequentialComparator("algorithm", "dataset"));

		String latexTable = KVStoreUtil.kvStoreCollectionToLaTeXTable(grouped, "dataset", "algorithm", "entry");

		System.out.println(latexTable);

	}

}

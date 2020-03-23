package results;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.api4.java.datastructure.kvstore.IKVStore;

import ai.libs.jaicore.basic.ValueUtil;
import ai.libs.jaicore.basic.kvstore.KVStore;
import ai.libs.jaicore.basic.kvstore.KVStoreCollection;
import ai.libs.jaicore.basic.kvstore.KVStoreCollection.EGroupMethod;
import ai.libs.jaicore.basic.kvstore.KVStoreSequentialComparator;
import ai.libs.jaicore.basic.kvstore.KVStoreUtil;
import ai.libs.jaicore.db.sql.SQLAdapter;

public class ResultTable {

	private static final String[] measures = { "FMacroAvgD", "FMacroAvgL", "FMicroAvg" };
	private static final int measureIx = 2;
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

		KVStoreCollection col = KVStoreUtil.readFromMySQLQuery(adapter, "SELECT dataset, algorithm, split, finalScore FROM testeval_jobs WHERE finalScore IS NOT NULL && measure='" + measures[measureIx] + "'", new HashMap<>());

//		KVStore bohbProto = new KVStore();
//		bohbProto.put("dataset", "arts1");
//		bohbProto.put("algorithm", "bohb");
//		bohbProto.put("split", "-1");
//		bohbProto.put("finalScore", "0.0");
//		col.add(bohbProto);

		Map<String, EGroupMethod> grouping = new HashMap<>();
		grouping.put("finalScore", EGroupMethod.AVG);
		col.stream().forEach(x -> x.put("finalScore", 1 - x.getAsDouble("finalScore")));
		KVStoreCollection grouped = col.group(new String[] { "algorithm", "measure", "dataset" }, grouping);

		KVStoreStatisticsUtil.best(grouped, "dataset", "algorithm", "finalScore");
		KVStoreStatisticsUtil.rank(grouped, "dataset", "algorithm", "finalScore");
		KVStoreStatisticsUtil.bestWilcoxonSignedRankTest(grouped, "dataset", "algorithm", "finalScore_list", "sig");

		Map<String, DescriptiveStatistics> averageRankStats = KVStoreStatisticsUtil.averageRank(grouped, "algorithm", "rank");
		grouped.stream().forEach(x -> x.put("finalScore", 1 - ValueUtil.round(x.getAsDouble("finalScore"), 2)));

		for (IKVStore store : grouped) {
			String replacedAlgoName = replacements.get(store.getAsString("algorithm"));
			if (replacedAlgoName == null) {
				System.out.println(store.getAsString("algorithm") + " " + store + " " + replacements);
			}
			store.put("algorithm", replacedAlgoName);
			store.put("entry", ValueUtil.valueToString(store.getAsDouble("finalScore"), 2) + "$\\pm$" + ValueUtil.valueToString(store.getAsDouble("finalScore_stdDev"), 2));
			if (store.getAsBoolean("best")) {
				store.put("entry", "\\textbf{" + store.getAsString("entry") + "}");
			} else {
				if (store.getAsString("sig").equals("TIE")) {
					store.put("entry", "\\underline{" + store.getAsString("entry") + "}");
				}
			}
		}

		grouped.sort(new KVStoreSequentialComparator("algorithm", "dataset"));

		for (Entry<String, DescriptiveStatistics> entry : averageRankStats.entrySet()) {
			IKVStore store = new KVStore();
			store.put("dataset", "avg. rank");
			store.put("algorithm", replacements.get(entry.getKey()));
			store.put("entry", ValueUtil.valueToString(entry.getValue().getMean(), 2));
			grouped.add(store);
		}

		String latexTable = KVStoreUtil.kvStoreCollectionToLaTeXTable(grouped, "dataset", "algorithm", "entry");

		System.out.println(latexTable);

	}

}

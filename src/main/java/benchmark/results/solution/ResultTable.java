package benchmark.results.solution;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.api4.java.datastructure.kvstore.IKVStore;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.libs.jaicore.basic.ValueUtil;
import ai.libs.jaicore.basic.kvstore.KVStore;
import ai.libs.jaicore.basic.kvstore.KVStoreCollection;
import ai.libs.jaicore.basic.kvstore.KVStoreCollection.EGroupMethod;
import ai.libs.jaicore.basic.kvstore.KVStoreSequentialComparator;
import ai.libs.jaicore.basic.kvstore.KVStoreStatisticsUtil;
import ai.libs.jaicore.basic.kvstore.KVStoreUtil;
import ai.libs.jaicore.db.sql.SQLAdapter;
import benchmark.results.ELoadStrategy;
import benchmark.results.ResultsConfig;

public class ResultTable {

	private static final File BASE_OUTPUT_DIR = new File(ResultsConfig.RESULT_DIR, "tables");
	private static final String TEST_FILE_NAME = "result_table_test_%s.data";
	private static final String INTERNAL_FILE_NAME = "result_table_internal_%s.data";
	private static final String RANDOM_INTERNAL_FILE_NAME = "result_table_random_internal.data";

	private static final Map<String, String> replacements = new HashMap<>();

	private static final ELoadStrategy LOAD_STRATEGY = ELoadStrategy.FILE;
	private static final boolean INCLUDE_INTERN = false;

	private static void loadReplacements() {
		replacements.put("smac", "0_smac");
		replacements.put("hb", "1_hb");
		replacements.put("bohb", "2_bohb");
		replacements.put("ggp", "3_ggp");
		replacements.put("bf", "4_bf");
		replacements.put("random", "5_random");
	}

	private static void refreshKVStoreFiles(final int measureIx) throws SQLException, IOException {
		// Load test data
		SQLAdapter adapter = new SQLAdapter("", "", "", "");
		KVStoreCollection col = KVStoreUtil.readFromMySQLQuery(adapter, "SELECT dataset, algorithm, split, finalScore FROM testeval_jobs WHERE finalScore IS NOT NULL && measure='" + ResultsConfig.MEASURES.get(measureIx) + "'",
				new HashMap<>());
		col.setCollectionID("ResultTable_" + measureIx);
		col.serializeTo(new File(ResultsConfig.RESULT_DATA_DIR, String.format(TEST_FILE_NAME, measureIx)));

		if (INCLUDE_INTERN) {
			// Load internal data for all methods except for random search
			Map<String, String> commonFields = new HashMap<>();
			KVStoreCollection internal = KVStoreUtil.readFromMySQLQuery(adapter, "SELECT * FROM bohb_clusterjobs NATURAL JOIN BOHB_InternalFMeasure", commonFields);
			internal.addAll(KVStoreUtil.readFromMySQLQuery(adapter, "SELECT * FROM ggp_clusterjobs NATURAL JOIN GGP_InternalFMeasure", commonFields));
			internal.addAll(KVStoreUtil.readFromMySQLQuery(adapter, "SELECT * FROM hblike_clusterjobs NATURAL JOIN HB_InternalFMeasure", commonFields));
			internal.addAll(KVStoreUtil.readFromMySQLQuery(adapter, "SELECT * FROM automlc_clusterjobs NATURAL JOIN HTNBF_InternalFMeasure", commonFields));
			internal.addAll(KVStoreUtil.readFromMySQLQuery(adapter, "SELECT * FROM smac_clusterjobs NATURAL JOIN SMAC_InternalFMeasure", commonFields));
			internal.setCollectionID("ResultTable_internal_" + measureIx);
			internal.serializeTo(new File(ResultsConfig.RESULT_DATA_DIR, String.format(INTERNAL_FILE_NAME, measureIx)));

			// Load internal data for random search
			KVStoreCollection randomEval = KVStoreUtil.readFromMySQLQuery(adapter, "SELECT * FROM random_eval as e JOIN random_clusterjobs as j ON e.experiment_id=j.experiment_id WHERE e.eval_value IS NOT NULL", commonFields);
			randomEval.setCollectionID("ResultTable_internal_random");
			randomEval.serializeTo(new File(ResultsConfig.RESULT_DATA_DIR, RANDOM_INTERNAL_FILE_NAME));
		}
	}

	public static void main(final String[] args) throws SQLException, IOException {
		loadReplacements();
		BASE_OUTPUT_DIR.mkdirs();

		for (int measureIx = 0; measureIx < ResultsConfig.MEASURES.size(); measureIx++) {
			if (LOAD_STRATEGY == ELoadStrategy.DB) {
				refreshKVStoreFiles(measureIx);
			}

			KVStoreCollection col = new KVStoreCollection(FileUtils.readFileToString(new File(ResultsConfig.RESULT_DATA_DIR, String.format(TEST_FILE_NAME, measureIx))));

			Map<String, EGroupMethod> grouping = new HashMap<>();
			grouping.put("finalScore", EGroupMethod.AVG);
			col.stream().forEach(x -> x.put("finalScore", 1 - ValueUtil.round(x.getAsDouble("finalScore"), 2)));
			String[] groupKeys = new String[] { "algorithm", "measure", "dataset" };
			KVStoreCollection grouped = col.group(groupKeys, grouping);

			Map<String, DescriptiveStatistics> averageRankStats = calculateStatistics(grouped);

			// aliasing the algorithms for convenient sorting
			grouped.stream().forEach(x -> x.put("algorithm", replacements.get(x.getAsString("algorithm"))));

			Map<String, DescriptiveStatistics> intAvgRankStats = null;
			if (INCLUDE_INTERN) {
				int currentMeasureIx = measureIx;
				KVStoreCollection internal = new KVStoreCollection(FileUtils.readFileToString(new File(ResultsConfig.RESULT_DATA_DIR, String.format(INTERNAL_FILE_NAME, currentMeasureIx))));
				internal = new KVStoreCollection(internal.stream().filter(x -> x.containsKey("measure") && x.getAsString("measure").equals(ResultsConfig.MEASURES.get(currentMeasureIx))).map(x -> {
					x.put("algorithm", replacements.get(x.getAsString("algorithm")) + "_int");
					if (x.containsKey("fmeasure")) {
						x.put("finalScore", x.get("fmeasure"));
					} else if (x.containsKey("fMeasure")) {
						x.put("finalScore", x.get("fMeasure"));
					}
					x.put("finalScore", 1 - ValueUtil.round(x.getAsDouble("finalScore"), 2));
					return x;
				}).collect(Collectors.toList()));

				KVStoreCollection randomEval = new KVStoreCollection(FileUtils.readFileToString(new File(ResultsConfig.RESULT_DATA_DIR, RANDOM_INTERNAL_FILE_NAME)));
				ObjectMapper mapper = new ObjectMapper();
				for (IKVStore store : randomEval) {
					store.put("algorithm", replacements.get(store.getAsString("algorithm")) + "_int");
					store.put("finalScore", ValueUtil.round(mapper.readTree(store.getAsString("evaluation_report")).get(ResultsConfig.MEASURES.get(currentMeasureIx) + "_mean").asDouble(), 2));
				}
				randomEval.projectRemove("exception", "evaluation_report", "eval_value", "time_until_found", "time_started", "order_no", "experiment_id", "thread_id", "memory_max", "done", "type", "cpus", "timestamp_found",
						"abstract_description", "component_instance", "host", "time_created", "evaluationTimeout", "id", "time_end", "globalTimeout", "done_time", "measure", "seed");

				Map<String, EGroupMethod> groupHandler = new HashMap<>();
				groupHandler.put("finalScore", EGroupMethod.AVG);
				KVStoreCollection randomBest = randomEval.group(new String[] { "split", "dataset", "algorithm", "exception" }, groupHandler);
				randomBest.stream().forEach(x -> x.put("finalScore", ValueUtil.round(x.getAsDouble("finalScore_min"), 2)));
				randomBest.projectRemove("GROUP_SIZE", "finalScore_list", "finalScore_max", "finalScore_min", "finalScore_sum", "finalScore_var", "finalScore_stdDev");

				internal.addAll(randomBest);

				KVStoreCollection intGrouped = internal.group(groupKeys, grouping);
				intGrouped.stream().forEach(x -> x.put("finalScore", ValueUtil.round(x.getAsDouble("finalScore"), 2)));
				intAvgRankStats = calculateStatistics(intGrouped);

				grouped.addAll(intGrouped);

			}
			grouped.sort(new KVStoreSequentialComparator("algorithm", "dataset"));

			for (IKVStore store : grouped) {
				store.put("entry", ValueUtil.valueToString(store.getAsDouble("finalScore"), 2) + "$\\pm$" + ValueUtil.valueToString(store.getAsDouble("finalScore_stdDev"), 2));
				if (store.getAsBoolean("best")) {
					store.put("entry", "\\textbf{" + store.getAsString("entry") + "}");
				} else {
					if (store.getAsString("sig").equals("TIE")) {
						store.put("entry", "\\underline{" + store.getAsString("entry") + "}");
					}
				}
			}

			// AVG Rank Statistics
			for (Entry<String, DescriptiveStatistics> entry : averageRankStats.entrySet()) {
				IKVStore store = new KVStore();
				store.put("dataset", "avg. rank");
				store.put("algorithm", replacements.get(entry.getKey()));
				store.put("entry", ValueUtil.valueToString(entry.getValue().getMean(), 2));
				grouped.add(store);
			}
			if (INCLUDE_INTERN) {
				for (Entry<String, DescriptiveStatistics> entry : intAvgRankStats.entrySet()) {
					IKVStore store = new KVStore();
					store.put("dataset", "avg. rank");
					store.put("algorithm", entry.getKey());
					store.put("entry", ValueUtil.valueToString(entry.getValue().getMean(), 2));
					grouped.add(store);
				}
			}
			String latexTable = KVStoreUtil.kvStoreCollectionToLaTeXTable(grouped, "dataset", "algorithm", "entry", "0.00$\\pm$0.00");
			latexTable = latexTable.replaceAll("0\\.", ".");

			try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(BASE_OUTPUT_DIR, String.format("table_%s%s.tex", (INCLUDE_INTERN ? "withintern_" : ""), ResultsConfig.MEASURES.get(measureIx)))))) {
				bw.write(latexTable);
			}

			System.out.println(latexTable);
		}
	}

	private static Map<String, DescriptiveStatistics> calculateStatistics(final KVStoreCollection collection) {
		KVStoreStatisticsUtil.best(collection, "dataset", "algorithm", "finalScore");
		KVStoreStatisticsUtil.rank(collection, "dataset", "algorithm", "finalScore");
		KVStoreStatisticsUtil.bestWilcoxonSignedRankTest(collection, "dataset", "algorithm", "split", "finalScore_list", "sig");

		Map<String, DescriptiveStatistics> averageRankStats = KVStoreStatisticsUtil.averageRank(collection, "algorithm", "rank");
		collection.stream().forEach(x -> x.put("finalScore", 1 - x.getAsDouble("finalScore")));
		return averageRankStats;
	}

}

package benchmark.results.candidates;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.api4.java.datastructure.kvstore.IKVStore;

import ai.libs.jaicore.basic.FileUtil;
import ai.libs.jaicore.basic.kvstore.KVStoreCollection;
import ai.libs.jaicore.basic.kvstore.KVStoreSequentialComparator;
import ai.libs.jaicore.basic.kvstore.KVStoreUtil;
import ai.libs.jaicore.basic.sets.SetUtil;
import ai.libs.jaicore.db.sql.SQLAdapter;

public class EvaluationTimes {
	private static final String[] tools = { "BOHB", "GGP", "HB", "SMAC", "HTNBF", "Random" };
	private static final boolean COLUMN_WISE = true;

	private static void downloadData() throws SQLException, IOException {
		Map<String, String> jobsTable = new HashMap<>();
		jobsTable.put(tools[0], "bohb_clusterjobs");
		jobsTable.put(tools[1], "ggp_clusterjobs");
		jobsTable.put(tools[2], "hblike_clusterjobs");
		jobsTable.put(tools[3], "smac_clusterjobs");
		jobsTable.put(tools[4], "automlc_clusterjobs");
		jobsTable.put(tools[5], "random_clusterjobs");

		SQLAdapter adapter = new SQLAdapter("", "", "", "");
		for (String tool : tools) {
			System.out.println("Download data for " + tool + "...");
			KVStoreCollection col = KVStoreUtil.readFromMySQLQuery(adapter, "SELECT * FROM `EvalTimes_" + tool + "` NATURAL JOIN " + jobsTable.get(tool), new HashMap<>());
			col.setCollectionID("EvalTimes_" + tool);
			col.projectRemove("exception", "finalScore", "measure", "time_started", "time_end", "seed", "cpus", "memory_max", "experiment_id", "split", "host", "done", "globalTimeout", "evaluationTimeout", "time_created", "done_time");
			col.serializeTo(new File("data/eval_times_" + tool + ".data"));
		}
	}

	public static void main(final String[] args) throws SQLException, IOException {
//		downloadData();
		KVStoreCollection col = new KVStoreCollection();
		for (String tool : tools) {
			col.addAll(new KVStoreCollection(FileUtil.readFileAsString(new File("data/eval_times_" + tool + ".data"))));
		}
		KVStoreCollection grouped = col.group("dataset");

		grouped.stream().forEach(x -> x.put("evalTime", x.getAsString("evalTime").replaceAll(",,", ",")));
		grouped.stream().forEach(x -> {
			List<Integer> list = x.getAsIntList("evalTime").stream().map(y -> (int) Math.round((double) y / 1000)).collect(Collectors.toList());
			Collections.shuffle(list);
			x.put("evalTime", SetUtil.implode(list, ","));
		});
		grouped.sort(new KVStoreSequentialComparator("dataset"));

		if (COLUMN_WISE) {
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File("boxplots/boxplots-columnwise.txt")))) {
				bw.write(SetUtil.implode(grouped.stream().map(x -> x.getAsString("dataset")).collect(Collectors.toList()), ";") + "\n");
				int maxListSize = grouped.stream().mapToInt(x -> x.getAsIntList("evalTime").size()).max().getAsInt();
				for (int i = 0; i < maxListSize; i++) {
					int currentI = i;
					bw.write(grouped.stream().map(x -> (x.getAsIntList("evalTime").size() > currentI) ? x.getAsIntList("evalTime").get(currentI) + "" : "").collect(Collectors.joining(";")) + "\n");
				}
			}
		} else {
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File("boxplots/boxplots-rowwise.txt")))) {
				for (IKVStore store : grouped) {
					bw.write(store.getAsString("dataset"));
					bw.write(";");
					bw.write(SetUtil.implode(store.getAsIntList("evalTime"), ";"));
					bw.write("\n");
				}
			}
		}
	}

}

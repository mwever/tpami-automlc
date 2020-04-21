package results.anytime;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;

import ai.libs.jaicore.basic.kvstore.KVStoreCollection;
import ai.libs.jaicore.basic.kvstore.KVStoreCollectionTwoLayerPartition;
import ai.libs.jaicore.basic.kvstore.KVStoreUtil;
import ai.libs.jaicore.db.sql.SQLAdapter;

public class AnytimeAverageRankPlotter {

	private static final boolean LOAD_DATA = false;

	private static final String[] measures = { "FMacroAvgD", "FMacroAvgL", "FMicroAvg" };
	private static final long MAX_TIMESTAMP = 1000 * 60 * 60 * 24;
	private static final Map<String, String> COLOR_MAP = new HashMap<>();

	public static void main(final String[] args) throws SQLException, IOException {
		COLOR_MAP.put("smac", "blue");
		COLOR_MAP.put("hb", "red");
		COLOR_MAP.put("bohb", "magenta");
		COLOR_MAP.put("bf", "green");
		COLOR_MAP.put("ggp", "brown");
		COLOR_MAP.put("random", "black");

		for (int measureIx = 0; measureIx < 3; measureIx++) {
			String fileName = "data/anytime_avg_rank_" + measureIx + ".data";
			if (LOAD_DATA) {
				SQLAdapter adapter = new SQLAdapter("", "", "", "");
				KVStoreCollection col = KVStoreUtil.readFromMySQLQuery(adapter, "SELECT dataset, algorithm, split, trace, finalScore FROM testeval_jobs WHERE finalScore IS NOT NULL && measure='" + measures[measureIx] + "'",
						new HashMap<>());
				col.setCollectionID("AnytimeAverageRankData_" + measureIx);
				col.serializeTo(new File(fileName));
			}

			KVStoreCollection col = new KVStoreCollection(FileUtils.readFileToString(new File(fileName)));

			Map<String, List<IAnytimePlot>> algoWiseRankPlots = new HashMap<>();
			KVStoreCollectionTwoLayerPartition partition = new KVStoreCollectionTwoLayerPartition("dataset", "algorithm", col);

			for (Entry<String, Map<String, KVStoreCollection>> datasetWiseEntry : partition) {
				StringBuilder plotsSB = new StringBuilder();

				List<String> algoList = new ArrayList<>();
				List<IAnytimePlot> algoPlotList = new ArrayList<>();
				for (Entry<String, KVStoreCollection> algoWiseEntry : datasetWiseEntry.getValue().entrySet()) {
					List<IAnytimePlot> plotList = algoWiseEntry.getValue().stream().map(x -> {
						try {
							return new AnytimePlot(x, "trace");
						} catch (IOException e) {
							e.printStackTrace();
							return null;
						}
					}).filter(x -> x != null).collect(Collectors.toList());

					// sanity check whether the plot values are matching the final scores:
					for (int i = 0; i < plotList.size(); i++) {
						double scoreAtEnd = plotList.get(i).getScoreAtTime(MAX_TIMESTAMP);
						double finalScore = algoWiseEntry.getValue().get(i).getAsDouble("finalScore");
					}

					String algo = algoWiseEntry.getKey();

					algoList.add(algo);
					AverageAnytimePlot avgAnytimePlot = new AverageAnytimePlot(plotList);
					algoPlotList.add(avgAnytimePlot);

					plotsSB.append(avgAnytimePlot.toString(MAX_TIMESTAMP, COLOR_MAP.get(algo)));
					plotsSB.append("\n\\addlegendentry{" + algo + "}\n");
				}

				List<IAnytimePlot> rankPlots = AnytimeRankPlotCreator.getRankPlots(algoPlotList, false);

				IntStream.range(0, rankPlots.size()).forEach(x -> algoWiseRankPlots.computeIfAbsent(algoList.get(x), t -> new ArrayList<>()).add(rankPlots.get(x)));

				try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(new File("plots/"), datasetWiseEntry.getKey() + ".tex")))) {
					bw.write(plotsSB.toString());
				}
			}

			System.out.println("Generate plots...");
			StringBuilder avgRankPlotSB = new StringBuilder();
			String[] keys = { "smac", "hb", "bohb", "ggp", "bf", "random" };

			for (String key : keys) {
				AverageAnytimePlot avgRankAnytimePlot = new AverageAnytimePlot(algoWiseRankPlots.get(key));
				avgRankPlotSB.append(avgRankAnytimePlot.toString(MAX_TIMESTAMP, COLOR_MAP.get(key)));
				avgRankPlotSB.append("\n\\addlegendentry{" + key + "}\n");
			}
			System.out.println("Write file...");
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(new File("plots/"), "avgrank-" + measures[measureIx] + ".tex")))) {
				bw.write(avgRankPlotSB.toString());
			}
		}
	}

}

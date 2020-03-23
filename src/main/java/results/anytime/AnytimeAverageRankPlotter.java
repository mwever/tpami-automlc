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

import ai.libs.jaicore.basic.kvstore.KVStoreCollection;
import ai.libs.jaicore.basic.kvstore.KVStoreCollectionTwoLayerPartition;
import ai.libs.jaicore.basic.kvstore.KVStoreUtil;
import ai.libs.jaicore.db.sql.SQLAdapter;

public class AnytimeAverageRankPlotter {

	private static final String[] measures = { "FMacroAvgD", "FMacroAvgL", "FMicroAvg" };
	private static final int measureIx = 2;
	private static final long MAX_TIMESTAMP = 1000 * 60 * 60 * 24;
	private static final Map<String, String> COLOR_MAP = new HashMap<>();

	public static void main(final String[] args) throws SQLException, IOException {
		SQLAdapter adapter = new SQLAdapter("isys-otfml.cs.upb.de", "results", "Hallo333!", "conference_tpami_mlc");
		KVStoreCollection col = KVStoreUtil.readFromMySQLQuery(adapter, "SELECT dataset, algorithm, split, trace, finalScore FROM testeval_jobs WHERE finalScore IS NOT NULL && measure='" + measures[measureIx] + "'", new HashMap<>());

		COLOR_MAP.put("smac", "green");
		COLOR_MAP.put("hb", "red");
		COLOR_MAP.put("bf", "blue");
		COLOR_MAP.put("ggp", "brown");
		COLOR_MAP.put("random", "black");

		Map<String, List<IAnytimePlot>> algoWiseRankPlots = new HashMap<>();
		KVStoreCollectionTwoLayerPartition partition = new KVStoreCollectionTwoLayerPartition("dataset", "algorithm", col);

		for (Entry<String, Map<String, KVStoreCollection>> datasetWiseEntry : partition) {
			StringBuilder plotsSB = new StringBuilder();
			String dataset = datasetWiseEntry.getKey();

			System.out.println(dataset);
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
					if (Math.abs(scoreAtEnd - finalScore) > 1E-8) {
						System.out.println(algoWiseEntry.getValue().get(i));
						System.err.println("Expected: " + finalScore + " Actual: " + scoreAtEnd);
					}
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

		StringBuilder avgRankPlotSB = new StringBuilder();
		for (Entry<String, List<IAnytimePlot>> algoRankPlots : algoWiseRankPlots.entrySet()) {
			AverageAnytimePlot avgRankAnytimePlot = new AverageAnytimePlot(algoRankPlots.getValue());
			avgRankPlotSB.append(avgRankAnytimePlot.toString(MAX_TIMESTAMP, COLOR_MAP.get(algoRankPlots.getKey())));
			avgRankPlotSB.append("\n\\addlegendentry{" + algoRankPlots.getKey() + "}\n");
		}
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(new File("plots/"), "avgrank-" + measures[measureIx] + ".tex")))) {
			bw.write(avgRankPlotSB.toString());
		}

	}

}

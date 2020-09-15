package benchmark.results.anytime;

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
import benchmark.results.ELoadStrategy;
import benchmark.results.ResultsConfig;

public class AnytimeAverageRankPlotter {

	private static final String DATA_FILENAME_PATTERN = "anytime_avg_rank_%s.data";

	private static final long MAX_TIMESTAMP = 1000 * 60 * 60 * 24;

	private static final File OUT_DIR = new File(ResultsConfig.RESULT_DIR, "anytime-plots");
	private static final String OUT_FILENAME_PATTERN = "avgrank-%s.tex";

	public static void main(final String[] args) throws SQLException, IOException {
		OUT_DIR.mkdirs();

		StringBuilder mainDoc = new StringBuilder();

		for (String measure : ResultsConfig.MEASURES) {
			File file = new File(ResultsConfig.RESULT_DATA_DIR, String.format(DATA_FILENAME_PATTERN, ResultsConfig.MEASURES.indexOf(measure)));

			if (ResultsConfig.LOAD_STRATEGY == ELoadStrategy.DB) {
				SQLAdapter adapter = new SQLAdapter(ResultsConfig.DB_HOST, ResultsConfig.DB_USER, ResultsConfig.DB_PWD, ResultsConfig.DB_BASE);
				KVStoreCollection col = KVStoreUtil.readFromMySQLQuery(adapter, "SELECT dataset, algorithm, split, trace, finalScore FROM testeval_jobs WHERE finalScore IS NOT NULL && measure='" + measure + "'", new HashMap<>());
				col.setCollectionID("AnytimeAverageRankData_" + measure);
				col.serializeTo(file);
			}

			KVStoreCollection col = new KVStoreCollection(FileUtils.readFileToString(file));

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

					String algo = algoWiseEntry.getKey();

					algoList.add(algo);
					AverageAnytimePlot avgAnytimePlot = new AverageAnytimePlot(plotList);
					algoPlotList.add(avgAnytimePlot);

					plotsSB.append(avgAnytimePlot.toString(MAX_TIMESTAMP, ResultsConfig.getApproachColorMap().get(algo)));
					plotsSB.append("\n\\addlegendentry{" + algo + "}\n");
				}

				List<IAnytimePlot> rankPlots = AnytimeRankPlotCreator.getRankPlots(algoPlotList, false);

				IntStream.range(0, rankPlots.size()).forEach(x -> algoWiseRankPlots.computeIfAbsent(algoList.get(x), t -> new ArrayList<>()).add(rankPlots.get(x)));

				try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(OUT_DIR, measure + "_" + datasetWiseEntry.getKey() + ".tex")))) {
					bw.write(plotsSB.toString());
				}
			}

			StringBuilder avgRankPlotSB = new StringBuilder();
			for (String key : ResultsConfig.APPROACHES) {
				AverageAnytimePlot avgRankAnytimePlot = new AverageAnytimePlot(algoWiseRankPlots.get(key));
				avgRankPlotSB.append(avgRankAnytimePlot.toString(MAX_TIMESTAMP, ResultsConfig.getApproachColorMap().get(key)));
				avgRankPlotSB.append("\n\\addlegendentry{" + key + "}\n");
			}

			try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(OUT_DIR, String.format(OUT_FILENAME_PATTERN, measure))))) {
				bw.write(avgRankPlotSB.toString());
			}
		}
	}

}

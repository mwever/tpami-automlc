package benchmark.results;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;

import ai.libs.jaicore.basic.kvstore.KVStoreCollection;
import ai.libs.jaicore.basic.kvstore.KVStoreCollection.EGroupMethod;
import ai.libs.jaicore.basic.kvstore.KVStoreCollectionTwoLayerPartition;
import ai.libs.jaicore.basic.kvstore.KVStoreSequentialComparator;
import ai.libs.jaicore.basic.kvstore.KVStoreUtil;
import ai.libs.jaicore.db.sql.SQLAdapter;

public class ScatterPlots {

	private static final ELoadStrategy LOAD_STRATEGY = ELoadStrategy.FILE;
	private static final String DATA_FILE_NAME = "data/scatter_plots_%s.data";
	private static final File BASE_OUT = new File(ResultsConfig.RESULT_DIR, "scatter-plots");
	private static final String OUT_FILE_NAME = "scatter-%sVSrest-%s.tex";
	private static final String PLOT_FILE_DIR = "plotdata/";

	// What approach to compare against the rest.

	private static KVStoreCollection loadFromDatabase(final String measure) throws SQLException {
		SQLAdapter adapter = new SQLAdapter(ResultsConfig.DB_HOST, ResultsConfig.DB_USER, ResultsConfig.DB_PWD, ResultsConfig.DB_BASE);
		KVStoreCollection col = KVStoreUtil.readFromMySQLQuery(adapter, "SELECT * FROM testeval_jobs WHERE finalScore IS NOT NULL && measure='" + measure + "'", new HashMap<>());
		col.setCollectionID("Scatter Plots Data " + measure);
		return col;
	}

	public static void main(final String[] args) throws SQLException, IOException {
		BASE_OUT.mkdirs();

		StringBuilder main = new StringBuilder();
		main.append("\\documentclass[multi=my,crop]{standalone}\n\\usepackage{tikz}\n\\usepackage{pgfplots}\n\\pgfplotsset{compat=1.5}\n\\begin{document}\n");

		Map<String, String> nameMap = new HashMap<>();
		nameMap.put("smac", "SMAC");
		nameMap.put("hb", "HB");
		nameMap.put("bohb", "BOHB");
		nameMap.put("ggp", "GGP");
		nameMap.put("bf", "HTN-BF");
		nameMap.put("random", "RANDOM");

		for (String approach : ResultsConfig.APPROACHES) {
			for (String measure : ResultsConfig.MEASURES) {
				KVStoreCollection col;
				switch (LOAD_STRATEGY) {
				case DB:
					col = loadFromDatabase(measure);
					break;
				case FILE:
				default:
					col = new KVStoreCollection(FileUtils.readFileToString(new File(String.format(DATA_FILE_NAME, ResultsConfig.MEASURES.indexOf(measure) + ""))));
				}
				Map<String, EGroupMethod> grouping = new HashMap<>();
				grouping.put("finalScore", EGroupMethod.AVG);
				KVStoreCollection grouped = col.group(new String[] { "algorithm", "measure", "dataset" }, grouping);

				grouped.sort(new KVStoreSequentialComparator("algorithm", "dataset"));

				KVStoreCollectionTwoLayerPartition partition = new KVStoreCollectionTwoLayerPartition("algorithm", "dataset", grouped);

				List<String> algoList = new ArrayList<>(partition.getData().keySet());
				algoList.remove(approach);
				algoList.sort(new Comparator<String>() {
					@Override
					public int compare(final String o1, final String o2) {
						int ixO1 = IntStream.range(0, ResultsConfig.APPROACHES.size()).filter(x -> ResultsConfig.APPROACHES.get(x).equals(o1)).findFirst().getAsInt();
						int ixO2 = IntStream.range(0, ResultsConfig.APPROACHES.size()).filter(x -> ResultsConfig.APPROACHES.get(x).equals(o2)).findFirst().getAsInt();
						return Integer.compare(ixO1, ixO2);
					}
				});

				StringBuilder sb = new StringBuilder();

				Map<String, KVStoreCollection> approachData = partition.getData().get(approach);
				for (String competitor : algoList) {
					Map<String, KVStoreCollection> competitorData = partition.getData().get(competitor);
					for (String dataset : competitorData.keySet()) {
						KVStoreCollection compCol = competitorData.get(dataset);
						KVStoreCollection apprCol = approachData.get(dataset);
						if (!compCol.isEmpty() && !apprCol.isEmpty()) {
							sb.append(apprCol.get(0).getAsDouble("finalScore")).append("  ").append(compCol.get(0).getAsDouble("finalScore")).append("  ").append(nameMap.get(competitor)).append("\n");
						}
					}
				}
				Map<String, String> scatterClasses = new HashMap<>();
				scatterClasses.put("smac", "SMAC={mark=triangle,blue}");
				scatterClasses.put("hb", "HB={mark=triangle,red}");
				scatterClasses.put("bohb", "BOHB={mark=triangle,magenta}");
				scatterClasses.put("ggp", "GGP={mark=square,brown}");
				scatterClasses.put("bf", "HTN-BF={mark=square,green}");
				scatterClasses.put("random", "RANDOM={mark=o,black}");

				main.append("\\begin{my}\n\t\\input{").append(PLOT_FILE_DIR).append(String.format(OUT_FILE_NAME, approach, measure)).append("}\n\\end{my}\n");
				try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(BASE_OUT, String.format(OUT_FILE_NAME, approach, measure))))) {
					bw.write("\\begin{tikzpicture}\n");

					bw.write("\\begin{axis}[xmin=0,xmax=1,ymin=0,ymax=1,legend pos=north west,extra x ticks={0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9},extra y ticks={0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9},tick style={grid=major}");
					bw.write(",xlabel=" + nameMap.get(approach));
					bw.write(",scatter/classes={");
					bw.write(algoList.stream().map(x -> scatterClasses.get(x)).collect(Collectors.joining(",")));
					bw.write("}");
					bw.write("]\n");
					bw.write("\\addplot[scatter,only marks,scatter src=explicit symbolic] table[meta=label] {\n");
					bw.write("x     y      label\n");
					bw.write(sb.toString());
					bw.write("};");

					for (String x : algoList) {
						bw.write("\\addlegendentry{" + nameMap.get(x) + "}\n");
					}
					bw.write("\\addplot[color=black] coordinates {\n\t(0,0)\n\t(1,1)\n};\n");
					bw.write("\\end{axis}\n\\end{tikzpicture}");
				}
			}

		}
		main.append("\\end{document}");

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(BASE_OUT, "main.tex")))) {
			bw.write(main.toString());
		}
	}
}
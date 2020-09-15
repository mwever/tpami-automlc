package benchmark.results.anytime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.libs.jaicore.basic.sets.Pair;

public class AnytimeRankPlotCreator {

	public static final List<IAnytimePlot> getRankPlots(final List<IAnytimePlot> plots, final boolean minimize) {
		Set<Long> allRelevantTimestamps = new HashSet<>();
		plots.stream().map(x -> x.getTimestampList()).forEach(allRelevantTimestamps::addAll);
		List<Long> timestamps = new ArrayList<>(allRelevantTimestamps);
		Collections.sort(timestamps);

		List<List<Double>> ranksList = new ArrayList<>(plots.size());
		IntStream.range(0, plots.size()).forEach(x -> ranksList.add(new ArrayList<>(allRelevantTimestamps.size())));

		for (Long timestamp : timestamps) {
			List<Pair<Integer, Double>> values = new ArrayList<>(plots.size());
			for (int i = 0; i < plots.size(); i++) {
				values.add(new Pair<>(i, plots.get(i).getScoreAtTime(timestamp)));
			}
			Collections.sort(values, new Comparator<Pair<Integer, Double>>() {
				@Override
				public int compare(final Pair<Integer, Double> o1, final Pair<Integer, Double> o2) {
					if (minimize) {
						return o1.getY().compareTo(o2.getY());
					} else {
						return o2.getY().compareTo(o1.getY());
					}
				}
			});
			int rank = 1;
			for (int i = 0; i < values.size(); i++) {
				ranksList.get(values.get(i).getX()).add((double) rank);
				if (i < values.size() - 1) {
					double currentValue = values.get(i).getY();
					double nextValue = values.get(i + 1).getY();
					if (Math.abs(currentValue - nextValue) > 1E-8) {
						rank++;
					}
				}
			}
		}

		return ranksList.stream().map(x -> new AnytimePlot(timestamps, x)).collect(Collectors.toList());
	}

}

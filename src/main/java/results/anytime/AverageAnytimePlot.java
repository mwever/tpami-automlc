package results.anytime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import ai.libs.jaicore.basic.ValueUtil;

public class AverageAnytimePlot implements IAverageAnytimePlot {

	private final NavigableMap<Long, DescriptiveStatistics> timestampMap;

	public AverageAnytimePlot(final Collection<IAnytimePlot> anytimePlots) {
		this.timestampMap = new TreeMap<>();
		for (Long timestamp : this.getRelevantTimestampsOfSubplots(anytimePlots)) {
			DescriptiveStatistics stats = this.timestampMap.computeIfAbsent(timestamp, t -> new DescriptiveStatistics());
			for (IAnytimePlot subplot : anytimePlots) {
				stats.addValue(subplot.getScoreAtTime(timestamp));
			}
		}
	}

	private List<Long> getRelevantTimestampsOfSubplots(final Collection<IAnytimePlot> anytimePlots) {
		Set<Long> timestampSet = new HashSet<>();
		anytimePlots.stream().map(x -> x.getTimestampList()).forEach(timestampSet::addAll);
		List<Long> timestamps = new ArrayList<>(timestampSet);
		Collections.sort(timestamps);
		return timestamps;
	}

	@Override
	public List<Long> getTimestampList() {
		return new ArrayList<>(this.timestampMap.keySet());
	}

	@Override
	public Double getScoreAtTime(final long timestamp) {
		return this.timestampMap.floorEntry(timestamp).getValue().getMean();
	}

	@Override
	public double getStandardDeviationAtTime(final long timestamp) {
		return this.timestampMap.floorEntry(timestamp).getValue().getStandardDeviation();
	}

	@Override
	public String toString() {
		return this.toString(this.getTimestampList(), "blue");
	}

	public String toString(final long maxTimestamp, final String color) {
		return this.toExcerptString(0, maxTimestamp, color);
	}

	public String toExcerptString(final long minTimestamp, final long maxTimestamp, final String color) {
		List<Long> timestamp = this.getTimestampList();
		Collections.sort(timestamp);
		while (timestamp.get(0) < minTimestamp) {
			timestamp.remove(0);
		}
		if (timestamp.get(0) > minTimestamp) {
			timestamp.add(0, minTimestamp);
		}

		while (timestamp.get(timestamp.size() - 1) > maxTimestamp) {
			timestamp.remove(timestamp.size() - 1);
		}
		if (timestamp.get(timestamp.size() - 1) < maxTimestamp) {
			timestamp.add(maxTimestamp);
		}
		return this.toString(timestamp, color);
	}

	public String toString(final List<Long> timestamps, final String color) {
		StringBuilder sb = new StringBuilder();
		sb.append("\\addplot[color=").append(color).append(",mark=none] coordinates {\n");
		for (int i = 0; i < timestamps.size(); i++) {
			long ts = timestamps.get(i);
			double currentScore = this.getScoreAtTime(ts);
			Double previousScore = null;
			Double nextScore = null;
			if (i > 0) {
				previousScore = this.getScoreAtTime(timestamps.get(i - 1));
			}
			if (i < timestamps.size() - 1) {
				nextScore = this.getScoreAtTime(timestamps.get(i + 1));
			}

			if (previousScore != null && nextScore != null && (Math.abs(currentScore - previousScore) < 1E-8 && Math.abs(currentScore - nextScore) < 1E-8)) {
				continue;
			}
			sb.append("(").append(ts).append(",").append(this.getScoreAtTime(ts)).append(")\n");
		}
		sb.append("};");
		return sb.toString();
	}

	@Override
	public String toLine() {
		StringBuilder sb = new StringBuilder();
		for (Long timestamp : this.getTimestampList()) {
			sb.append(ValueUtil.round(this.getScoreAtTime(timestamp), 2) + " ");
		}
		return sb.toString();
	}

}

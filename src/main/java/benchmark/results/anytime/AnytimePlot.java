package benchmark.results.anytime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.api4.java.datastructure.kvstore.IKVStore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.libs.jaicore.basic.ValueUtil;

public class AnytimePlot implements IAnytimePlot {

	private IKVStore plotData;
	private final NavigableMap<Long, Double> timestampMap;

	public AnytimePlot(final IKVStore plotData, final String traceFN) throws IOException {
		this.plotData = plotData;
		this.timestampMap = new TreeMap<>();
		this.timestampMap.put(0l, 0.0);
		JsonNode trace = new ObjectMapper().readTree(this.plotData.getAsString(traceFN));
		Iterator<Entry<String, JsonNode>> traceIt = trace.fields();
		while (traceIt.hasNext()) {
			Entry<String, JsonNode> traceElement = traceIt.next();
			this.timestampMap.put(Long.parseLong(traceElement.getKey()), traceElement.getValue().asDouble());
		}
	}

	public AnytimePlot(final List<Long> timestamps, final List<Double> values) {
		if (timestamps.size() != values.size()) {
			throw new IllegalArgumentException("The size of the timestamp list does not match the size of the value list");
		}
		this.timestampMap = new TreeMap<>();
		for (int i = 0; i < timestamps.size(); i++) {
			this.timestampMap.put(timestamps.get(i), values.get(i));
		}
	}

	@Override
	public List<Long> getTimestampList() {
		List<Long> timestamps = new ArrayList<>(this.timestampMap.keySet());
		Collections.sort(timestamps);
		return timestamps;
	}

	@Override
	public Double getScoreAtTime(final long timestamp) {
		return this.timestampMap.floorEntry(timestamp).getValue();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Long timestamp : this.getTimestampList()) {
			sb.append("(" + timestamp + "," + this.getScoreAtTime(timestamp) + ")");
		}
		return sb.toString() + "\n";
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

package benchmark.results.anytime;

import java.util.List;

public interface IAnytimePlot {

	public List<Long> getTimestampList();

	public Double getScoreAtTime(final long timestamp);

	public String toLine();

}

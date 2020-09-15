package benchmark.results;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultsConfig {

	private ResultsConfig() {
		// nothing to do here
	}

	// credentials for accessing experiment database
	public static final String DB_HOST = "";
	public static final String DB_USER = "";
	public static final String DB_PWD = "";
	public static final String DB_BASE = "";

	public static final ELoadStrategy LOAD_STRATEGY = ELoadStrategy.FILE;

	public static final File RESULT_DIR = new File("results/");

	public static final File RESULT_DATA_DIR = new File(RESULT_DIR, "data");
	public static final File INCUMBENT_DATA_FILE = new File(RESULT_DATA_DIR, "incumbents.csv");

	public static final List<String> MEASURES = Arrays.asList("FMacroAvgD", "FMacroAvgL", "FMicroAvg");

	public static final List<String> APPROACHES = Arrays.asList("smac", "hb", "bohb", "ggp", "bf", "random");

	public static final File SEARCH_SPACE_ROOT_FILE = new File("searchspace/meka-all.json");
	public static final String REQUESTED_INTERFACE = "MLClassifier";

	public static final File MLC_SEARCH_SPACE_ROOT_FILE = SEARCH_SPACE_ROOT_FILE;
	public static final String MLC_REQUESTED_INTERFACE = REQUESTED_INTERFACE;
	public static final File SLC_SEARCH_SPACE_ROOT_FILE = new File("searchspace/weka-all.json");
	public static final String SLC_REQUESTED_INTERFACE = "AbstractClassifier";

	private static final Map<String, String> COLOR_MAP = new HashMap<>();

	public static Map<String, String> getApproachColorMap() {
		if (COLOR_MAP.isEmpty()) {
			COLOR_MAP.put("smac", "blue");
			COLOR_MAP.put("hb", "red");
			COLOR_MAP.put("bohb", "magenta");
			COLOR_MAP.put("bf", "green");
			COLOR_MAP.put("ggp", "brown");
			COLOR_MAP.put("random", "black");
		}
		return COLOR_MAP;
	}

}

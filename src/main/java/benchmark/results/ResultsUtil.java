package benchmark.results;

import java.io.IOException;
import java.util.Collection;

import ai.libs.jaicore.components.model.Component;
import ai.libs.jaicore.components.serialization.ComponentLoader;

public class ResultsUtil {

	private ResultsUtil() {
		// TODO Auto-generated constructor stub
	}

	public static String shortenAlgoName(final String name) {
		if (name.length() <= 3 || name.toLowerCase().contains("rakel")) {
			return name;
		} else if (name.equals("None")) {
			return "/";
		}

		String shortenedString = "";
		for (char c : name.toCharArray()) {
			if (Character.isUpperCase(c)) {
				shortenedString += c;
			} else if (Character.isDigit(c)) {
				shortenedString += c;
			}
		}

		if (name.endsWith("dup")) {
			shortenedString.concat("dup");
		}

		return shortenedString;
	}

	public static boolean isMekaMeta(final String x) {
		return x.contains("meka") && x.contains("meta") || x.contains("MLCBMaD");
	}

	public static boolean isMekaBase(final String x) {
		return x.contains("meka") && !x.contains("meta") && !x.contains("MLCBMaD");
	}

	public static boolean isWekaMeta(final String x) {
		return x.contains("weka") && x.contains("meta") || x.contains("lazy.LWL");
	}

	public static boolean isWekaBase(final String x) {
		return x.contains("weka") && !x.contains("meta") && !x.contains("lazy.LWL") && !x.contains("supportVector");
	}

	public static boolean isWekaKernel(final String x) {
		return x.contains("weka") && x.contains("supportVector");
	}

	public static Collection<Component> getComponents() throws IOException {
		return new ComponentLoader(ResultsConfig.SEARCH_SPACE_ROOT_FILE).getComponents();
	}

}

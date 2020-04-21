package ai.libs.hyperopt.impl.optimizer.pcs.smac;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ScenarioFileBuilder {

	private static final String K_PCS_FILE = "paramfile";
	private static final String K_ALGO = "algo";
	private static final String K_RUN_OBJ = "run_obj";
	private static final String K_WALLCLOCK_LIMIT = "wallclock_limit";
	private static final String K_SHARED_MODEL = "shared_model";
	private static final String K_ABORT_ON_FIRST_RUN_CRASH = "abort_on_first_run_crash";
	private static final String K_INPUT_PSMAC_DIRS = "input_psmac_dirs";
	private static final String K_DETERMINISTIC = "deterministic";

	public static final String DEF_RUN_OBJ = "quality";
	public static final String DEF_ABORT_ON_FIRST_RUN_CRASH = "True";
	public static final String DEF_INPUT_PSMAC_DIRS = "smac3-output*";

	private final Map<String, String> map = new HashMap<>();

	public ScenarioFileBuilder() {
		this.withRunObj(DEF_RUN_OBJ).withAbortOnFirstRunCrash(DEF_ABORT_ON_FIRST_RUN_CRASH);
	}

	public ScenarioFileBuilder with(final String key, final String value) {
		this.map.put(key, value);
		return this;
	}

	public ScenarioFileBuilder withPCSFile(final String value) {
		this.map.put(K_PCS_FILE, value);
		return this;
	}

	public ScenarioFileBuilder withAlgo(final String value) {
		this.map.put(K_ALGO, value);
		return this;
	}

	public ScenarioFileBuilder withRunObj(final String value) {
		this.map.put(K_RUN_OBJ, value);
		return this;
	}

	public ScenarioFileBuilder withWallclockLimit(final String value) {
		this.map.put(K_WALLCLOCK_LIMIT, value);
		return this;
	}

	public ScenarioFileBuilder withSharedModel(final String value) {
		this.map.put(K_SHARED_MODEL, value);
		return this;
	}

	public ScenarioFileBuilder withAbortOnFirstRunCrash(final String value) {
		this.map.put(K_ABORT_ON_FIRST_RUN_CRASH, value);
		return this;
	}

	public ScenarioFileBuilder withInputPsmacDirs(final String value) {
		this.map.put(K_INPUT_PSMAC_DIRS, value);
		return this;
	}

	public ScenarioFileBuilder withDefaultInputPsmacDirs() {
		this.map.put(K_INPUT_PSMAC_DIRS, DEF_INPUT_PSMAC_DIRS);
		return this;
	}

	public ScenarioFileBuilder withDeterministic(final String value) {
		this.map.put(K_DETERMINISTIC, value);
		return this;
	}

	public void toScenarioFile(final File file) throws IOException {
		this.checkRequiredConfigs();

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
			bw.write(this.toString());
		}
	}

	private void checkRequiredConfigs() {
		if (!this.map.containsKey(K_ALGO) || !this.map.containsKey(K_PCS_FILE)) {
			throw new IllegalStateException("Cannot write scenario file if there is not all required configurations given.");
		}
	}

	@Override
	public String toString() {
		return this.map.entrySet().stream().map(x -> x.getKey() + "=" + x.getValue()).collect(Collectors.joining("\n"));
	}

}

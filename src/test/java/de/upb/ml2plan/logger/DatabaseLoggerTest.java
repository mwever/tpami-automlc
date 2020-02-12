package de.upb.ml2plan.logger;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.BeforeClass;
import org.junit.Test;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.model.ComponentUtil;
import ai.libs.hasco.serialization.ComponentLoader;
import de.upb.ml2plan.event.CandidateEvaluatedEvent;
import de.upb.ml2plan.event.CandidateEvaluatedEventImpl;

public class DatabaseLoggerTest {

	private static DatabaseLogger logger;

	@BeforeClass
	public static void setup() throws Exception {
		Map<String, Object> defaults = new HashMap<>();
		defaults.put("experiment_id", "-1");
		logger = new DatabaseLogger(defaults);
	}

	@Test
	public void testRcvCandidateEvaluatedEvent() throws IOException, SQLException {
		ComponentLoader cl = new ComponentLoader(new File("testrsc/tinytest.json"));
		ComponentInstance ci = ComponentUtil.randomParameterizationOfComponent(cl.getComponentWithName("weka.classifiers.lazy.KStar"), new Random(42));

		Map<String, Object> evaluationReport = new HashMap<>();
		evaluationReport.put("testRuntime", 120);
		evaluationReport.put("testTraintime", 12392);
		evaluationReport.put("accuracy", 0.975);

		CandidateEvaluatedEvent evalEvent = new CandidateEvaluatedEventImpl(Thread.currentThread().getName(), ci, -1, evaluationReport);

		logger.rcvCandidateEvaluatedEvent(evalEvent);
	}

}

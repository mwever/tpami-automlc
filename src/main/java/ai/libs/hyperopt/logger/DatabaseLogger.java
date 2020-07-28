package ai.libs.hyperopt.logger;

import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;

import ai.libs.hyperopt.api.logger.IDatabaseLoggerConfig;
import ai.libs.hyperopt.api.output.IOptimizationSolutionCandidateFoundEvent;
import ai.libs.hyperopt.util.ComponentInstanceAdapter;
import ai.libs.hyperopt.util.DBSchemaUtil;
import ai.libs.jaicore.components.model.ComponentInstance;
import ai.libs.jaicore.db.sql.SQLAdapter;

public class DatabaseLogger<M> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseLogger.class);

	private static final File CONFIG_FILE = new File("automlc-setup.properties");
	private static final IDatabaseLoggerConfig DB_CONFIG = (IDatabaseLoggerConfig) ConfigFactory.create(IDatabaseLoggerConfig.class).loadPropertiesFromFile(CONFIG_FILE);
	private static final Class<SCandidateEvaluatedSchema> SCHEMA = SCandidateEvaluatedSchema.class;

	private final SQLAdapter adapter;
	private final Map<String, Object> defaultValues;

	private final AtomicInteger orderNo = new AtomicInteger(0);

	public DatabaseLogger(final Map<String, Object> defaultValues) throws Exception {
		this.adapter = new SQLAdapter(DB_CONFIG);
		this.defaultValues = defaultValues;

		// check whether table exists and has the right format.
		this.adapter.query(DBSchemaUtil.generateCreateTableStatement(SCHEMA, DB_CONFIG.getLogTable()));
		List<String> columnsTable = this.adapter.query("SHOW COLUMNS FROM " + DB_CONFIG.getLogTable()).stream().map(x -> x.getAsString("Field")).collect(Collectors.toList());
		String checkAndExtendTable = DBSchemaUtil.checkAndExtendTable(SCHEMA, DB_CONFIG.getLogTable(), columnsTable);
		if (checkAndExtendTable != null) {
			this.adapter.query(checkAndExtendTable);
		}
	}

	@Subscribe
	public void rcvCandidateEvaluatedEvent(final IOptimizationSolutionCandidateFoundEvent<M> e) throws JsonProcessingException, SQLException {
		Map<String, Object> keyValuePairs = new HashMap<>(this.defaultValues);
		keyValuePairs.put(SCandidateEvaluatedSchema.THREAD_ID.getName(), e.getAlgorithmId());
		keyValuePairs.put(SCandidateEvaluatedSchema.COMPONENT_INSTANCE.getName(), new ComponentInstanceAdapter().componentInstanceToString(e.getSolutionCandidate()));

		if (e.getEvaluationReport() != null && !e.getEvaluationReport().isEmpty()) {
			Map<String, Object> mapToWrite = new HashMap<>();
			for (Entry<String, ? extends Object> entry : e.getEvaluationReport().entrySet()) {
				if (entry.getValue() instanceof DescriptiveStatistics) { // transform descriptive statistics appropriately
					DescriptiveStatistics stats = (DescriptiveStatistics) entry.getValue();
					mapToWrite.put(entry.getKey() + "_min", stats.getMin());
					mapToWrite.put(entry.getKey() + "_max", stats.getMax());
					mapToWrite.put(entry.getKey() + "_mean", stats.getMean());
					mapToWrite.put(entry.getKey() + "_median", stats.getGeometricMean());
					mapToWrite.put(entry.getKey() + "_n", stats.getN());
				} else {
					mapToWrite.put(entry.getKey(), entry.getValue());
				}
			}
			keyValuePairs.put(SCandidateEvaluatedSchema.EVALUATION_REPORT.getName(), new ObjectMapper().writeValueAsString(mapToWrite));
			keyValuePairs.put(SCandidateEvaluatedSchema.EVAL_VALUE.getName(), ((DescriptiveStatistics) e.getEvaluationReport().get(this.defaultValues.get(SCandidateEvaluatedSchema.MEASURE.getName()))).getMean());
		}

		if (e.getException() != null) {
			keyValuePairs.put(SCandidateEvaluatedSchema.EXCEPTION.getName(), e.getException());
		}

		keyValuePairs.put(SCandidateEvaluatedSchema.TIME_UNTIL_FOUND.getName(), e.getTimeUntilFound());
		keyValuePairs.put(SCandidateEvaluatedSchema.TIMESTAMP_FOUND.getName(), e.getTimestamp());

		StringBuilder abstractDescription = new StringBuilder();
		List<ComponentInstance> ciList = new LinkedList<>(Arrays.asList(e.getSolutionCandidate()));
		while (!ciList.isEmpty()) {
			ComponentInstance ci = ciList.remove(0);
			abstractDescription.append(ci.getComponent().getName()).append("(");
			abstractDescription.append(ci.getParameterValues().entrySet().stream().map(x -> x.getKey() + "=" + x.getValue()).collect(Collectors.joining(", ")));
			abstractDescription.append(")").append(" - ");
			ciList.addAll(ci.getSatisfactionOfRequiredInterfaces().values());
		}

		keyValuePairs.put(SCandidateEvaluatedSchema.ABSTRACT_DESCRIPTION.getName(), abstractDescription.toString());

		keyValuePairs.put(SCandidateEvaluatedSchema.ORDER_NO.getName(), this.orderNo.getAndIncrement());
		this.adapter.insert(DB_CONFIG.getLogTable(), keyValuePairs);
	}

}

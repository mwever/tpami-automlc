package de.upb.ml2plan.logger;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.aeonbits.owner.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;

import ai.libs.hasco.gui.statsplugin.ComponentInstanceSerializer;
import ai.libs.jaicore.db.sql.SQLAdapter;
import de.upb.ml2plan.event.CandidateEvaluatedEvent;

public class DatabaseLogger {

	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseLogger.class);

	private static final File CONFIG_FILE = new File("benchmark.properties");
	private static final IDatabaseLoggerConfig DB_CONFIG = (IDatabaseLoggerConfig) ConfigFactory.create(IDatabaseLoggerConfig.class).loadPropertiesFromFile(CONFIG_FILE);
	private static final Class<SCandidateEvaluatedSchema> SCHEMA = SCandidateEvaluatedSchema.class;

	private final SQLAdapter adapter;
	private final Map<String, Object> defaultValues;

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
	public void rcvCandidateEvaluatedEvent(final CandidateEvaluatedEvent e) throws JsonProcessingException, SQLException {
		Map<String, Object> keyValuePairs = new HashMap<>(this.defaultValues);
		keyValuePairs.put(SCandidateEvaluatedSchema.THREAD_ID.getName(), e.getThreadID());
		keyValuePairs.put(SCandidateEvaluatedSchema.COMPONENT_INSTANCE.getName(), new ComponentInstanceSerializer().serializeComponentInstance(e.getComponentInstance()));

		if (e.getEvaluationReport() != null) {
			keyValuePairs.put(SCandidateEvaluatedSchema.EVALUATION_REPORT.getName(), new ObjectMapper().writeValueAsString(e.getEvaluationReport()));
		}
		if (e.getException() != null) {
			keyValuePairs.put(SCandidateEvaluatedSchema.EXCEPTION.getName(), e.getException());
		}

		keyValuePairs.put(SCandidateEvaluatedSchema.ORDER_NO.getName(), e.getOrderNo());
		this.adapter.insert(DB_CONFIG.getLogTable(), keyValuePairs);
	}

}

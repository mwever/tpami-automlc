package ai.libs.hyperopt.experimenter.test;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;

import ai.libs.jaicore.components.model.ComponentInstance;
import benchmark.core.api.output.IOptimizationSolutionCandidateFoundEvent;
import benchmark.core.logger.DatabaseLogger;
import benchmark.core.logger.SCandidateEvaluatedSchema;
import benchmark.core.util.ComponentInstanceAdapter;

public class CommandLineLogger<M> extends DatabaseLogger<M> {
	private final Map<String, Object> defaultValues;
	private final AtomicInteger orderNo = new AtomicInteger(0);

	public CommandLineLogger(final Map<String, Object> defaultValues) throws Exception {
		this.defaultValues = defaultValues;
	}

	@Override
	@Subscribe
	public void rcvCandidateEvaluatedEvent(final IOptimizationSolutionCandidateFoundEvent<M> e)
			throws JsonProcessingException, SQLException {
		Map<String, Object> keyValuePairs = new HashMap<>(this.defaultValues);
		keyValuePairs.put(SCandidateEvaluatedSchema.THREAD_ID.getName(), e.getAlgorithmId());
		keyValuePairs.put(SCandidateEvaluatedSchema.COMPONENT_INSTANCE.getName(),
				new ComponentInstanceAdapter().componentInstanceToString(e.getSolutionCandidate()));

		if (e.getEvaluationReport() != null && !e.getEvaluationReport().isEmpty()) {
			Map<String, Object> mapToWrite = new HashMap<>();
			for (Entry<String, ? extends Object> entry : e.getEvaluationReport().entrySet()) {
				if (entry.getValue() instanceof DescriptiveStatistics) { // transform descriptive statistics
																			// appropriately
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
			keyValuePairs.put(SCandidateEvaluatedSchema.EVALUATION_REPORT.getName(),
					new ObjectMapper().writeValueAsString(mapToWrite));
			keyValuePairs
					.put(SCandidateEvaluatedSchema.EVAL_VALUE.getName(),
							((DescriptiveStatistics) e.getEvaluationReport()
									.get(this.defaultValues.get(SCandidateEvaluatedSchema.MEASURE.getName())))
									.getMean());
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
			abstractDescription.append(ci.getParameterValues().entrySet().stream()
					.map(x -> x.getKey() + "=" + x.getValue()).collect(Collectors.joining(", ")));
			abstractDescription.append(")").append(" - ");
			ciList.addAll(ci.getSatisfactionOfRequiredInterfaces().values());
		}

		keyValuePairs.put(SCandidateEvaluatedSchema.ABSTRACT_DESCRIPTION.getName(), abstractDescription.toString());

		keyValuePairs.put(SCandidateEvaluatedSchema.ORDER_NO.getName(), this.orderNo.getAndIncrement());
		System.out.println(keyValuePairs);
	}

}

package benchmark.core.logger;

import benchmark.core.api.logger.ETableKey;
import benchmark.core.api.logger.IDBSchema;

public enum SCandidateEvaluatedSchema implements IDBSchema {

	/* The unique auto-increment ID of the evaluated candidate */
	ID("int-10", ETableKey.AUTO_INCREMENT_PRIMARY),
	/* The unique ID of the experiment connected to this evaluation */
	EXPERIMENT_ID("int-10"),
	/* The id of the thread having evaluated this candidate. */
	THREAD_ID("varchar-255"),
	/* The component instance description of the candidate evaluated. */
	COMPONENT_INSTANCE("text"),
	/* The component instance description of the candidate evaluated. */
	MEASURE("varchar-255"),
	/* The evaluation report for this candidate. */
	EVALUATION_REPORT("json"),
	/* Field for logging exception stack traces (if any exception occurred while evaluating the candidate). */
	EXCEPTION("text"),
	/* Abstract description of a component containing only the different levels of components. */
	ABSTRACT_DESCRIPTION("text"),
	/* Time passed until this candidate was found. */
	TIME_UNTIL_FOUND("varchar-25"),
	/* Timestamp when this candidate was found. */
	TIMESTAMP_FOUND("varchar-25"),
	/* Value of the performance measure used for searching. */
	EVAL_VALUE("varchar-255"),
	/* Order number of the evaluation run. */
	ORDER_NO("int-10");

	private final String type;
	private final ETableKey keyType;

	private SCandidateEvaluatedSchema(final String type, final ETableKey autoKey) {
		this.type = type;
		this.keyType = autoKey;
	}

	private SCandidateEvaluatedSchema(final String type) {
		this(type, ETableKey.NONE);
	}

	@Override
	public String getName() {
		return this.name().toLowerCase();
	}

	@Override
	public String getType() {
		return this.type;
	}

	@Override
	public ETableKey getKeyType() {
		return this.keyType;
	}

}

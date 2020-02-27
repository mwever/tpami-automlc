package de.upb.ml2plan.logger;

public enum SCandidateEvaluatedSchema implements IDBSchema {

	/* The unique auto-increment ID of the evaluated candidate */
	ID("int-10", TableKey.AUTO_INCREMENT_PRIMARY),
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
	/* Order number of the evaluation run. */
	ORDER_NO("int-10");

	private final String type;
	private final TableKey keyType;

	private SCandidateEvaluatedSchema(final String type, final TableKey autoKey) {
		this.type = type;
		this.keyType = autoKey;
	}

	private SCandidateEvaluatedSchema(final String type) {
		this(type, TableKey.NONE);
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
	public TableKey getKeyType() {
		return this.keyType;
	}

}

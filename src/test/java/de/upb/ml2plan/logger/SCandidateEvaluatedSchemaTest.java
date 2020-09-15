package de.upb.ml2plan.logger;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import benchmark.core.logger.SCandidateEvaluatedSchema;
import benchmark.core.util.DBSchemaUtil;

public class SCandidateEvaluatedSchemaTest {

	@Test
	public void testGetName() {
		assertEquals("The name of the enum field was not as expected.", "id", SCandidateEvaluatedSchema.ID.getName());
	}

	@Test
	public void testGetCreateTableStatement() {
		String statement = DBSchemaUtil.generateCreateTableStatement(SCandidateEvaluatedSchema.class, "testTable");
		System.out.println(statement);
	}

	@Test
	public void testCheckAndExtendTable() {
		List<String> addColumns = Arrays.asList("exception", "order_no");
		List<String> alreadyExistingColumns = Arrays.stream(SCandidateEvaluatedSchema.values()).filter(x -> !addColumns.contains(x.getName())).map(x -> x.getName()).collect(Collectors.toList());
		String statement = DBSchemaUtil.checkAndExtendTable(SCandidateEvaluatedSchema.class, "testTable", alreadyExistingColumns);
		System.out.println(statement);
	}

}

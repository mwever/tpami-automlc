package results;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import ai.libs.jaicore.basic.kvstore.KVStoreCollection;
import ai.libs.jaicore.basic.kvstore.KVStoreCollection.EGroupMethod;
import ai.libs.jaicore.basic.kvstore.KVStoreUtil;
import ai.libs.jaicore.db.sql.SQLAdapter;

public class ResultTable {

	private static final String[] measures = { "FMacroAvgD", "FMacroAvgL", "FMicroAvg" };

	public static void main(final String[] args) throws SQLException {
		SQLAdapter adapter = new SQLAdapter("isys-otfml.cs.upb.de", "results", "Hallo333!", "conference_tpami_mlc");

		KVStoreCollection col = KVStoreUtil.readFromMySQLQuery(adapter, "SELECT * FROM automlc_clusterjobs WHERE finalScore IS NOT NULL && measure='" + measures[0] + "'", new HashMap<>());

		Map<String, EGroupMethod> grouping = new HashMap<>();
		grouping.put("finalScore", EGroupMethod.AVG);
		KVStoreCollection grouped = col.group(new String[] { "algorithm", "measure", "dataset" }, grouping);

		String latexTable = KVStoreUtil.kvStoreCollectionToLaTeXTable(grouped, "dataset", "algorithm", "finalScore");

		System.out.println(latexTable);

	}

}

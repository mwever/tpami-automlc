package ai.libs.hyperopt.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import ai.libs.hyperopt.api.logger.ETableKey;
import ai.libs.hyperopt.api.logger.IDBSchema;
import ai.libs.jaicore.basic.sets.SetUtil;

public class DBSchemaUtil {

	@SuppressWarnings("unchecked")
	public static <V extends IDBSchema> String generateCreateTableStatement(final Class<V> schemaClass, final String tableName) {
		StringBuilder sb = new StringBuilder();

		try {
			V[] values = (V[]) schemaClass.getMethod("values").invoke(null);
			List<V> keys = new LinkedList<>();
			sb.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");

			sb.append(Arrays.stream(values).map(DBSchemaUtil::schemaEntryToSQL).collect(Collectors.joining(",")));
			Arrays.stream(values).filter(x -> x.getKeyType() != ETableKey.NONE).forEach(keys::add);

			if (!keys.isEmpty()) {
				sb.append(", PRIMARY KEY (").append(keys.stream().map(x -> x.getName()).collect(Collectors.joining(","))).append(")");
			}

			sb.append(")");
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	private static String schemaEntryToSQL(final IDBSchema schemaEntry) {
		StringBuilder isb = new StringBuilder();
		isb.append(schemaEntry.getName()).append(" ");

		Integer length = null;
		String type = schemaEntry.getType();
		if (type.contains("-")) {
			length = Integer.parseInt(type.split("-")[1]);
			type = type.split("-")[0];
		}

		switch (type.toLowerCase()) {
		case "json":
			isb.append("JSON");
			break;
		case "text":
			isb.append("TEXT");
			break;
		case "varchar":
			isb.append("VARCHAR(").append(length).append(")");
			break;
		case "int":
			isb.append("INT(").append(length).append(")");
			break;
		default:
			isb.append("VARCHAR(500)");
			break;
		}

		if (schemaEntry.getKeyType() != ETableKey.NONE) {
			if (schemaEntry.getKeyType() == ETableKey.AUTO_INCREMENT_PRIMARY) {
				isb.append(" AUTO_INCREMENT");
			}
		}

		return isb.toString();
	}

	@SuppressWarnings("unchecked")
	public static <V extends IDBSchema> String checkAndExtendTable(final Class<V> schemaClass, final String tableName, final List<String> existingColumns) {
		StringBuilder sb = new StringBuilder();
		try {
			List<String> columns = Arrays.stream((V[]) schemaClass.getMethod("values").invoke(null)).map(x -> x.getName()).collect(Collectors.toList());
			List<String> notYetExistingColumns = SetUtil.difference(columns, existingColumns.stream().map(x -> x.toLowerCase()).collect(Collectors.toList()));
			if (notYetExistingColumns.isEmpty()) {
				return null;
			}

			sb.append("ALTER TABLE ").append(tableName);
			sb.append(Arrays.stream((V[]) schemaClass.getMethod("values").invoke(null)).filter(x -> notYetExistingColumns.contains(x.getName())).map(DBSchemaUtil::schemaEntryToSQL).map(x -> new String(" ADD COLUMN ").concat(x))
					.collect(Collectors.joining(",")));

			System.out.println("Extend table by columns " + SetUtil.implode(notYetExistingColumns, ", "));
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

}

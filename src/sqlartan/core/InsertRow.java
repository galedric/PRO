package sqlartan.core;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class InsertRow {
	private Table table;
	private List<Object> data = new ArrayList<>();

	InsertRow(Table table) {
		this.table = table;
	}

	public InsertRow add(Object label) {
		data.add(label);
		return this;
	}

	public InsertRow add(int index, Object label) {
		while (data.size() <= index) {
			data.add(null);
		}
		data.add(index, label);
		return this;
	}

	public Result execute() throws SQLException {
		int cardinality = data.size();

		String phs = String.join(", ", (Iterable<String>) Stream.generate(() -> "?").limit(cardinality)::iterator);
		PreparedQuery query = table.database.assemble("INSERT INTO ", table.fullName(), " VALUES (" + phs + ")")
		                                    .prepare();

		for (int i = 0; i < cardinality; i++) {
			query.set(i, data.get(i));
		}

		data.clear();
		return query.execute();
	}
}

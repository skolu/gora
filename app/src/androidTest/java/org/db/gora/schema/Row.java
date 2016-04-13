package org.db.gora.schema;

public class Row {
	@SqlColumn(name="id", pk=true)
	long id;

	public long getId() {
		return id;
	}
}

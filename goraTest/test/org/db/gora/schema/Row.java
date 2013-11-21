package org.db.gora.schema;

import org.db.gora.schema.SqlColumn;

public class Row {
	@SqlColumn(name="id", pk=true, getter="getId", setter="setId")
	long id;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
}

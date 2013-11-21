package org.db.gora.fom;

import org.db.gora.schema.SqlColumn;

public class FomRow {
	@SqlColumn(name="id", pk=true, getter="getId", setter="setId")
	long id;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
}

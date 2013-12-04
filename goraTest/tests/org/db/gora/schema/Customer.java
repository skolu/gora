package org.db.gora.schema;

import org.db.gora.SqliteEvent;

@SqlTable(name="Customer")
public class Customer extends Entity {
	@SqlColumn(name="first_name")
	public String firstName;

	@SqlColumn(name="last_name")
	public String lastName;

}

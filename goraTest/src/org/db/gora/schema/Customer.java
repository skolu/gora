package org.db.gora.schema;

@SqlTable(name="Customer")
public class Customer extends Entity {
	@SqlColumn(name="first_name")
	public String firstName;

	@SqlColumn(name="last_name")
	public String lastName;

}

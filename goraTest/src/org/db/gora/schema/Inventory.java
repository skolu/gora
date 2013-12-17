package org.db.gora.schema;

@SqlTable(name="Inventory")
public class Inventory extends Entity {
	@SqlColumn(name="desc")
	public String desc;
	
	@SqlColumn(name="price")
	public double price;

	@SqlColumn(name="taxable")
	public boolean taxable;
}

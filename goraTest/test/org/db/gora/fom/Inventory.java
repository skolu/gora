package org.db.gora.fom;

import org.db.gora.schema.SqlColumn;
import org.db.gora.schema.SqlTable;

@SqlTable(name="Inventory")
public class Inventory extends FomEntity {
	@SqlColumn(name="desc")
	public String desc;
	
	@SqlColumn(name="price")
	public double price;

	@SqlColumn(name="taxable")
	public boolean taxable;
}

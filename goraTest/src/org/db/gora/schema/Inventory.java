package org.db.gora.schema;

@SqlTable(name="Inventory")
public class Inventory extends Entity {
    @SqlColumn(name="item_no")
    public int itemNo;

	@SqlColumn(name="desc")
	public String desc;
	
	@SqlColumn(name="price")
	public double price;

	@SqlColumn(name="taxable")
	public boolean taxable;

    @SqlColumn(name="image")
    public byte[] image;
}

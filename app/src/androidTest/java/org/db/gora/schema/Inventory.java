package org.db.gora.schema;

import org.db.gora.EntityKeyword;

import java.util.Locale;

@SqlTable(name="Inventory")
public class Inventory extends Entity implements EntityKeyword {
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

    @Override
    public String getKeywords() {
        return String.format(Locale.getDefault(), "%s %s %d", name, desc != null ? desc : "", itemNo);
    }
}

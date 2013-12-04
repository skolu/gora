package org.db.gora.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.db.gora.WhenLinkBroken;

@SqlTable(name="Invoice")
public class Invoice extends Entity {
	@SqlChild
	public ArrayList<InvoiceItem> items;
	
	@SqlChild(clazz=InvoicePayment.class, getter="getPayments")
	private List<?> payments;
	
	public List<?> getPayments() {
		if (payments == null) {
			payments = new ArrayList<Invoice.InvoicePayment>();
		}
		return payments;
	}

	@SqlColumn(name="customer_id")
	@SqlLinkedEntity(entity=Customer.class, whenBroken=WhenLinkBroken.UNLINK) 
	public long customerId;

	@SqlTable(name="InvoiceItemAttr")
	public static class InvoiceItemAttribute extends Row {
		@SqlColumn(name="invoice_item_id", fk=true)
		public long invoiceItemId;
		
		@SqlColumn(name="attribute") 
		public String attribute;
		
	}
	
	@SqlTable(name="InvoiceItem")
	public static class InvoiceItem extends Row {
		@SqlColumn(name="invoice_id", fk=true)
		public long invoiceId;
		
		@SqlColumn(name="desc")
		public String desc;
		
		@SqlColumn(name="price")
		public double price;

		@SqlColumn(name="taxable")
		public boolean taxable;

		@SqlColumn(name="qty")
		public double qty;
		
		@SqlColumn(name="invn_id")
		@SqlLinkedEntity(entity=Inventory.class, whenBroken=WhenLinkBroken.UNLINK)
		public long invn_id;
		
		@SqlChild
		public TreeSet<InvoiceItemAttribute> attributes;
		
		public double getExtendedPrice() {
			return qty * price;
		}
	}

	@SqlTable(name="InvoicePayment")
	public static class InvoicePayment extends Row {
		@SqlColumn(name="invoice_id", fk=true)
		public long invoiceId;
		
		@SqlColumn(name="amount")
		public double amount;
	}
}

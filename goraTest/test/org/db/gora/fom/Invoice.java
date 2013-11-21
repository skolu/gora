package org.db.gora.fom;

import java.util.ArrayList;
import java.util.List;

import org.db.gora.schema.SqlChild;
import org.db.gora.schema.SqlColumn;
import org.db.gora.schema.SqlLinkedEntity;
import org.db.gora.schema.WhenLinkBroken;

public class Invoice extends FomEntity {
	@SqlChild
	public ArrayList<InvoiceItem> items;
	
	@SqlChild(clazz=InvoicePayment.class)
	public List<?> getPayments() {
		if (sPayments == null) {
			sPayments = new ArrayList<Invoice.InvoicePayment>();
		}
		return sPayments;
	}
	private ArrayList<InvoicePayment> sPayments;

	@SqlColumn(name="customer_id")
	@SqlLinkedEntity(clazz=Customer.class, whenBroken=WhenLinkBroken.UNLINK) 
	public long customerId;
	
	public static class InvoiceItem extends Inventory {
		@SqlColumn(name="parent_id", fk=true)
		public long parentId;
		
		@SqlColumn(name="qty")
		public double qty;
		
		public double getExtendedPrice() {
			return qty * price;
		}
	}

	public static class InvoicePayment extends FomRow {
		@SqlColumn(name="parent_id", fk=true)
		public long parentId;
	}
}

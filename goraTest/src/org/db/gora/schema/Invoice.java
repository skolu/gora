package org.db.gora.schema;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.db.gora.WhenLinkBroken;

@SqlTable(name="Invoice")
public class Invoice extends Entity {
	@SqlChild
	public ArrayList<InvoiceItem> items;
	
	@SqlChild(classes={InvoiceCashPayment.class, InvoiceCreditPayment.class}, getter="getPayments")
	private Set<? extends InvoicePayment> payments;
	
	public Set<? extends InvoicePayment> getPayments() {
		if (payments == null) {
			payments = new HashSet<InvoicePayment>();
		}
		return payments;
	}

    @SqlChild
    public InvoiceCustomer customer;

    @SqlTable(name="InvoiceCustomer")
    public static class InvoiceCustomer extends Row {
        public InvoiceCustomer() {
        }

        public InvoiceCustomer(Customer customer) {
            this();
            this.firstName = customer.firstName;
            this.fullName = customer.name;
            this.lastName = customer.lastName;
            this.customerId = customer.getId();
        }

        @SqlColumn(name="invoice_id", fk=true)
        public long invoiceId;

        @SqlColumn(name="full_name")
        public String fullName;

        @SqlColumn(name="first_name")
        public String firstName;

        @SqlColumn(name="last_name")
        public String lastName;

        @SqlColumn(name="customer_id")
        @SqlLinkedEntity(entity=Customer.class, whenBroken=WhenLinkBroken.UNLINK)
        public long customerId;
    }


	@SqlTable(name="InvoiceItemAttr")
	public static class InvoiceItemAttribute extends Row {
		@SqlColumn(name="invoice_item_id", fk=true)
		public long invoiceItemId;
		
		@SqlColumn(name="attribute") 
		public String attribute;
		
	}
	
	@SqlTable(name="InvoiceItem")
	public static class InvoiceItem extends Row {
        public InvoiceItem() {
        }

        public InvoiceItem(Inventory item) {
            this();
            if (item != null) {
                this.name = item.name;
                this.desc = item.desc;
                this.price = item.price;
                this.taxable = item.taxable;
                this.invn_id = item.getId();
            }
        }

		@SqlColumn(name="invoice_id", fk=true)
		public long invoiceId;
		
        @SqlColumn(name="name")
      		public String name;

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

	public static class InvoicePayment extends Row {
		@SqlColumn(name="invoice_id", fk=true)
		public long invoiceId;
		
		@SqlColumn(name="amount")
		public double amount;
    }

    @SqlTable(name="InvoiceCashPayment")
    public static class InvoiceCashPayment extends InvoicePayment {
        @SqlColumn(name="cash_taken")
        public double cashTaken;
    }

    @SqlTable(name="InvoiceCreditPayment")
    public static class InvoiceCreditPayment extends InvoicePayment {
        @SqlColumn(name="auth_id")
        public String authId;

        @SqlColumn(name="last_four")
        public String lastFourDigits;
    }

}

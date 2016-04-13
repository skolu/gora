package org.db.gora.schema;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.db.gora.WhenLinkBroken;

@SqlTable(name="Invoice")
public class Invoice extends Entity {
    @SqlChild
    public ArrayList<InvoiceItem> items;

    @SqlChild(classes={InvoiceCashPayment.class, InvoiceCreditPayment.class}, getter="getPayments")
    private Set<InvoicePayment> payments;

    public Set<InvoicePayment> getPayments() {
        if (payments == null) {
            payments = new HashSet<InvoicePayment>();
        }
        return payments;
    }

    @SqlChild
    public InvoiceCustomer customer;

    @SqlColumn(name="customer_id")
    @SqlLinkedEntity(entity=Customer.class, whenBroken=WhenLinkBroken.UNLINK)
    public long customerId;

    @SqlTable(name="InvoiceItemAttr")
    public static class InvoiceItemAttribute extends Row {
        @SqlColumn(name="invoice_item_id", fk=true)
        public long invoiceItemId;

        @SqlColumn(name="name")
        public String name;

        @SqlColumn(name="value")
        public String value;
    }

    @SqlTable(name="InvoiceItem")
    public static class InvoiceItem extends Row {
        public InvoiceItem() { }

        public InvoiceItem(Inventory item) {
            this();
            if (item != null) {
                this.itemNo = item.itemNo;
                this.name = item.name;
                this.desc = item.desc;
                this.price = item.price;
                this.taxable = item.taxable;
                this.invn_id = item.getId();
            }
        }

        @SqlColumn(name="invoice_id", fk=true)
        public long invoiceId;

        @SqlColumn(name="item_no", getter="getItemNo", setter="setItemNo")
        int itemNo;
        public int getItemNo() { return itemNo; }
        public void setItemNo(int itemNo) { this.itemNo = itemNo; }

        @SqlColumn(name="name", getter="getName", setter="setName")
        String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        @SqlColumn(name="desc")
        public String desc;

        @SqlColumn(name="price")
        public double price;

        @SqlColumn(name="taxable")
        public boolean taxable;

        @SqlColumn(name="qty", getter="getQty", setter="setQty")
        float qty;
        public float getQty() { return qty; }
        public void setQty(float qty) { this.qty = qty; }

        @SqlColumn(name="invn_id")
        @SqlLinkedEntity(entity=Inventory.class, whenBroken=WhenLinkBroken.UNLINK)
        public long invn_id;

        @SqlChild
        public List<InvoiceItemAttribute> attributes;

        public double getExtendedPrice() {
            return qty * price;
        }
    }
}

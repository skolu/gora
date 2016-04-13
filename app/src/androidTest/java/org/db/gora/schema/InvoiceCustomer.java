package org.db.gora.schema;

@SqlTable(name="InvoiceCustomer")
public class InvoiceCustomer extends Row {
    public InvoiceCustomer() {
    }

    public InvoiceCustomer(Customer customer) {
        this();
        this.firstName = customer.firstName;
        this.fullName = customer.name;
        this.lastName = customer.getLastName();
    }

    @SqlColumn(name="invoice_id", fk=true)
    public long invoiceId;

    @SqlColumn(name="full_name")
    public String fullName;

    @SqlColumn(name="first_name")
    public String firstName;

    @SqlColumn(name="last_name")
    public String lastName;
}



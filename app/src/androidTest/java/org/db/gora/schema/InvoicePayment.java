package org.db.gora.schema;

public abstract class InvoicePayment extends Row {
	@SqlColumn(name="invoice_id", fk=true)
	public long invoiceId;

	@SqlColumn(name="amount")
	public double amount;

    public abstract PaymentType getPaymentType();
}


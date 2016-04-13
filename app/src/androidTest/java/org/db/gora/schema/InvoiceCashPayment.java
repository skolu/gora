package org.db.gora.schema;

@SqlTable(name="InvoiceCashPayment")
public final class InvoiceCashPayment extends InvoicePayment {

    @SqlColumn(name="cash_taken")
    public double cashTaken;

    @Override
    public PaymentType getPaymentType() {
        return PaymentType.Cash;
    }
}

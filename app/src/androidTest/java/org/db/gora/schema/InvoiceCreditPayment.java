package org.db.gora.schema;

@SqlTable(name="InvoiceCreditPayment")
public class InvoiceCreditPayment extends InvoicePayment {

    @SqlColumn(name="auth_id")
    public String authId;

    @SqlColumn(name="last_four")
    public String lastFourDigits;

    @SqlColumn(name="name_on_card")
    public String nameOnCard;

    @Override
    public PaymentType getPaymentType() {
        return PaymentType.Credit;
    }
}

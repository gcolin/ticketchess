package com.github.gcolin.payment;

import com.github.gcolin.platform.AbstractMail;

public class PaymentReceiptMail extends AbstractMail {

    private String name;
    private String documentLabel;
    private String amount;
    private String reference;
    private boolean donation;

    public PaymentReceiptMail() {
        setTemplate("payment/paymentReceipt");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDocumentLabel() {
        return documentLabel;
    }

    public void setDocumentLabel(String documentLabel) {
        this.documentLabel = documentLabel;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public boolean isDonation() {
        return donation;
    }

    public void setDonation(boolean donation) {
        this.donation = donation;
    }
}

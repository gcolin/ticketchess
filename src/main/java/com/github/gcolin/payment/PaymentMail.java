package com.github.gcolin.payment;

import com.github.gcolin.platform.AbstractMail;
public class PaymentMail extends AbstractMail {
    private String name;
    private String eventName;
    private String evenDate;
    private String amount;
    private String reference;
    private String loginUrl;

    public PaymentMail() {
        setTemplate("payment/paymentConfirmation");
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getEvenDate() {
        return evenDate;
    }

    public void setEvenDate(String evenDate) {
        this.evenDate = evenDate;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }
}

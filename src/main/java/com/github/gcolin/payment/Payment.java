package com.github.gcolin.payment;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.util.List;
import com.github.gcolin.membership.Membership;
import com.github.gcolin.platform.TimedEntity;
import com.github.gcolin.registration.PlayerSubscription;

@Entity
@Table(name = "payment")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Payment extends TimedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String stripeSessionId;

    private String stripeIntent;

    @Column(nullable = false)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status; // PENDING, PAID, EXPIRED

    @Enumerated(EnumType.STRING)
    private PaymentType type; // CARD, BANK_TRANSFER, FREE

    private Double amount;

    private Long amountCents;

    @OneToMany
    @JoinColumn(name = "payment_id")
    @XmlTransient
    @JsonbTransient
    private List<PlayerSubscription> subscriptions;

    @OneToMany
    @JoinColumn(name = "payment_id")
    @XmlTransient
    @JsonbTransient
    private List<Membership> memberships;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStripeSessionId() {
        return stripeSessionId;
    }

    public void setStripeSessionId(String stripeSessionId) {
        this.stripeSessionId = stripeSessionId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public PaymentType getType() {
        return type;
    }

    public void setType(PaymentType type) {
        this.type = type;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
        this.amountCents = amount == null ? null : (long) (amount * 100);
    }

    public Long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(Long amountCents) {
        this.amountCents = amountCents;
    }

    public List<PlayerSubscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<PlayerSubscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public List<Membership> getMemberships() {
        return memberships;
    }

    public void setMemberships(List<Membership> memberships) {
        this.memberships = memberships;
    }

    @Override
    public String toString() {
        return "Payment [id="
                + id
                + ", stripeSessionId="
                + stripeSessionId
                + ", userEmail="
                + userEmail
                + ", status="
                + status
                + ", type="
                + type
                + ", amount="
                + amount
                + ", amountCents="
                + amountCents
                + ", subscriptions="
                + subscriptions
                + "]";
    }

    public String getStripeIntent() {
        return stripeIntent;
    }

    public void setStripeIntent(String stripeIntent) {
        this.stripeIntent = stripeIntent;
    }
}

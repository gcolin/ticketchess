package com.github.gcolin.registration;

import com.github.gcolin.registration.PlayerSubscriptionOptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import com.github.gcolin.payment.Payment;
import com.github.gcolin.platform.TimedEntity;

@Entity
@Table(name = "player_subscription_option")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class PlayerSubscriptionOption extends TimedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_subscription_id", nullable = false)
    @XmlTransient
    private PlayerSubscription playerSubscription;

    @Column(name = "amountcents")
    private Long amountCents;

    @Column(name = "description", length = 512)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    @XmlTransient
    private Payment payment;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private PlayerSubscriptionOptionStatus status;

    public PlayerSubscriptionOption() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public PlayerSubscription getPlayerSubscription() {
        return playerSubscription;
    }

    public void setPlayerSubscription(PlayerSubscription playerSubscription) {
        this.playerSubscription = playerSubscription;
    }

    public Long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(Long amountCents) {
        this.amountCents = amountCents;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public PlayerSubscriptionOptionStatus getStatus() {
        return status;
    }

    public void setStatus(PlayerSubscriptionOptionStatus status) {
        this.status = status;
    }
}

package com.github.gcolin.registration;

import com.github.gcolin.player.DisplayPlayer;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Index;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.time.LocalDateTime;
import com.github.gcolin.event.Event;
import com.github.gcolin.payment.Payment;
import com.github.gcolin.platform.TimedEntity;

@Entity
@Table(
        name = "player_subscription",
        indexes = {
            @Index(name = "idx_player_subscription_event_id", columnList = "event_id"),
            @Index(name = "idx_player_subscription_creation_user", columnList = "creation_user")
        })
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class PlayerSubscription extends TimedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nr_ffe", length = 255)
    private String nrFfe;

    @Column(name = "creation_user", length = 255)
    private String creationUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private PlayerSubscriptionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    @XmlTransient
    private Payment payment;

    @Column(name = "amountcents")
    private Long amountCents;

    @Column(name = "attendance_at")
    private LocalDateTime attendanceAt;

    @XmlTransient
    @JsonbTransient
    @Transient
    private DisplayPlayer player;

    // Constructeurs
    public PlayerSubscription() {}

    public DisplayPlayer getPlayer() {
        return player;
    }

    public void setDisplayPlayer(DisplayPlayer player) {
        this.player = player;
    }

    // Getters et Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public Long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(Long amountCents) {
        this.amountCents = amountCents;
    }

    public String getNrFfe() {
        return nrFfe;
    }

    public void setNrFfe(String nrFfe) {
        this.nrFfe = nrFfe;
    }

    public String getCreationUser() {
        return creationUser;
    }

    public void setCreationUser(String creationUser) {
        this.creationUser = creationUser;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public PlayerSubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(PlayerSubscriptionStatus status) {
        this.status = status;
    }

    public LocalDateTime getAttendanceAt() {
        return attendanceAt;
    }

    public void setAttendanceAt(LocalDateTime attendanceAt) {
        this.attendanceAt = attendanceAt;
    }
}

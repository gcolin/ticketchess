package com.github.gcolin.membership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.time.LocalDateTime;
import com.github.gcolin.platform.TimedEntity;

@Entity
@Table(name = "membership")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Membership extends TimedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "creation_user", nullable = false, length = 255)
    private String user;

    @Column(name = "nr_ffe", nullable = false, length = 255)
    private String nrFfe;

    @Column(name = "lastname", length = 255)
    private String lastname;

    @Column(name = "firstname", length = 255)
    private String firstname;

    @Column(name = "birthdate", length = 255)
    private String birthDate;

    @Column(name = "club_ref", nullable = false)
    private int clubRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MembershipStatus status;

    @Column(name = "amountcents", nullable = false)
    private int amountCents;

    public Membership() {}

    public Membership(
            String user,
            String nrFfe,
            String lastname,
            String firstname,
            String birthDate,
            int clubRef,
            LocalDateTime create,
            LocalDateTime update,
            MembershipStatus status,
            int amountCents) {
        this.user = user;
        this.nrFfe = nrFfe;
        this.lastname = lastname;
        this.firstname = firstname;
        this.birthDate = birthDate;
        this.clubRef = clubRef;
        this.createdAt = create;
        this.updatedAt = update;
        this.status = status;
        this.amountCents = amountCents;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getNrFfe() {
        return nrFfe;
    }

    public void setNrFfe(String nrFfe) {
        this.nrFfe = nrFfe;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public int getClubRef() {
        return clubRef;
    }

    public void setClubRef(int clubRef) {
        this.clubRef = clubRef;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    public void setStatus(MembershipStatus status) {
        this.status = status;
    }

    public int getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(int amountCents) {
        this.amountCents = amountCents;
    }
}

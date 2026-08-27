package com.github.gcolin.player;

import com.github.gcolin.platform.ModelUtils;

import java.time.LocalDateTime;
import com.github.gcolin.event.EventType;
import com.github.gcolin.registration.PlayerSubscriptionStatus;

public class DisplayPlayer implements IPlayer {

    private PlayerSubscriptionStatus status;
    private LocalDateTime attendanceAt;
    private int subId;
    private String rating;
    private String rapidRating;
    private String blitzRating;
    private String birthDate;
    private String clubRef;
    private String name;
    private String firstname;
    private Double price;
    private String category;
    private String nrffe;
    private String nrffeId;
    private String refffe;
    private String federation;
    private String affType;
    private String fideCode;
    private String fideTitre;
    private String club;
    private String fide;
    private boolean editable;
    private int id;
    private Integer pendingQueueAhead;

    public DisplayPlayer() {}

    public DisplayPlayer(IPlayer player) {
        name = player.getName();
        firstname = player.getFirstname();
        setCategory(player.getCategory());
        setNrffe(player.getLicence());
        setId(player.getId());
        setNrffeId(player.getNrffe());
        if (player.getNrffe().isEmpty()) {
            setNrffeId(player.getFide());
        }
        setAffType(player.getAffType());
        setFideTitre(player.getFideTitre());
        setFideCode(player.getFideCode());
        setClubRef(player.getClubRef());
        setFederation(player.getFederation());
        setClub(player.getClub());
        setFide(player.getFide());
        setRefffe(player.getRefffe());
        if (club == null) {
            club = federation;
        }
    }

    public String getClub() {
        return club;
    }

    public void setClub(String club) {
        this.club = club;
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

    public int getSubId() {
        return subId;
    }

    public void setSubId(int subId) {
        this.subId = subId;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public void setRating(IPlayer p, EventType eventType) {
        if (eventType == EventType.RAPID) {
            this.rating = p.getRapidRating();
        } else if (eventType == EventType.BLITZ) {
            this.rating = p.getBlitzRating();
        } else {
            this.rating = p.getRating();
        }
    }

    public String getRapidRating() {
        return rapidRating;
    }

    public void setRapidRating(String rapidRating) {
        this.rapidRating = rapidRating;
    }

    public String getBlitzRating() {
        return blitzRating;
    }

    public void setBlitzRating(String blitzRating) {
        this.blitzRating = blitzRating;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getClubRef() {
        return clubRef;
    }

    public void setClubRef(String clubRef) {
        this.clubRef = clubRef;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getNrffe() {
        return nrffe;
    }

    public void setNrffe(String nrffe) {
        this.nrffe = nrffe;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public Integer getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNrffeId() {
        return nrffeId;
    }

    public void setNrffeId(String nrffeId) {
        this.nrffeId = nrffeId;
    }

    @Override
    public String getRefffe() {
        return refffe;
    }

    public void setRefffe(String refffe) {
        this.refffe = refffe;
    }

    public String getFederation() {
        return federation;
    }

    public void setFederation(String federation) {
        this.federation = federation;
    }

    public String getAffType() {
        return affType;
    }

    public void setAffType(String affType) {
        this.affType = affType;
    }

    public String getFideCode() {
        return fideCode;
    }

    public void setFideCode(String fideCode) {
        this.fideCode = fideCode;
    }

    public String getFideTitre() {
        return fideTitre;
    }

    public void setFideTitre(String fideTitre) {
        this.fideTitre = fideTitre;
    }

    @Override
    public boolean isYoung() {
        return ModelUtils.isYoung(category);
    }

    @Override
    public String getLicence() {
        return nrffe == null || nrffe.isEmpty() ? fide : nrffe;
    }

    public String getFide() {
        return fide;
    }

    public void setFide(String fide) {
        this.fide = fide;
    }

    public Integer getPendingQueueAhead() {
        return pendingQueueAhead;
    }

    public void setPendingQueueAhead(Integer pendingQueueAhead) {
        this.pendingQueueAhead = pendingQueueAhead;
    }
}

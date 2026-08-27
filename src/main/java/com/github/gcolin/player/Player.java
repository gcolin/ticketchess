package com.github.gcolin.player;

import com.github.gcolin.platform.ModelUtils;

public class Player implements java.io.Serializable, IPlayer {

    private static final long serialVersionUID = 1L;
    private String nrffe; // Numéro FFE
    private String fide;
    private String name;
    private String firstname;
    private String clubRef;
    private String rating; // Elo rating
    private String rapidRating; // FIDE ID
    private String blitzRating; // Score Lucene optionnel
    private String birthDate;
    private String category;
    private String fideTitre;
    private String fideCode;
    private String affType;
    private String federation;
    private String club;
    private String refffe;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getNrffe() {
        return nrffe;
    }

    public void setNrffe(String nrffe) {
        this.nrffe = nrffe;
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

    public String getClubRef() {
        return clubRef;
    }

    public void setClubRef(String clubRef) {
        this.clubRef = clubRef;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
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

    public boolean isYoung() {
        return ModelUtils.isYoung(category);
    }

    @Override
    public String getLicence() {
        return getNrffe();
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public Integer getId() {
        return -1;
    }

    public String getFideTitre() {
        return fideTitre;
    }

    public void setFideTitre(String fideTitre) {
        this.fideTitre = fideTitre;
    }

    public String getFideCode() {
        return fideCode;
    }

    public void setFideCode(String fideCode) {
        this.fideCode = fideCode;
    }

    public String getAffType() {
        return affType;
    }

    public void setAffType(String affType) {
        this.affType = affType;
    }

    public String getFederation() {
        return federation;
    }

    public void setFederation(String federation) {
        this.federation = federation;
    }

    public String getClub() {
        return club;
    }

    public void setClub(String club) {
        this.club = club;
    }

    public String getFide() {
        return fide;
    }

    public void setFide(String fide) {
        this.fide = fide;
    }

    @Override
    public String getRefffe() {
        return refffe;
    }

    public void setRefffe(String refffe) {
        this.refffe = refffe;
    }
}

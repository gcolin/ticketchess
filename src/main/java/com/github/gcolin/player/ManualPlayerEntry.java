package com.github.gcolin.player;

import com.github.gcolin.platform.ModelUtils;

public class ManualPlayerEntry {

    private String nrffe;
    private String fide;
    private String name;
    private String firstname;
    private String birth;
    private String category;
    private String affType;
    private String fideCode;
    private String fideTitre;
    private String federation;
    private String club;
    private String eloStd;
    private String eloRapide;
    private String eloBlitz;

    public void normalize() {
        nrffe = ModelUtils.trimToNull(nrffe);
        fide = ModelUtils.trimToNull(fide);
        name = ModelUtils.trimToNull(name);
        firstname = ModelUtils.trimToNull(firstname);
        birth = ModelUtils.trimToNull(birth);
        category = ModelUtils.trimToNull(category);
        affType = ModelUtils.trimToNull(affType);
        fideCode = ModelUtils.trimToNull(fideCode);
        fideTitre = ModelUtils.trimToNull(fideTitre);
        federation = ModelUtils.trimToNull(federation);
        club = ModelUtils.trimToNull(club);
        eloStd = ModelUtils.trimToNull(eloStd);
        eloRapide = ModelUtils.trimToNull(eloRapide);
        eloBlitz = ModelUtils.trimToNull(eloBlitz);
    }

    public String getKey() {
        if (nrffe != null) {
            return "nrffe:" + nrffe;
        }
        if (fide != null) {
            return "fide:" + fide;
        }
        return null;
    }

    public String getNrffe() {
        return nrffe;
    }

    public void setNrffe(String nrffe) {
        this.nrffe = nrffe;
    }

    public String getFide() {
        return fide;
    }

    public void setFide(String fide) {
        this.fide = fide;
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

    public String getBirth() {
        return birth;
    }

    public void setBirth(String birth) {
        this.birth = birth;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public String getEloStd() {
        return eloStd;
    }

    public void setEloStd(String eloStd) {
        this.eloStd = eloStd;
    }

    public String getEloRapide() {
        return eloRapide;
    }

    public void setEloRapide(String eloRapide) {
        this.eloRapide = eloRapide;
    }

    public String getEloBlitz() {
        return eloBlitz;
    }

    public void setEloBlitz(String eloBlitz) {
        this.eloBlitz = eloBlitz;
    }
}

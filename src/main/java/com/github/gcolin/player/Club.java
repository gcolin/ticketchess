package com.github.gcolin.player;

public class Club {
    private String ref; // Ref club
    private String nrffe;
    private String nom;
    private String commune;
    private String actif;
    private String ligue;
    private float score; // Score Lucene optionnel

    // ----- Getters / Setters -----
    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getNrffe() {
        return nrffe;
    }

    public void setNrffe(String nrffe) {
        this.nrffe = nrffe;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getCommune() {
        return commune;
    }

    public void setCommune(String commune) {
        this.commune = commune;
    }

    public String getActif() {
        return actif;
    }

    public void setActif(String actif) {
        this.actif = actif;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "Club [ref="
                + ref
                + ", nrffe="
                + nrffe
                + ", nom="
                + nom
                + ", commune="
                + commune
                + ", actif="
                + actif
                + ", score="
                + score
                + "]";
    }

    public String getLigue() {
        return ligue;
    }

    public void setLigue(String ligue) {
        this.ligue = ligue;
    }
}

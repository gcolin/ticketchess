package com.github.gcolin.player;

public interface IPlayer {
    boolean isYoung();

    String getNrffe();

    String getLicence();

    String getName();

    String getFirstname();

    String getCategory();

    String getRating();

    String getBlitzRating();

    String getRapidRating();

    boolean isEditable();

    Integer getId();

    String getBirthDate();

    String getClubRef();

    String getClub();

    String getFideTitre();

    String getFideCode();

    String getAffType();

    String getFederation();

    String getFide();

    /** FFE internal player id (JOUEUR.Ref), used as RefFFE in Papi tournament files. */
    default String getRefffe() {
        return null;
    }

    default String getFullname() {
        String firstname = getFirstname();
        String name = getName();
        if (firstname == null) {
            return firstname;
        }
        if (name == null) {
            return name;
        }
        return firstname + " " + name;
    }
}

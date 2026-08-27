package com.github.gcolin.player;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class FidePlayer {

    private long fideid;
    private String name;
    private String country;
    private String sex;

    private String title;
    private String w_title;
    private String o_title;
    private String foa_title;

    private int rating;
    private int games;
    private int k;

    private int rapid_rating;
    private int rapid_games;
    private int rapid_k;

    private int blitz_rating;
    private int blitz_games;
    private int blitz_k;

    private int birthday;
    private String flag;

    // Getters / Setters

    public long getFideid() {
        return fideid;
    }

    public void setFideid(long fideid) {
        this.fideid = fideid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getW_title() {
        return w_title;
    }

    public void setW_title(String w_title) {
        this.w_title = w_title;
    }

    public String getO_title() {
        return o_title;
    }

    public void setO_title(String o_title) {
        this.o_title = o_title;
    }

    public String getFoa_title() {
        return foa_title;
    }

    public void setFoa_title(String foa_title) {
        this.foa_title = foa_title;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public int getGames() {
        return games;
    }

    public void setGames(int games) {
        this.games = games;
    }

    public int getK() {
        return k;
    }

    public void setK(int k) {
        this.k = k;
    }

    public int getRapid_rating() {
        return rapid_rating;
    }

    public void setRapid_rating(int rapid_rating) {
        this.rapid_rating = rapid_rating;
    }

    public int getRapid_games() {
        return rapid_games;
    }

    public void setRapid_games(int rapid_games) {
        this.rapid_games = rapid_games;
    }

    public int getRapid_k() {
        return rapid_k;
    }

    public void setRapid_k(int rapid_k) {
        this.rapid_k = rapid_k;
    }

    public int getBlitz_rating() {
        return blitz_rating;
    }

    public void setBlitz_rating(int blitz_rating) {
        this.blitz_rating = blitz_rating;
    }

    public int getBlitz_games() {
        return blitz_games;
    }

    public void setBlitz_games(int blitz_games) {
        this.blitz_games = blitz_games;
    }

    public int getBlitz_k() {
        return blitz_k;
    }

    public void setBlitz_k(int blitz_k) {
        this.blitz_k = blitz_k;
    }

    public int getBirthday() {
        return birthday;
    }

    public void setBirthday(int birthday) {
        this.birthday = birthday;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }
}

package com.github.gcolin.membership;

import com.github.gcolin.club.ClubSeason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.github.gcolin.platform.TimedEntity;

@Entity
@Table(name = "membership_option")
public class MembershipOption extends TimedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_type", nullable = false)
    private MembershipOptionType optionType;

    @Column(name = "option_value", nullable = false, length = 255)
    private String optionValue;

    @Column(name = "amount_cents")
    private Integer amountCents;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_rule")
    private MembershipOptionAccessRule accessRule = MembershipOptionAccessRule.ALL;

    @ManyToOne
    @JoinColumn(name = "license_id", foreignKey = @ForeignKey(name = "fk_membership_option_license"))
    private License license;

    @ManyToOne
    @JoinColumn(name = "season_id", foreignKey = @ForeignKey(name = "fk_membership_option_season"))
    private ClubSeason season;

    public MembershipOption() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public MembershipOptionType getOptionType() {
        return optionType;
    }

    public void setOptionType(MembershipOptionType optionType) {
        this.optionType = optionType;
    }

    public String getOptionValue() {
        return optionValue;
    }

    public void setOptionValue(String optionValue) {
        this.optionValue = optionValue;
    }

    public Integer getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(Integer amountCents) {
        this.amountCents = amountCents;
    }

    public MembershipOptionAccessRule getAccessRule() {
        return accessRule;
    }

    public void setAccessRule(MembershipOptionAccessRule accessRule) {
        this.accessRule = accessRule;
    }

    public License getLicense() {
        return license;
    }

    public void setLicense(License license) {
        this.license = license;
    }

    public ClubSeason getSeason() {
        return season;
    }

    public void setSeason(ClubSeason season) {
        this.season = season;
    }
}

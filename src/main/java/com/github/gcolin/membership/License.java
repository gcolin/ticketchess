package com.github.gcolin.membership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import com.github.gcolin.platform.TimedEntity;

@Entity
@Table(name = "license")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class License extends TimedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 50, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_rule", nullable = false)
    private MembershipOptionAccessRule accessRule = MembershipOptionAccessRule.ALL;

    public License() {}

    public License(String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MembershipOptionAccessRule getAccessRule() {
        return accessRule;
    }

    public void setAccessRule(MembershipOptionAccessRule accessRule) {
        this.accessRule = accessRule;
    }

    @Override
    public String toString() {
        return "License{" + "id=" + id + ", name='" + name + '\'' + '}';
    }
}

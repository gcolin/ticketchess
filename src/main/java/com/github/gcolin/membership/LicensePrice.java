package com.github.gcolin.membership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import com.github.gcolin.platform.TimedEntity;

@Entity
@Table(name = "license_price", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"category", "license_id"})
})
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class LicensePrice extends TimedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "category", nullable = false, length = 10)
    private String category;

    @Column(name = "price_cents", nullable = false)
    private Integer priceCents;

    @ManyToOne(optional = false)
    @JoinColumn(name = "license_id", nullable = false, foreignKey = @ForeignKey(name = "fk_license_price_license"))
    private License license;

    public LicensePrice() {}

    public LicensePrice(String category, Integer priceCents, License license) {
        this.category = category;
        this.priceCents = priceCents;
        this.license = license;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getPriceCents() {
        return priceCents;
    }

    public void setPriceCents(Integer priceCents) {
        this.priceCents = priceCents;
    }

    public License getLicense() {
        return license;
    }

    public void setLicense(License license) {
        this.license = license;
    }

    @Override
    public String toString() {
        return "LicensePrice{" + "id=" + id + ", category='" + category + '\'' + ", priceCents=" + priceCents
                + ", license=" + (license != null ? license.getName() : null) + '}';
    }
}

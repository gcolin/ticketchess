package com.github.gcolin.event;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "big_event")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class EventCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String name;

    @OneToMany(mappedBy = "eventCollection", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @XmlTransient
    private List<Event> events = new ArrayList<>();

    @Transient
    private Integer maxSubscribe;

    @Transient
    private Integer nbSubscriptions;

    // =========================
    // Constructeurs
    // =========================
    public EventCollection() {}

    // =========================
    // Getters et Setters
    // =========================
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

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public Integer getMaxSubscribe() {
        return maxSubscribe;
    }

    public void setMaxSubscribe(Integer maxSubscribe) {
        this.maxSubscribe = maxSubscribe;
    }

    public Integer getNbSubscriptions() {
        return nbSubscriptions;
    }

    public void setNbSubscriptions(Integer nbSubscriptions) {
        this.nbSubscriptions = nbSubscriptions;
    }

    public int getEventCount() {
        return events != null ? events.size() : 0;
    }

    @Override
    public String toString() {
        return "EventCollection [id=" + id + ", name=" + name + ", eventCount=" + getEventCount() + "]";
    }
}

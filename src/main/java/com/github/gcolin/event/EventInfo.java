package com.github.gcolin.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "eventinfo")
public class EventInfo {

    @Id
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "eventid")
    private Event event;

    @Column(columnDefinition = "TEXT")
    private String description;

    // =========================
    // Constructeurs
    // =========================
    public EventInfo() {}

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventInfo)) return false;
        EventInfo that = (EventInfo) o;
        return event != null && event.getId().equals(that.event.getId());
    }

    @Override
    public int hashCode() {
        return event != null ? event.getId().hashCode() : 0;
    }
}

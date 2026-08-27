package com.github.gcolin.event;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import com.github.gcolin.notification.Notification;

@Entity
@Table(name = "event_group")
public class EventGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 255)
    private String shortname;

    // Relation vers Notification (OneToMany)
    @OneToMany(mappedBy = "eventGroup")
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "eventGroup")
    private List<Event> events = new ArrayList<>();

    // =========================
    // Constructeurs
    // =========================
    public EventGroup() {}

    public EventGroup(String name) {
        this.name = name;
    }

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

    public List<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public List<Event> getEvents() {
        return events;
    }

    public String getShortname() {
        return shortname;
    }

    public void setShortname(String shortname) {
        this.shortname = shortname;
    }
}

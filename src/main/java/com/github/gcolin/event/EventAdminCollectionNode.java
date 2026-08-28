package com.github.gcolin.event;

import java.util.ArrayList;
import java.util.List;

public class EventAdminCollectionNode {

    private final EventCollection collection;
    private final List<Event> events = new ArrayList<>();

    public EventAdminCollectionNode(EventCollection collection) {
        this.collection = collection;
    }

    public EventCollection getCollection() {
        return collection;
    }

    public List<Event> getEvents() {
        return events;
    }
}

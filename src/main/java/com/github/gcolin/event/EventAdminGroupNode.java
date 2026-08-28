package com.github.gcolin.event;

import java.util.ArrayList;
import java.util.List;

public class EventAdminGroupNode {

    private final EventGroup group;
    private final boolean ungrouped;
    private final List<EventAdminCollectionNode> collections = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();

    public EventAdminGroupNode(EventGroup group, boolean ungrouped) {
        this.group = group;
        this.ungrouped = ungrouped;
    }

    public EventGroup getGroup() {
        return group;
    }

    public boolean isUngrouped() {
        return ungrouped;
    }

    public List<EventAdminCollectionNode> getCollections() {
        return collections;
    }

    public List<Event> getEvents() {
        return events;
    }
}

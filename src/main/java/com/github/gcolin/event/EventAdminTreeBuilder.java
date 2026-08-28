package com.github.gcolin.event;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EventAdminTreeBuilder {

    private static final Comparator<Event> EVENT_ORDER = Comparator.comparing(
                    Event::getStartDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Event::getName, String.CASE_INSENSITIVE_ORDER);

    private static final Comparator<EventAdminCollectionNode> COLLECTION_ORDER = Comparator.comparing(
                    EventAdminTreeBuilder::earliestEventDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(n -> n.getCollection().getName(), String.CASE_INSENSITIVE_ORDER);

    private static final Comparator<EventGroup> GROUP_ORDER =
            Comparator.comparing(EventGroup::getName, String.CASE_INSENSITIVE_ORDER);

    public List<EventAdminGroupNode> build(List<EventGroup> groups, List<Event> events) {
        List<EventAdminGroupNode> tree = new ArrayList<>();
        List<EventGroup> sortedGroups = new ArrayList<>(groups);
        sortedGroups.sort(GROUP_ORDER);
        for (EventGroup group : sortedGroups) {
            tree.add(buildGroupNode(group, false, events));
        }
        EventAdminGroupNode ungrouped = buildGroupNode(null, true, events);
        if (!ungrouped.getCollections().isEmpty() || !ungrouped.getEvents().isEmpty()) {
            tree.add(ungrouped);
        }
        return tree;
    }

    private EventAdminGroupNode buildGroupNode(EventGroup group, boolean ungrouped, List<Event> allEvents) {
        EventAdminGroupNode node = new EventAdminGroupNode(group, ungrouped);
        List<Event> groupEvents = allEvents.stream()
                .filter(event -> belongsToGroup(event, group))
                .sorted(EVENT_ORDER)
                .toList();

        Map<Integer, EventAdminCollectionNode> collectionNodes = new LinkedHashMap<>();
        for (Event event : groupEvents) {
            EventCollection collection = event.getEventCollection();
            if (collection == null) {
                node.getEvents().add(event);
                continue;
            }
            EventAdminCollectionNode collectionNode = collectionNodes.computeIfAbsent(
                    collection.getId(), id -> new EventAdminCollectionNode(collection));
            collectionNode.getEvents().add(event);
        }

        List<EventAdminCollectionNode> sortedCollections = new ArrayList<>(collectionNodes.values());
        sortedCollections.sort(COLLECTION_ORDER);
        for (EventAdminCollectionNode collectionNode : sortedCollections) {
            collectionNode.getEvents().sort(EVENT_ORDER);
            node.getCollections().add(collectionNode);
        }
        node.getEvents().sort(EVENT_ORDER);
        return node;
    }

    private static LocalDateTime earliestEventDate(EventAdminCollectionNode collectionNode) {
        return collectionNode.getEvents().stream()
                .map(Event::getStartDate)
                .filter(date -> date != null)
                .min(LocalDateTime::compareTo)
                .orElse(null);
    }

    private boolean belongsToGroup(Event event, EventGroup group) {
        if (group == null) {
            return event.getEventGroup() == null;
        }
        EventGroup eventGroup = event.getEventGroup();
        return eventGroup != null && group.getId().equals(eventGroup.getId());
    }
}

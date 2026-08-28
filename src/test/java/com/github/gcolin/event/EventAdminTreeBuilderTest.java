package com.github.gcolin.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventAdminTreeBuilderTest {

    private final EventAdminTreeBuilder builder = new EventAdminTreeBuilder();

    @Test
    void buildShouldNestCollectionsAndStandaloneEventsUnderGroup() {
        EventGroup group = group(1, "Open");
        EventCollection collection = collection(10, "Weekend series");

        Event inCollection = event(100, "Rapid", group, collection);
        Event standalone = event(101, "Blitz", group, null);

        List<EventAdminGroupNode> tree = builder.build(List.of(group), List.of(inCollection, standalone));

        assertEquals(1, tree.size());
        EventAdminGroupNode node = tree.get(0);
        assertEquals(group, node.getGroup());
        assertEquals(1, node.getCollections().size());
        assertEquals(collection, node.getCollections().get(0).getCollection());
        assertEquals(List.of(inCollection), node.getCollections().get(0).getEvents());
        assertEquals(List.of(standalone), node.getEvents());
    }

    @Test
    void buildShouldAppendUngroupedEvents() {
        EventCollection collection = collection(20, "Standalone collection");
        Event grouped = event(1, "Grouped", group(1, "Club"), null);
        Event ungroupedInCollection = event(2, "Open rapid", null, collection);
        Event ungroupedStandalone = event(3, "Open blitz", null, null);

        List<EventAdminGroupNode> tree = builder.build(
                List.of(group(1, "Club")), List.of(grouped, ungroupedInCollection, ungroupedStandalone));

        assertEquals(2, tree.size());
        EventAdminGroupNode ungrouped = tree.get(1);
        assertTrue(ungrouped.isUngrouped());
        assertEquals(1, ungrouped.getCollections().size());
        assertEquals(List.of(ungroupedInCollection), ungrouped.getCollections().get(0).getEvents());
        assertEquals(List.of(ungroupedStandalone), ungrouped.getEvents());
    }

    @Test
    void buildShouldIncludeEmptyGroups() {
        EventGroup emptyGroup = group(1, "Empty");
        EventGroup activeGroup = group(2, "Active");
        Event event = event(1, "Rapid", activeGroup, null);

        List<EventAdminGroupNode> tree = builder.build(List.of(emptyGroup, activeGroup), List.of(event));

        assertEquals(2, tree.size());
        assertEquals(activeGroup, tree.get(0).getGroup());
        assertEquals(emptyGroup, tree.get(1).getGroup());
        assertTrue(tree.get(1).getCollections().isEmpty());
        assertTrue(tree.get(1).getEvents().isEmpty());
    }

    @Test
    void buildShouldSortEventsByDateThenNameWithinCollectionAndStandalone() {
        EventGroup group = group(1, "Open");
        EventCollection collection = collection(1, "Weekend series");

        Event sameDateB = eventWithDate(1, "Blitz", group, collection, 2026, 3, 1);
        Event sameDateA = eventWithDate(2, "Rapid", group, collection, 2026, 3, 1);
        Event earlier = eventWithDate(3, "Earlier", group, collection, 2026, 2, 1);
        Event laterStandalone = eventWithDate(4, "Later", group, null, 2026, 5, 1);

        List<EventAdminGroupNode> tree = builder.build(
                List.of(group), List.of(sameDateB, sameDateA, earlier, laterStandalone));

        EventAdminGroupNode node = tree.get(0);
        List<String> collectionEventNames = node.getCollections().get(0).getEvents().stream()
                .map(Event::getName)
                .toList();
        List<String> standaloneEventNames =
                node.getEvents().stream().map(Event::getName).toList();

        assertEquals(List.of("Earlier", "Blitz", "Rapid"), collectionEventNames);
        assertEquals(List.of("Later"), standaloneEventNames);
    }

    private EventGroup group(int id, String name) {
        EventGroup group = new EventGroup();
        group.setId(id);
        group.setName(name);
        group.setShortname(name.toLowerCase());
        return group;
    }

    private EventCollection collection(int id, String name) {
        EventCollection collection = new EventCollection();
        collection.setId(id);
        collection.setName(name);
        return collection;
    }

    private Event event(int id, String name, EventGroup group, EventCollection collection) {
        return eventWithDate(id, name, group, collection, 2026, 1, Math.min(id, 28));
    }

    private Event eventWithDate(
            int id, String name, EventGroup group, EventCollection collection, int year, int month, int day) {
        Event event = new Event();
        event.setId(id);
        event.setName(name);
        event.setStartDate(LocalDateTime.of(year, month, day, 0, 0));
        event.setEndDate(LocalDateTime.of(year, month, day, 0, 0));
        event.setEventGroup(group);
        event.setEventCollection(collection);
        return event;
    }
}

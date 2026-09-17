package com.github.gcolin.event;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SharlySelectService {

    private final EventDao eventDao;
    private final EventCollectionOptionDao eventCollectionOptionDao;

    @Inject
    public SharlySelectService(EventDao eventDao, EventCollectionOptionDao eventCollectionOptionDao) {
        this.eventDao = eventDao;
        this.eventCollectionOptionDao = eventCollectionOptionDao;
    }

    public List<SharlySelectItem> listOpenItems() {
        List<Event> active = eventDao.findAllByStatus(EventStatus.ACTIVE);
        Map<Integer, List<Event>> byCollection = new LinkedHashMap<>();
        List<Event> standalone = new ArrayList<>();

        for (Event event : active) {
            EventCollection collection = event.getEventCollection();
            if (collection != null && collection.getId() != null) {
                byCollection.computeIfAbsent(collection.getId(), id -> new ArrayList<>()).add(event);
            } else {
                standalone.add(event);
            }
        }

        List<SharlySelectItem> items = new ArrayList<>();
        for (Map.Entry<Integer, List<Event>> entry : byCollection.entrySet()) {
            List<Event> events = entry.getValue();
            EventCollection collection = events.get(0).getEventCollection();
            String slug = eventCollectionOptionDao.findOptionValue(
                    collection.getId(), EventCollectionOptionType.CHESS_EVENT_ID);
            String chessEventId =
                    (slug != null && !slug.isBlank()) ? slug.trim() : String.valueOf(collection.getId());
            items.add(new SharlySelectItem(
                    SharlySelectItem.Kind.COLLECTION,
                    collection.getId(),
                    null,
                    chessEventId,
                    collection.getName(),
                    events.size()));
        }
        for (Event event : standalone) {
            items.add(new SharlySelectItem(
                    SharlySelectItem.Kind.EVENT,
                    null,
                    event.getId(),
                    String.valueOf(event.getId()),
                    event.getName(),
                    1));
        }
        items.sort(Comparator.comparing(SharlySelectItem::getName, Comparator.nullsLast(String::compareToIgnoreCase)));
        return items;
    }

    public SharlySelectItem resolveSelection(String kind, Integer collectionId, Integer eventId) {
        if ("COLLECTION".equalsIgnoreCase(kind) && collectionId != null) {
            return listOpenItems().stream()
                    .filter(item -> item.isCollection() && collectionId.equals(item.getCollectionId()))
                    .findFirst()
                    .orElse(null);
        }
        if ("EVENT".equalsIgnoreCase(kind) && eventId != null) {
            return listOpenItems().stream()
                    .filter(item -> !item.isCollection() && eventId.equals(item.getEventId()))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}

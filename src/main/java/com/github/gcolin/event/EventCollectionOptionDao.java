package com.github.gcolin.event;

import com.github.gcolin.event.EventCollection;
import com.github.gcolin.event.EventCollectionOption;
import com.github.gcolin.event.EventCollectionOptionType;
import com.github.gcolin.platform.Transactional;
import jakarta.persistence.TypedQuery;
import com.github.gcolin.platform.AbstractDao;

public class EventCollectionOptionDao extends AbstractDao<EventCollectionOption> {

    public EventCollectionOptionDao() {
        super(EventCollectionOption.class);
    }

    public EventCollectionOption findByEventCollectionIdAndOptionType(
            int eventCollectionId, EventCollectionOptionType optionType) {
        TypedQuery<EventCollectionOption> query = em.createQuery(
                "SELECT o FROM EventCollectionOption o WHERE o.eventCollection.id = :eventCollectionId AND o.optionType = :optionType",
                EventCollectionOption.class);
        query.setParameter("eventCollectionId", eventCollectionId);
        query.setParameter("optionType", optionType);
        return query.getResultStream().findFirst().orElse(null);
    }

    public void setOption(int eventCollectionId, EventCollectionOptionType optionType, String value) {
        EventCollectionOption option = findByEventCollectionIdAndOptionType(eventCollectionId, optionType);
        if (value == null || value.isBlank()) {
            if (option != null) {
                remove(option);
            }
            return;
        }
        if (option == null) {
            EventCollection eventCollection = em.find(EventCollection.class, eventCollectionId);
            option = new EventCollectionOption();
            option.setEventCollection(eventCollection);
            option.setOptionType(optionType);
            option.setValue(value);
            persist(option);
        } else {
            option.setValue(value);
        }
    }

    public String findOptionValue(int eventCollectionId, EventCollectionOptionType optionType) {
        EventCollectionOption option = findByEventCollectionIdAndOptionType(eventCollectionId, optionType);
        return option == null ? null : option.getValue();
    }

    public Integer findIntOptionValue(int eventCollectionId, EventCollectionOptionType optionType) {
        String value = findOptionValue(eventCollectionId, optionType);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public EventCollection findByOptionValue(EventCollectionOptionType optionType, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        TypedQuery<EventCollection> query = em.createQuery(
                "SELECT o.eventCollection FROM EventCollectionOption o"
                        + " WHERE o.optionType = :optionType AND o.value = :value",
                EventCollection.class);
        query.setParameter("optionType", optionType);
        query.setParameter("value", value.trim());
        return query.getResultStream().findFirst().orElse(null);
    }
}

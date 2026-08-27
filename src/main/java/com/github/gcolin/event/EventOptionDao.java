package com.github.gcolin.event;

import com.github.gcolin.event.Event;
import com.github.gcolin.event.EventOption;
import com.github.gcolin.event.EventOptionType;
import com.github.gcolin.platform.AbstractDao;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.function.Supplier;

public class EventOptionDao extends AbstractDao<EventOption> {

    private Supplier<EventDao> eventDao;

    public EventOptionDao() {
        super(EventOption.class);
    }

    public void setEventDao(Supplier<EventDao> eventDao) {
        this.eventDao = eventDao;
    }

    public List<EventOption> findByEventId(int eventId) {
        TypedQuery<EventOption> query =
                em.createQuery("SELECT o FROM EventOption o WHERE o.event.id = :eventId", EventOption.class);
        query.setParameter("eventId", eventId);
        return query.getResultList();
    }

    public EventOption findByEventIdAndOptionType(int eventId, EventOptionType optionType) {
        TypedQuery<EventOption> query = em.createQuery(
                "SELECT o FROM EventOption o WHERE o.event.id = :eventId AND o.optionType = :optionType",
                EventOption.class);
        query.setParameter("eventId", eventId);
        query.setParameter("optionType", optionType);
        return query.getResultStream().findFirst().orElse(null);
    }

    public void setOption(int eventId, EventOptionType optionType, String value) {
        Event event = eventDao.get().find(eventId);
        EventOption option = findByEventIdAndOptionType(eventId, optionType);
        if (option == null) {
            option = new EventOption();
            option.setEvent(event);
            option.setOptionType(optionType);
            option.setValue(value);
            persist(option);
        } else {
            option.setValue(value);
        }
    }

    public String findOptionValue(int eventId, EventOptionType optionType) {
        EventOption option = findByEventIdAndOptionType(eventId, optionType);
        return option == null ? null : option.getValue();
    }

    public Integer findIntOptionValue(int eventId, EventOptionType optionType) {
        String value = findOptionValue(eventId, optionType);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

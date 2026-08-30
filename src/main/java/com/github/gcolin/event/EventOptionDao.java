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

    public List<Integer> findEventIdsByChessEventCredentials(String userId, String password) {
        if (userId == null || userId.isBlank() || password == null) {
            return List.of();
        }
        TypedQuery<Integer> query = em.createQuery(
                "SELECT u.event.id FROM EventOption u, EventOption p"
                        + " WHERE u.event.id = p.event.id"
                        + " AND u.optionType = :userType AND u.value = :userId"
                        + " AND p.optionType = :passType AND p.value = :password",
                Integer.class);
        query.setParameter("userType", EventOptionType.CHESS_EVENT_USER);
        query.setParameter("passType", EventOptionType.CHESS_EVENT_PASSWORD);
        query.setParameter("userId", userId.trim());
        query.setParameter("password", password);
        return query.getResultList();
    }

    public boolean chessEventUserExists(String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(o) FROM EventOption o WHERE o.optionType = :type AND o.value = :userId",
                Long.class);
        query.setParameter("type", EventOptionType.CHESS_EVENT_USER);
        query.setParameter("userId", userId.trim());
        return query.getSingleResult() > 0;
    }
}

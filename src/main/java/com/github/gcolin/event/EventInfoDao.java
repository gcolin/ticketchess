package com.github.gcolin.event;

import com.github.gcolin.event.Event;
import com.github.gcolin.event.EventInfo;
import com.github.gcolin.platform.AbstractDao;
import jakarta.persistence.TypedQuery;
import java.util.function.Supplier;

public class EventInfoDao extends AbstractDao<EventInfo> {

    private Supplier<EventDao> eventService;

    public EventInfoDao() {
        super(EventInfo.class);
    }

    public void setEventDao(Supplier<EventDao> eventService) {
        this.eventService = eventService;
    }

    public EventInfo find(Event id) {
        TypedQuery<EventInfo> query =
                em.createQuery("SELECT e FROM EventInfo e where e.event = :event", EventInfo.class);
        query.setParameter("event", id);
        return query.getResultStream().findFirst().orElse(null);
    }

    public void setEventInfo(Integer eventId, String description) {
        Event event = eventService.get().find(eventId);
        EventInfo eventInfo = event.getEventInfo();
        if (eventInfo == null) {
            eventInfo = new EventInfo();
            eventInfo.setEvent(event);
            eventInfo.setDescription(description);
            persist(eventInfo);
        } else {
            eventInfo.setDescription(description);
        }
    }

    public EventInfo findByEventId(int id) {
        TypedQuery<EventInfo> query =
                em.createQuery("SELECT e FROM EventInfo e where e.event.id = :id", EventInfo.class);
        query.setParameter("id", id);
        return query.getResultStream().findFirst().orElse(null);
    }
}

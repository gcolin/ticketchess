package com.github.gcolin.event;

import com.github.gcolin.event.EventGroup;
import com.github.gcolin.platform.Transactional;
import jakarta.persistence.TypedQuery;
import com.github.gcolin.platform.AbstractDao;

public class EventGroupDao extends AbstractDao<EventGroup> {

    public EventGroupDao() {
        super(EventGroup.class);
    }

    public EventGroup findByShortname(String shortname) {
        TypedQuery<EventGroup> query =
                em.createQuery("SELECT e FROM EventGroup e where e.shortname = :shortname", EventGroup.class);
        query.setParameter("shortname", shortname);
        return query.getResultStream().findFirst().orElse(null);
    }

    public EventGroup findDetachedForEdit(int id) {
        EventGroup eventGroup = find(id);
        eventGroup.setEvents(detachAll(eventGroup.getEvents()));
        eventGroup.setNotifications(detachAll(eventGroup.getNotifications()));
        return detach(eventGroup);
    }
}

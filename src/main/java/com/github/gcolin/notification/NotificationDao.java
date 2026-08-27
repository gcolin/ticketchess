package com.github.gcolin.notification;

import com.github.gcolin.event.EventDao;
import com.github.gcolin.event.EventGroupDao;
import com.github.gcolin.event.EventStatus;
import com.github.gcolin.platform.AbstractDao;
import jakarta.persistence.TypedQuery;
import java.util.List;

public class NotificationDao extends AbstractDao<Notification> {

    private EventDao eventService;
    private EventGroupDao eventGroupService;

    public NotificationDao() {
        super(Notification.class);
    }

    public void setEventDao(EventDao eventService) {
        this.eventService = eventService;
    }

    public void setEventGroupDao(EventGroupDao eventGroupService) {
        this.eventGroupService = eventGroupService;
    }

    public List<Notification> findByStatus(EventStatus status) {
        TypedQuery<Notification> query =
                em.createQuery("SELECT e FROM Notification e where e.event_group = :status", Notification.class);
        query.setParameter("status", status);
        return query.getResultList();
    }

    public List<Notification> findGlobal() {
        TypedQuery<Notification> query = em.createQuery(
                "SELECT n FROM Notification n where n.event is NULL and n.eventGroup is NULL", Notification.class);
        return query.getResultList();
    }

    public void setNotification(Integer id, Integer eventId, Integer eventGroupId, String content) {
        Notification notification;
        if (id == null || id == 0) {
            notification = new Notification();
        } else {
            notification = find(id);
        }
        if (eventId != null) {
            notification.setEvent(eventService.find(eventId));
        }
        if (eventGroupId != null) {
            notification.setEventGroup(eventGroupService.find(eventGroupId));
        }
        notification.setContent(content);

        if (notification.getId() == null) {
            persist(notification);
        }
    }
}

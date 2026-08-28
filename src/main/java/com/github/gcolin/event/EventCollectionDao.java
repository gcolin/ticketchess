package com.github.gcolin.event;

import com.github.gcolin.event.EventCollection;
import com.github.gcolin.event.EventCollectionOptionType;
import com.github.gcolin.platform.AbstractDao;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.function.Supplier;

public class EventCollectionDao extends AbstractDao<EventCollection> {

    private Supplier<EventCollectionOptionDao> eventCollectionOptionDao;
    private Supplier<PlayerSubscriptionDao> playerSubscriptionDao;

    public EventCollectionDao() {
        super(EventCollection.class);
    }

    public void setEventCollectionOptionDao(Supplier<EventCollectionOptionDao> eventCollectionOptionDao) {
        this.eventCollectionOptionDao = eventCollectionOptionDao;
    }

    public void setPlayerSubscriptionDao(Supplier<PlayerSubscriptionDao> playerSubscriptionDao) {
        this.playerSubscriptionDao = playerSubscriptionDao;
    }

    public List<EventCollection> allOrdered() {
        TypedQuery<EventCollection> query =
                em.createQuery("SELECT b FROM EventCollection b ORDER BY b.name ASC", EventCollection.class);
        List<EventCollection> collections = query.getResultList();
        collections.forEach(this::fillSubscriptionLimits);
        return collections;
    }

    public long countLinkedEvents(Integer id) {
        TypedQuery<Long> query =
                em.createQuery("SELECT COUNT(e) FROM Event e WHERE e.eventCollection.id = :id", Long.class);
        query.setParameter("id", id);
        return query.getSingleResult();
    }

    public void fillSubscriptionLimits(EventCollection eventCollection) {
        if (eventCollection == null || eventCollection.getId() == null) {
            return;
        }
        eventCollection.setMaxSubscribe(
                eventCollectionOptionDao.get().findIntOptionValue(eventCollection.getId(), EventCollectionOptionType.MAX_SUBSCRIPTIONS));
        eventCollection.setNbSubscriptions((int) playerSubscriptionDao.get().countByEventCollection(eventCollection.getId()));
        eventCollection.setEventCount((int) countLinkedEvents(eventCollection.getId()));
    }
}

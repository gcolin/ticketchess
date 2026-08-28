package com.github.gcolin.registration;

import com.github.gcolin.club.SeasonScope;
import com.github.gcolin.event.Event;
import com.github.gcolin.registration.PlayerPendingSubscription;
import com.github.gcolin.platform.Transactional;
import jakarta.persistence.TypedQuery;
import java.util.List;
import com.github.gcolin.platform.AbstractDao;

public class PlayerPendingSubscriptionDao extends AbstractDao<PlayerPendingSubscription> {

    public PlayerPendingSubscriptionDao() {
        super(PlayerPendingSubscription.class);
    }

    public PlayerPendingSubscription findByEventAndNrffe(Event event, String nrffe) {
        TypedQuery<PlayerPendingSubscription> query = em.createQuery(
                "SELECT e FROM PlayerPendingSubscription e where e.event = :event and e.nrFfe = :nrFfe",
                PlayerPendingSubscription.class);
        query.setParameter("event", event);
        query.setParameter("nrFfe", nrffe);
        return query.getResultStream().findFirst().orElse(null);
    }

    public PlayerPendingSubscription findOldestByEvent(Event event) {
        TypedQuery<PlayerPendingSubscription> query = em.createQuery(
                "SELECT e FROM PlayerPendingSubscription e where e.event = :event ORDER BY e.createdAt ASC, e.id ASC",
                PlayerPendingSubscription.class);
        query.setParameter("event", event);
        query.setMaxResults(1);
        return query.getResultStream().findFirst().orElse(null);
    }

    public List<PlayerPendingSubscription> findByEvent(Event event) {
        TypedQuery<PlayerPendingSubscription> query = em.createQuery(
                "SELECT e FROM PlayerPendingSubscription e where e.event = :event ORDER BY e.createdAt ASC, e.id ASC",
                PlayerPendingSubscription.class);
        query.setParameter("event", event);
        return query.getResultList();
    }

    public List<PlayerPendingSubscription> findAllWithEvent() {
        return findAllWithEvent(SeasonScope.all());
    }

    public List<PlayerPendingSubscription> findAllWithEvent(SeasonScope scope) {
        String jpql = "SELECT e FROM PlayerPendingSubscription e join fetch e.event";
        if (scope.isFiltered()) {
            jpql += " WHERE e.event.startDate >= :seasonStart AND e.event.startDate <= :seasonEnd";
        }
        jpql += " ORDER BY e.createdAt ASC, e.id ASC";
        TypedQuery<PlayerPendingSubscription> query = em.createQuery(jpql, PlayerPendingSubscription.class);
        bindSeasonScope(query, scope);
        return query.getResultList();
    }

    private void bindSeasonScope(jakarta.persistence.TypedQuery<?> query, SeasonScope scope) {
        if (scope.isFiltered()) {
            query.setParameter("seasonStart", scope.getStart());
            query.setParameter("seasonEnd", scope.getEnd());
        }
    }

    public List<PlayerPendingSubscription> findByEventCollection(Integer eventCollectionId) {
        TypedQuery<PlayerPendingSubscription> query = em.createQuery(
            "SELECT e FROM PlayerPendingSubscription e "
                + "join fetch e.event ev "
                + "where ev.eventCollection.id = :eventCollectionId "
                + "ORDER BY e.createdAt ASC, e.id ASC",
            PlayerPendingSubscription.class);
        query.setParameter("eventCollectionId", eventCollectionId);
        return query.getResultList();
    }

    public List<PlayerPendingSubscription> findByCreationUserWithEvent(String email) {
        TypedQuery<PlayerPendingSubscription> query = em.createQuery(
            "SELECT e FROM PlayerPendingSubscription e join fetch e.event "
                + "where e.creationUser = :creationUser ORDER BY e.event.id ASC, e.createdAt ASC, e.id ASC",
                PlayerPendingSubscription.class);
        query.setParameter("creationUser", email);
        return query.getResultList();
    }

    public PlayerPendingSubscription findByEventAndNrffeAndCreationUser(Event event, String nrffe, String creationUser) {
        TypedQuery<PlayerPendingSubscription> query = em.createQuery(
            "SELECT e FROM PlayerPendingSubscription e "
                + "where e.event = :event and e.nrFfe = :nrFfe and e.creationUser = :creationUser",
            PlayerPendingSubscription.class);
        query.setParameter("event", event);
        query.setParameter("nrFfe", nrffe);
        query.setParameter("creationUser", creationUser);
        return query.getResultStream().findFirst().orElse(null);
    }
}

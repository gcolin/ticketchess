package com.github.gcolin.registration;

import com.github.gcolin.event.Event;
import com.github.gcolin.club.SeasonScope;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.platform.Transactional;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import jakarta.persistence.TypedQuery;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.github.gcolin.platform.AbstractDao;

public class PlayerSubscriptionDao extends AbstractDao<PlayerSubscription> {

    public PlayerSubscriptionDao() {
        super(PlayerSubscription.class);
    }

    public List<PlayerSubscription> findByCreationUser(String email) {
        TypedQuery<PlayerSubscription> query = em.createQuery(
                "SELECT e FROM PlayerSubscription e where e.creationUser = :creationUser", PlayerSubscription.class);
        query.setParameter("creationUser", email);
        return query.getResultList();
    }

    public List<PlayerSubscription> findByEvent(Event event) {
        TypedQuery<PlayerSubscription> query = em.createQuery(
                "SELECT e FROM PlayerSubscription e left join fetch e.payment where e.event = :event",
                PlayerSubscription.class);
        query.setParameter("event", event);
        return query.getResultList();
    }

    public List<PlayerSubscription> findByCreationUserWithEvents(String email) {
        TypedQuery<PlayerSubscription> query = em.createQuery(
                "SELECT e FROM PlayerSubscription e join fetch e.event where e.creationUser = :creationUser",
                PlayerSubscription.class);
        query.setParameter("creationUser", email);
        return query.getResultList();
    }

    public PlayerSubscription findByEventAndNrffe(Event event, String nrffe) {
        TypedQuery<PlayerSubscription> query = em.createQuery(
                "SELECT e FROM PlayerSubscription e where e.event = :event and e.nrFfe = :nrFfe and e.status != :status",
                PlayerSubscription.class);
        query.setParameter("event", event);
        query.setParameter("nrFfe", nrffe);
        query.setParameter("status", PlayerSubscriptionStatus.CANCELLED);
        return query.getResultStream().findFirst().orElse(null);
    }

    public long countByEvent(Event event) {
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(e) FROM PlayerSubscription e where e.event = :event and e.status != :status", Long.class);
        query.setParameter("event", event);
        query.setParameter("status", PlayerSubscriptionStatus.CANCELLED);
        return query.getSingleResult();
    }

    public Map<Integer, Long> countByEventIds(List<Integer> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }
        TypedQuery<Object[]> query = em.createQuery(
                "SELECT e.event.id, COUNT(e) FROM PlayerSubscription e "
                        + "WHERE e.event.id IN :eventIds AND e.status != :status "
                        + "GROUP BY e.event.id",
                Object[].class);
        query.setParameter("eventIds", eventIds);
        query.setParameter("status", PlayerSubscriptionStatus.CANCELLED);
        Map<Integer, Long> counts = new HashMap<>();
        for (Object[] row : query.getResultList()) {
            counts.put((Integer) row[0], (Long) row[1]);
        }
        return counts;
    }

        public long countByEventCollection(Integer eventCollectionId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(e) FROM PlayerSubscription e "
                + "WHERE e.event.eventCollection.id = :eventCollectionId and e.status != :status",
            Long.class);
        query.setParameter("eventCollectionId", eventCollectionId);
        query.setParameter("status", PlayerSubscriptionStatus.CANCELLED);
        return query.getSingleResult();
        }

    public PlayerSubscription findWithEvent(Integer id) {
        TypedQuery<PlayerSubscription> query = em.createQuery(
                "SELECT e FROM PlayerSubscription e join fetch e.event where e.id = :id", PlayerSubscription.class);
        query.setParameter("id", id);
        return query.getResultStream().findFirst().orElse(null);
    }

    public List<PlayerSubscription> findByPaymentId(Integer payment_id) {
        TypedQuery<PlayerSubscription> query = em.createQuery(
                "SELECT e FROM PlayerSubscription e join fetch e.event where e.payment.id = :payment_id",
                PlayerSubscription.class);
        query.setParameter("payment_id", payment_id);
        return query.getResultList();
    }

    public List<PlayerSubscription> findWithoutPaymentWithEvent(PlayerSubscriptionStatus status) {
        return findWithoutPaymentWithEvent(status, SeasonScope.all());
    }

    public List<PlayerSubscription> findWithoutPaymentWithEvent(PlayerSubscriptionStatus status, SeasonScope scope) {
        String jpql =
                "SELECT e FROM PlayerSubscription e join fetch e.event where e.payment is null and e.event.price > 0 and e.status = :status";
        if (scope.isFiltered()) {
            jpql += " and e.event.startDate >= :seasonStart and e.event.startDate <= :seasonEnd";
        }
        TypedQuery<PlayerSubscription> query = em.createQuery(jpql, PlayerSubscription.class);
        query.setParameter("status", status);
        bindSeasonScope(query, scope);
        return query.getResultList();
    }

    public List<PlayerSubscription> findCancelledWithEvent() {
        return findCancelledWithEvent(SeasonScope.all());
    }

    public List<PlayerSubscription> findCancelledWithEvent(SeasonScope scope) {
        String jpql = "SELECT e FROM PlayerSubscription e join fetch e.event where e.status = :status";
        if (scope.isFiltered()) {
            jpql += " and e.event.startDate >= :seasonStart and e.event.startDate <= :seasonEnd";
        }
        TypedQuery<PlayerSubscription> query = em.createQuery(jpql, PlayerSubscription.class);
        query.setParameter("status", PlayerSubscriptionStatus.CANCELLED);
        bindSeasonScope(query, scope);
        return query.getResultList();
    }

    public List<PlayerSubscription> findNotPaidWithEvent() {
        return findNotPaidWithEvent(SeasonScope.all());
    }

    public List<PlayerSubscription> findNotPaidWithEvent(SeasonScope scope) {
        String jpql = "SELECT e FROM PlayerSubscription e join fetch e.event where e.status = :status";
        if (scope.isFiltered()) {
            jpql += " and e.event.startDate >= :seasonStart and e.event.startDate <= :seasonEnd";
        }
        TypedQuery<PlayerSubscription> query = em.createQuery(jpql, PlayerSubscription.class);
        query.setParameter("status", PlayerSubscriptionStatus.NOT_PAID);
        bindSeasonScope(query, scope);
        return query.getResultList();
    }

    public List<String> findDistinctCreationUsers() {
        TypedQuery<String> query = em.createQuery(
                "SELECT DISTINCT e.creationUser FROM PlayerSubscription e WHERE e.creationUser IS NOT NULL AND e.creationUser <> '' ORDER BY e.creationUser",
                String.class);
        return query.getResultList();
    }

    public List<PlayerSubscription> findLinkedToCustomPlayers() {
        return findLinkedToCustomPlayers(SeasonScope.all());
    }

    public List<PlayerSubscription> findLinkedToCustomPlayers(SeasonScope scope) {
        String jpql =
                "SELECT e FROM PlayerSubscription e join fetch e.event WHERE e.nrFfe LIKE :customRef";
        if (scope.isFiltered()) {
            jpql += " and e.event.startDate >= :seasonStart and e.event.startDate <= :seasonEnd";
        }
        jpql += " ORDER BY e.createdAt DESC";
        TypedQuery<PlayerSubscription> query = em.createQuery(jpql, PlayerSubscription.class);
        query.setParameter("customRef", "@%");
        bindSeasonScope(query, scope);
        return query.getResultList();
    }

    public List<PlayerSubscription> findByEventCollection(Integer eventCollectionId) {
        TypedQuery<PlayerSubscription> query = em.createQuery(
                "SELECT e FROM PlayerSubscription e join fetch e.event left join fetch e.payment"
                        + " WHERE e.event.eventCollection.id = :eventCollectionId",
                PlayerSubscription.class);
        query.setParameter("eventCollectionId", eventCollectionId);
        return query.getResultList();
    }

        public Set<String> findActiveRefsByEventCollection(Integer eventCollectionId) {
        TypedQuery<String> query = em.createQuery(
            "SELECT DISTINCT e.nrFfe FROM PlayerSubscription e "
                + "WHERE e.event.eventCollection.id = :eventCollectionId and e.status != :status",
            String.class);
        query.setParameter("eventCollectionId", eventCollectionId);
        query.setParameter("status", PlayerSubscriptionStatus.CANCELLED);
            return new HashSet<>(query.getResultList());
        }

        public boolean existsActiveByEventCollectionAndNrffe(Integer eventCollectionId, String nrffe) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(e) FROM PlayerSubscription e "
                + "WHERE e.event.eventCollection.id = :eventCollectionId "
                + "and e.nrFfe = :nrFfe and e.status != :status",
            Long.class);
        query.setParameter("eventCollectionId", eventCollectionId);
        query.setParameter("nrFfe", nrffe);
        query.setParameter("status", PlayerSubscriptionStatus.CANCELLED);
        return query.getSingleResult() > 0;
        }

    private void bindSeasonScope(TypedQuery<?> query, SeasonScope scope) {
        if (scope.isFiltered()) {
            query.setParameter("seasonStart", scope.getStart());
            query.setParameter("seasonEnd", scope.getEnd());
        }
    }
}

package com.github.gcolin.registration;

import com.github.gcolin.registration.PlayerSubscriptionOption;
import com.github.gcolin.platform.Transactional;
import com.github.gcolin.registration.PlayerSubscriptionOptionStatus;
import jakarta.persistence.TypedQuery;
import java.util.List;
import com.github.gcolin.platform.AbstractDao;

public class PlayerSubscriptionOptionDao extends AbstractDao<PlayerSubscriptionOption> {

    public PlayerSubscriptionOptionDao() {
        super(PlayerSubscriptionOption.class);
    }

    public List<PlayerSubscriptionOption> findByPlayerSubscription(Integer subId) {
        TypedQuery<PlayerSubscriptionOption> query = em.createQuery(
                "SELECT e FROM PlayerSubscriptionOption e left join fetch e.payment"
                        + " WHERE e.playerSubscription.id = :subId ORDER BY e.id",
                PlayerSubscriptionOption.class);
        query.setParameter("subId", subId);
        return query.getResultList();
    }

    public List<PlayerSubscriptionOption> findByPaymentId(Integer paymentId) {
        TypedQuery<PlayerSubscriptionOption> query = em.createQuery(
                "SELECT e FROM PlayerSubscriptionOption e"
                        + " join fetch e.playerSubscription ps"
                        + " join fetch ps.event"
                        + " WHERE e.payment.id = :paymentId",
                PlayerSubscriptionOption.class);
        query.setParameter("paymentId", paymentId);
        return query.getResultList();
    }

    public List<PlayerSubscriptionOption> findByEvent(Integer eventId) {
        TypedQuery<PlayerSubscriptionOption> query = em.createQuery(
                "SELECT e FROM PlayerSubscriptionOption e"
                        + " join fetch e.playerSubscription ps"
                        + " left join fetch e.payment"
                        + " WHERE ps.event.id = :eventId ORDER BY ps.id, e.id",
                PlayerSubscriptionOption.class);
        query.setParameter("eventId", eventId);
        return query.getResultList();
    }

    public List<PlayerSubscriptionOption> findNotPaidByCreationUser(String email) {
        TypedQuery<PlayerSubscriptionOption> query = em.createQuery(
                "SELECT e FROM PlayerSubscriptionOption e"
                        + " join fetch e.playerSubscription ps"
                        + " WHERE ps.creationUser = :email AND e.status = :status",
                PlayerSubscriptionOption.class);
        query.setParameter("email", email);
        query.setParameter("status", PlayerSubscriptionOptionStatus.NOT_PAID);
        return query.getResultList();
    }

    public PlayerSubscriptionOption findWithSubscription(Integer id) {
        TypedQuery<PlayerSubscriptionOption> query = em.createQuery(
                "SELECT e FROM PlayerSubscriptionOption e"
                        + " join fetch e.playerSubscription ps"
                        + " join fetch ps.event"
                        + " left join fetch e.payment"
                        + " WHERE e.id = :id",
                PlayerSubscriptionOption.class);
        query.setParameter("id", id);
        return query.getResultStream().findFirst().orElse(null);
    }

    public void removeByPlayerSubscription(Integer subId) {
        List<PlayerSubscriptionOption> options = findByPlayerSubscription(subId);
        for (PlayerSubscriptionOption option : options) {
            remove(option);
        }
    }
}

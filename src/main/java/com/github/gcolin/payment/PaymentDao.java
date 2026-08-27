package com.github.gcolin.payment;

import com.github.gcolin.payment.Payment;
import com.github.gcolin.payment.PaymentStatus;
import com.github.gcolin.payment.PaymentType;
import com.github.gcolin.platform.Transactional;
import com.github.gcolin.platform.PagedList;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import java.util.List;
import com.github.gcolin.platform.AbstractDao;
import com.github.gcolin.registration.PlayerSubscription;

public class PaymentDao extends AbstractDao<Payment> {

    public PaymentDao() {
        super(Payment.class);
    }

    @Override
    public PagedList<Payment> page(int start, int pageSize) {
        TypedQuery<Payment> query =
                em.createQuery("SELECT e FROM Payment e ORDER BY e.id DESC", Payment.class);
        query.setFirstResult(start);
        query.setMaxResults(pageSize);
        List<Payment> list = query.getResultList();

        TypedQuery<Long> countQuery = em.createQuery("SELECT COUNT(e) FROM Payment e", Long.class);
        long total = countQuery.getSingleResult();

        return new PagedList<>(list, start, total);
    }

    public Payment findPendingByUser(String email) {
        TypedQuery<Payment> query = em.createQuery(
                "SELECT e FROM Payment e where e.userEmail = :email AND e.status = :status", Payment.class);
        query.setParameter("email", email);
        query.setParameter("status", PaymentStatus.PENDING);
        return query.getResultStream().findFirst().orElse(null);
    }

    public List<Payment> findAllPaidNotFreeByUser(String email) {
        TypedQuery<Payment> query = em.createQuery(
                "SELECT e FROM Payment e where e.userEmail = :email AND e.status = :status AND e.type <> :type ORDER BY e.id DESC",
                Payment.class);
        query.setParameter("email", email);
        query.setParameter("status", PaymentStatus.PAID);
        query.setParameter("type", PaymentType.FREE);
        return query.getResultList();
    }

    public Payment findBySessionId(String sessionId) {
        TypedQuery<Payment> query =
                em.createQuery("SELECT e FROM Payment e where e.stripeSessionId = :sessionId", Payment.class);
        query.setParameter("sessionId", sessionId);
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        return query.getResultStream().findFirst().orElse(null);
    }

    public Double sumAllPayments() {
        TypedQuery<Double> query = em.createQuery(
                "SELECT COALESCE(SUM(p.amount), 0.0) FROM Payment p WHERE p.status = :status", Double.class);
        query.setParameter("status", PaymentStatus.PAID);
        return query.getSingleResult();
    }

    public List<String> findDistinctUserEmails() {
        TypedQuery<String> query = em.createQuery(
                "SELECT DISTINCT p.userEmail FROM Payment p WHERE p.userEmail IS NOT NULL AND p.userEmail <> '' ORDER BY p.userEmail",
                String.class);
        return query.getResultList();
    }

    public PagedList<Payment> pageSearch(String search, int start, int pageSize) {
        String term = "%" + search.toLowerCase() + "%";
        TypedQuery<Payment> query = em.createQuery(
                "SELECT DISTINCT p FROM Payment p WHERE LOWER(p.userEmail) LIKE :term"
                        + " OR EXISTS (SELECT s FROM PlayerSubscription s WHERE s.payment = p AND LOWER(s.nrFfe) LIKE :term)"
                        + " ORDER BY p.id DESC",
                Payment.class);
        query.setParameter("term", term);
        query.setFirstResult(start);
        query.setMaxResults(pageSize);
        List<Payment> list = query.getResultList();

        TypedQuery<Long> countQuery = em.createQuery(
                "SELECT COUNT(DISTINCT p) FROM Payment p WHERE LOWER(p.userEmail) LIKE :term"
                        + " OR EXISTS (SELECT s FROM PlayerSubscription s WHERE s.payment = p AND LOWER(s.nrFfe) LIKE :term)",
                Long.class);
        countQuery.setParameter("term", term);
        long total = countQuery.getSingleResult();

        return new PagedList<>(list, start, total);
    }
}

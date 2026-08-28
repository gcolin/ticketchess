package com.github.gcolin.payment;

import com.github.gcolin.club.SeasonScope;
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
        return page(start, pageSize, SeasonScope.all());
    }

    public PagedList<Payment> page(int start, int pageSize, SeasonScope scope) {
        String jpql = "SELECT e FROM Payment e" + paymentCreatedAtSeasonWhere("e", scope) + " ORDER BY e.id DESC";
        TypedQuery<Payment> query = em.createQuery(jpql, Payment.class);
        bindSeasonScope(query, scope);
        query.setFirstResult(start);
        query.setMaxResults(pageSize);
        List<Payment> list = query.getResultList();

        String countJpql = "SELECT COUNT(e) FROM Payment e" + paymentCreatedAtSeasonWhere("e", scope);
        TypedQuery<Long> countQuery = em.createQuery(countJpql, Long.class);
        bindSeasonScope(countQuery, scope);
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
        return sumAllPayments(SeasonScope.all());
    }

    public Double sumAllPayments(SeasonScope scope) {
        String jpql = "SELECT COALESCE(SUM(p.amount), 0.0) FROM Payment p WHERE p.status = :status"
                + paymentCreatedAtSeasonAnd("p", scope);
        TypedQuery<Double> query = em.createQuery(jpql, Double.class);
        query.setParameter("status", PaymentStatus.PAID);
        bindSeasonScope(query, scope);
        return query.getSingleResult();
    }

    public List<Payment> all(SeasonScope scope) {
        String jpql = "SELECT e FROM Payment e" + paymentCreatedAtSeasonWhere("e", scope) + " ORDER BY e.id DESC";
        TypedQuery<Payment> query = em.createQuery(jpql, Payment.class);
        bindSeasonScope(query, scope);
        return query.getResultList();
    }

    public List<Payment> findPaid(SeasonScope scope) {
        String jpql = "SELECT p FROM Payment p WHERE p.status = :status"
                + paymentCreatedAtSeasonAnd("p", scope)
                + " ORDER BY COALESCE(p.updatedAt, p.createdAt), p.id";
        TypedQuery<Payment> query = em.createQuery(jpql, Payment.class);
        query.setParameter("status", PaymentStatus.PAID);
        bindSeasonScope(query, scope);
        return query.getResultList();
    }

    public List<String> findDistinctUserEmails() {
        TypedQuery<String> query = em.createQuery(
                "SELECT DISTINCT p.userEmail FROM Payment p WHERE p.userEmail IS NOT NULL AND p.userEmail <> '' ORDER BY p.userEmail",
                String.class);
        return query.getResultList();
    }

    public PagedList<Payment> pageSearch(String search, int start, int pageSize) {
        return pageSearch(search, start, pageSize, SeasonScope.all());
    }

    public PagedList<Payment> pageSearch(String search, int start, int pageSize, SeasonScope scope) {
        String term = "%" + search.toLowerCase() + "%";
        String jpql = "SELECT DISTINCT p FROM Payment p WHERE (LOWER(p.userEmail) LIKE :term"
                + " OR EXISTS (SELECT s FROM PlayerSubscription s WHERE s.payment = p AND LOWER(s.nrFfe) LIKE :term))"
                + paymentCreatedAtSeasonAnd("p", scope)
                + " ORDER BY p.id DESC";
        TypedQuery<Payment> query = em.createQuery(jpql, Payment.class);
        query.setParameter("term", term);
        bindSeasonScope(query, scope);
        query.setFirstResult(start);
        query.setMaxResults(pageSize);
        List<Payment> list = query.getResultList();

        String countJpql = "SELECT COUNT(DISTINCT p) FROM Payment p WHERE (LOWER(p.userEmail) LIKE :term"
                + " OR EXISTS (SELECT s FROM PlayerSubscription s WHERE s.payment = p AND LOWER(s.nrFfe) LIKE :term))"
                + paymentCreatedAtSeasonAnd("p", scope);
        TypedQuery<Long> countQuery = em.createQuery(countJpql, Long.class);
        countQuery.setParameter("term", term);
        bindSeasonScope(countQuery, scope);
        long total = countQuery.getSingleResult();

        return new PagedList<>(list, start, total);
    }

    private String paymentCreatedAtSeasonWhere(String alias, SeasonScope scope) {
        if (!scope.isFiltered()) {
            return "";
        }
        return " WHERE " + alias + ".createdAt >= :seasonStart AND " + alias + ".createdAt <= :seasonEnd";
    }

    private String paymentCreatedAtSeasonAnd(String alias, SeasonScope scope) {
        if (!scope.isFiltered()) {
            return "";
        }
        return " AND " + alias + ".createdAt >= :seasonStart AND " + alias + ".createdAt <= :seasonEnd";
    }

    private void bindSeasonScope(TypedQuery<?> query, SeasonScope scope) {
        if (scope.isFiltered()) {
            query.setParameter("seasonStart", scope.getStart());
            query.setParameter("seasonEnd", scope.getEnd());
        }
    }
}

package com.github.gcolin.membership;

import com.github.gcolin.club.SeasonScope;
import com.github.gcolin.membership.Membership;
import com.github.gcolin.membership.MembershipStatus;
import com.github.gcolin.payment.PaymentStatus;
import jakarta.persistence.TypedQuery;
import java.util.List;
import com.github.gcolin.platform.AbstractDao;

public class MembershipDao extends AbstractDao<Membership> {

    /** Inscriptions ouvertes avant le début officiel de la saison (été). */
    private static final int PRE_SEASON_MONTHS = 5;

    public MembershipDao() {
        super(Membership.class);
    }

    public List<Membership> all(SeasonScope scope) {
        String jpql = "SELECT m FROM Membership m";
        if (scope.isSeasonIdFiltered() && scope.isFiltered()) {
            jpql += " WHERE (m.season.id = :seasonId"
                    + " OR (m.season IS NULL AND ("
                    + "(COALESCE(m.createdAt, m.updatedAt) >= :seasonStart"
                    + " AND COALESCE(m.createdAt, m.updatedAt) <= :seasonEnd)"
                    + " OR (COALESCE(m.createdAt, m.updatedAt) >= :seasonPreStart"
                    + " AND COALESCE(m.createdAt, m.updatedAt) < :seasonStart))))";
        } else if (scope.isSeasonIdFiltered()) {
            jpql += " WHERE m.season.id = :seasonId";
        } else if (scope.isFiltered()) {
            jpql += " WHERE ((COALESCE(m.createdAt, m.updatedAt) >= :seasonStart"
                    + " AND COALESCE(m.createdAt, m.updatedAt) <= :seasonEnd)"
                    + " OR (COALESCE(m.createdAt, m.updatedAt) >= :seasonPreStart"
                    + " AND COALESCE(m.createdAt, m.updatedAt) < :seasonStart))";
        }
        jpql += " ORDER BY m.id DESC";
        TypedQuery<Membership> query = em.createQuery(jpql, Membership.class);
        bindSeasonScope(query, scope);
        return query.getResultList();
    }

    public List<Membership> findByUser(String user) {
        return findByUser(user, SeasonScope.all());
    }

    public List<Membership> findByUser(String user, SeasonScope scope) {
        String jpql = "SELECT m FROM Membership m WHERE m.user = :user";
        if (scope.isSeasonIdFiltered() && scope.isFiltered()) {
            jpql += " AND (m.season.id = :seasonId"
                    + " OR (m.season IS NULL AND ("
                    + "(COALESCE(m.createdAt, m.updatedAt) >= :seasonStart"
                    + " AND COALESCE(m.createdAt, m.updatedAt) <= :seasonEnd)"
                    + " OR (COALESCE(m.createdAt, m.updatedAt) >= :seasonPreStart"
                    + " AND COALESCE(m.createdAt, m.updatedAt) < :seasonStart))))";
        } else if (scope.isSeasonIdFiltered()) {
            jpql += " AND m.season.id = :seasonId";
        } else if (scope.isFiltered()) {
            jpql += " AND ((COALESCE(m.createdAt, m.updatedAt) >= :seasonStart"
                    + " AND COALESCE(m.createdAt, m.updatedAt) <= :seasonEnd)"
                    + " OR (COALESCE(m.createdAt, m.updatedAt) >= :seasonPreStart"
                    + " AND COALESCE(m.createdAt, m.updatedAt) < :seasonStart))";
        }
        jpql += " ORDER BY m.id DESC";
        TypedQuery<Membership> query = em.createQuery(jpql, Membership.class);
        query.setParameter("user", user);
        bindSeasonScope(query, scope);
        return query.getResultList();
    }

    public List<Membership> findApprovedUnpaidByUser(String user, SeasonScope scope) {
        String jpql = "SELECT m FROM Membership m WHERE m.user = :user"
                + " AND m.status = :status AND m.amountCents > 0"
                + " AND NOT EXISTS (SELECT 1 FROM Payment p WHERE p = m.payment AND p.status = :paidStatus)";
        if (scope.isSeasonIdFiltered() && scope.isFiltered()) {
            jpql += " AND (m.season.id = :seasonId"
                    + " OR (m.season IS NULL AND ("
                    + "(COALESCE(m.createdAt, m.updatedAt) >= :seasonStart"
                    + " AND COALESCE(m.createdAt, m.updatedAt) <= :seasonEnd)"
                    + " OR (COALESCE(m.createdAt, m.updatedAt) >= :seasonPreStart"
                    + " AND COALESCE(m.createdAt, m.updatedAt) < :seasonStart))))";
        } else if (scope.isSeasonIdFiltered()) {
            jpql += " AND m.season.id = :seasonId";
        } else if (scope.isFiltered()) {
            jpql += " AND ((COALESCE(m.createdAt, m.updatedAt) >= :seasonStart"
                    + " AND COALESCE(m.createdAt, m.updatedAt) <= :seasonEnd)"
                    + " OR (COALESCE(m.createdAt, m.updatedAt) >= :seasonPreStart"
                    + " AND COALESCE(m.createdAt, m.updatedAt) < :seasonStart))";
        }
        jpql += " ORDER BY m.id ASC";
        TypedQuery<Membership> query = em.createQuery(jpql, Membership.class);
        query.setParameter("user", user);
        query.setParameter("status", MembershipStatus.APPROVED);
        query.setParameter("paidStatus", PaymentStatus.PAID);
        bindSeasonScope(query, scope);
        return query.getResultList();
    }

    public List<Membership> findByPaymentId(int paymentId) {
        TypedQuery<Membership> query = em.createQuery(
                "SELECT m FROM Membership m WHERE m.payment.id = :paymentId ORDER BY m.id", Membership.class);
        query.setParameter("paymentId", (long) paymentId);
        return query.getResultList();
    }

    private void bindSeasonScope(TypedQuery<?> query, SeasonScope scope) {
        if (scope.isSeasonIdFiltered()) {
            query.setParameter("seasonId", scope.getSeasonId());
            if (scope.isFiltered()) {
                bindSeasonDateRange(query, scope);
            }
        } else if (scope.isFiltered()) {
            bindSeasonDateRange(query, scope);
        }
    }

    private void bindSeasonDateRange(TypedQuery<?> query, SeasonScope scope) {
        query.setParameter("seasonStart", scope.getStart());
        query.setParameter("seasonEnd", scope.getEnd());
        query.setParameter("seasonPreStart", scope.getStart().minusMonths(PRE_SEASON_MONTHS));
    }
}

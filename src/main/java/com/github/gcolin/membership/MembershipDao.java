package com.github.gcolin.membership;

import com.github.gcolin.club.SeasonScope;
import com.github.gcolin.membership.Membership;
import com.github.gcolin.platform.Transactional;
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
        if (scope.isFiltered()) {
            jpql += " WHERE COALESCE(m.createdAt, m.updatedAt) >= :seasonStart"
                    + " AND COALESCE(m.createdAt, m.updatedAt) <= :seasonEnd";
        }
        jpql += " ORDER BY m.id DESC";
        TypedQuery<Membership> query = em.createQuery(jpql, Membership.class);
        bindSeasonScope(query, scope);
        return query.getResultList();
    }

    public List<Membership> findByUser(String user) {
        TypedQuery<Membership> query = em.createQuery(
                "SELECT m FROM Membership m WHERE m.user = :user ORDER BY m.id DESC", Membership.class);
        query.setParameter("user", user);
        return query.getResultList();
    }

    private void bindSeasonScope(TypedQuery<?> query, SeasonScope scope) {
        if (scope.isFiltered()) {
            query.setParameter("seasonStart", scope.getStart().minusMonths(PRE_SEASON_MONTHS));
            query.setParameter("seasonEnd", scope.getEnd());
        }
    }
}

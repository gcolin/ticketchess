package com.github.gcolin.membership;

import com.github.gcolin.club.SeasonScope;
import com.github.gcolin.membership.MembershipOption;
import com.github.gcolin.platform.Transactional;
import jakarta.persistence.TypedQuery;
import java.util.List;
import com.github.gcolin.platform.AbstractDao;

public class MembershipOptionDao extends AbstractDao<MembershipOption> {

    public MembershipOptionDao() {
        super(MembershipOption.class);
    }

    public List<MembershipOption> all(SeasonScope scope) {
        String jpql = "SELECT o FROM MembershipOption o";
        if (scope.isSeasonIdFiltered() && scope.isFiltered()) {
            jpql += " WHERE (o.season.id = :seasonId"
                    + " OR (o.season IS NULL AND o.createdAt >= :seasonStart AND o.createdAt <= :seasonEnd))";
        } else if (scope.isSeasonIdFiltered()) {
            jpql += " WHERE o.season.id = :seasonId";
        } else if (scope.isFiltered()) {
            jpql += " WHERE o.createdAt >= :seasonStart AND o.createdAt <= :seasonEnd";
        }
        jpql += " ORDER BY o.id DESC";
        TypedQuery<MembershipOption> query = em.createQuery(jpql, MembershipOption.class);
        bindSeasonScope(query, scope);
        return query.getResultList();
    }

    private void bindSeasonScope(TypedQuery<?> query, SeasonScope scope) {
        if (scope.isSeasonIdFiltered()) {
            query.setParameter("seasonId", scope.getSeasonId());
            if (scope.isFiltered()) {
                query.setParameter("seasonStart", scope.getStart());
                query.setParameter("seasonEnd", scope.getEnd());
            }
        } else if (scope.isFiltered()) {
            query.setParameter("seasonStart", scope.getStart());
            query.setParameter("seasonEnd", scope.getEnd());
        }
    }
}

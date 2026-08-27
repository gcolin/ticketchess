package com.github.gcolin.membership;

import com.github.gcolin.membership.MembershipOptionSubscription;
import com.github.gcolin.platform.Transactional;
import jakarta.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;
import com.github.gcolin.platform.AbstractDao;

public class MembershipOptionSubscriptionDao extends AbstractDao<MembershipOptionSubscription> {

    public MembershipOptionSubscriptionDao() {
        super(MembershipOptionSubscription.class);
    }

    public List<MembershipOptionSubscription> findByMembershipIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        TypedQuery<MembershipOptionSubscription> query = em.createQuery(
                "SELECT s FROM MembershipOptionSubscription s JOIN FETCH s.membershipOption WHERE s.membership.id IN :ids",
                MembershipOptionSubscription.class);
        query.setParameter("ids", ids);
        return query.getResultList();
    }
}

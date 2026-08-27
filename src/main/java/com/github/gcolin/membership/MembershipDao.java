package com.github.gcolin.membership;

import com.github.gcolin.membership.Membership;
import com.github.gcolin.platform.Transactional;
import jakarta.persistence.TypedQuery;
import java.util.List;
import com.github.gcolin.platform.AbstractDao;

public class MembershipDao extends AbstractDao<Membership> {

    public MembershipDao() {
        super(Membership.class);
    }

    public List<Membership> findByUser(String user) {
        TypedQuery<Membership> query = em.createQuery(
                "SELECT m FROM Membership m WHERE m.user = :user ORDER BY m.id DESC", Membership.class);
        query.setParameter("user", user);
        return query.getResultList();
    }
}

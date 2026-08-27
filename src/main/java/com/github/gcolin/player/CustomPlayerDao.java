package com.github.gcolin.player;

import com.github.gcolin.player.CustomPlayer;
import com.github.gcolin.platform.Transactional;
import com.github.gcolin.platform.PagedList;
import jakarta.persistence.TypedQuery;
import java.util.List;
import com.github.gcolin.platform.AbstractDao;
import com.github.gcolin.registration.PlayerSubscription;

public class CustomPlayerDao extends AbstractDao<CustomPlayer> {

    public CustomPlayerDao() {
        super(CustomPlayer.class);
    }

    public List<CustomPlayer> findByCreationUser(String email) {
        TypedQuery<CustomPlayer> query =
                em.createQuery("SELECT e FROM CustomPlayer e where e.creationUser = :creationUser", CustomPlayer.class);
        query.setParameter("creationUser", email);
        return query.getResultList();
    }

    public List<CustomPlayer> findWithoutSubscription() {
        TypedQuery<CustomPlayer> query = em.createQuery(
                "SELECT e FROM CustomPlayer e WHERE NOT EXISTS (SELECT 1 FROM PlayerSubscription s WHERE s.nrFfe = concat('@', e.id))",
                CustomPlayer.class);
        return query.getResultList();
    }

    public PagedList<CustomPlayer> pageSorted(int start, int pageSize) {
        TypedQuery<CustomPlayer> query =
                em.createQuery("SELECT e FROM CustomPlayer e ORDER BY e.name, e.firstname", CustomPlayer.class);
        query.setFirstResult(start);
        query.setMaxResults(pageSize);
        List<CustomPlayer> list = query.getResultList();

        TypedQuery<Long> countQuery = em.createQuery("SELECT COUNT(e) FROM CustomPlayer e", Long.class);
        long total = countQuery.getSingleResult();

        return new PagedList<>(list, start, total);
    }
}

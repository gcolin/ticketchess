package com.github.gcolin.membership;

import com.github.gcolin.platform.AbstractDao;
import jakarta.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;

public class MembershipOptionFileDao extends AbstractDao<MembershipOptionFile> {

    public MembershipOptionFileDao() {
        super(MembershipOptionFile.class);
    }

    public List<MembershipOptionFile> findByOptionId(Integer optionId) {
        if (optionId == null) {
            return Collections.emptyList();
        }
        TypedQuery<MembershipOptionFile> query = em.createQuery(
                "SELECT f FROM MembershipOptionFile f"
                        + " JOIN FETCH f.membershipOption"
                        + " WHERE f.membershipOption.id = :optionId"
                        + " ORDER BY f.createdAt DESC, f.id DESC",
                MembershipOptionFile.class);
        query.setParameter("optionId", optionId);
        return query.getResultList();
    }

    public List<MembershipOptionFile> findByOptionIds(List<Integer> optionIds) {
        if (optionIds == null || optionIds.isEmpty()) {
            return Collections.emptyList();
        }
        TypedQuery<MembershipOptionFile> query = em.createQuery(
                "SELECT f FROM MembershipOptionFile f"
                        + " JOIN FETCH f.membershipOption"
                        + " WHERE f.membershipOption.id IN :optionIds"
                        + " ORDER BY f.membershipOption.id, f.createdAt DESC, f.id DESC",
                MembershipOptionFile.class);
        query.setParameter("optionIds", optionIds);
        return query.getResultList();
    }

    public MembershipOptionFile findWithOption(Integer id) {
        if (id == null) {
            return null;
        }
        TypedQuery<MembershipOptionFile> query = em.createQuery(
                "SELECT f FROM MembershipOptionFile f"
                        + " JOIN FETCH f.membershipOption"
                        + " WHERE f.id = :id",
                MembershipOptionFile.class);
        query.setParameter("id", id);
        return query.getResultStream().findFirst().orElse(null);
    }
}

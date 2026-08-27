package com.github.gcolin.auth;

import com.github.gcolin.auth.AuthorizationScopeType;
import com.github.gcolin.auth.PermissionCode;
import com.github.gcolin.platform.Transactional;
import com.github.gcolin.auth.UserAuthorization;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.List;
import com.github.gcolin.platform.AbstractDao;

public class UserAuthorizationDao extends AbstractDao<UserAuthorization> {

    public UserAuthorizationDao() {
        super(UserAuthorization.class);
    }

    public List<UserAuthorization> allOrdered() {
        TypedQuery<UserAuthorization> query = em.createQuery(
                "SELECT ua FROM UserAuthorization ua ORDER BY ua.email, ua.permission, ua.scopeType, ua.scopeId",
                UserAuthorization.class);
        return query.getResultList();
    }

    public boolean hasActiveGlobalPermission(String email, PermissionCode permission) {
        if (email == null || permission == null) {
            return false;
        }
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(ua) FROM UserAuthorization ua"
                        + " WHERE ua.email = :email"
                        + " AND ua.permission = :permission"
                        + " AND ua.scopeType = :scopeType"
                        + " AND ua.active = true"
                        + " AND (ua.validUntil IS NULL OR ua.validUntil > :now)",
                Long.class);
        query.setParameter("email", email.trim().toLowerCase());
        query.setParameter("permission", permission);
        query.setParameter("scopeType", AuthorizationScopeType.GLOBAL);
        query.setParameter("now", LocalDateTime.now());
        return query.getSingleResult() > 0;
    }

    public UserAuthorization upsert(
            String email,
            PermissionCode permission,
            AuthorizationScopeType scopeType,
            Integer scopeId,
            LocalDateTime validUntil,
            String grantedBy) {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        String normalizedGrantedBy = grantedBy == null ? null : grantedBy.trim().toLowerCase();

        AuthorizationScopeType normalizedScopeType = scopeType == null ? AuthorizationScopeType.GLOBAL : scopeType;
        Integer normalizedScopeId = normalizedScopeType == AuthorizationScopeType.GLOBAL ? null : scopeId;

        UserAuthorization existing =
                findByNaturalKey(normalizedEmail, permission, normalizedScopeType, normalizedScopeId);
        if (existing == null) {
            existing = new UserAuthorization();
            existing.setEmail(normalizedEmail);
            existing.setPermission(permission);
            existing.setScopeType(normalizedScopeType);
            existing.setScopeId(normalizedScopeId);
        }

        existing.setActive(true);
        existing.setValidUntil(validUntil);
        existing.setGrantedBy(normalizedGrantedBy);

        if (existing.getId() == null) {
            persist(existing);
            return existing;
        }
        return merge(existing);
    }

    private UserAuthorization findByNaturalKey(
            String email, PermissionCode permission, AuthorizationScopeType scopeType, Integer scopeId) {
        TypedQuery<UserAuthorization> query = em.createQuery(
                "SELECT ua FROM UserAuthorization ua WHERE ua.email = :email"
                        + " AND ua.permission = :permission"
                        + " AND ua.scopeType = :scopeType"
                        + " AND ((:scopeId IS NULL AND ua.scopeId IS NULL) OR ua.scopeId = :scopeId)",
                UserAuthorization.class);
        query.setParameter("email", email);
        query.setParameter("permission", permission);
        query.setParameter("scopeType", scopeType);
        query.setParameter("scopeId", scopeId);
        return query.getResultStream().findFirst().orElse(null);
    }
}

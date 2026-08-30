package com.github.gcolin.auth;

import com.github.gcolin.platform.AbstractDao;
import com.github.gcolin.platform.Transactional;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class UserAuthorizationDao extends AbstractDao<UserAuthorization> {

    public UserAuthorizationDao() {
        super(UserAuthorization.class);
    }

    public List<UserAuthorization> allOrdered() {
        TypedQuery<UserAuthorization> query = em.createQuery(
                "SELECT ua FROM UserAuthorization ua ORDER BY ua.email, ua.role, ua.scopeType, ua.scopeId",
                UserAuthorization.class);
        return query.getResultList();
    }

    public Set<RoleCode> findActiveGlobalRoles(String email) {
        if (email == null || email.isBlank()) {
            return Set.of();
        }
        TypedQuery<RoleCode> query = em.createQuery(
                "SELECT ua.role FROM UserAuthorization ua"
                        + " WHERE ua.email = :email"
                        + " AND ua.scopeType = :scopeType"
                        + " AND ua.active = true"
                        + " AND (ua.validUntil IS NULL OR ua.validUntil > :now)",
                RoleCode.class);
        query.setParameter("email", email.trim().toLowerCase());
        query.setParameter("scopeType", AuthorizationScopeType.GLOBAL);
        query.setParameter("now", LocalDateTime.now());
        List<RoleCode> roles = query.getResultList();
        if (roles.isEmpty()) {
            return Set.of();
        }
        return EnumSet.copyOf(roles);
    }

    public UserAuthorization upsert(
            String email,
            RoleCode role,
            AuthorizationScopeType scopeType,
            Integer scopeId,
            LocalDateTime validUntil,
            String grantedBy) {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        String normalizedGrantedBy = grantedBy == null ? null : grantedBy.trim().toLowerCase();

        AuthorizationScopeType normalizedScopeType = scopeType == null ? AuthorizationScopeType.GLOBAL : scopeType;
        Integer normalizedScopeId = normalizedScopeType == AuthorizationScopeType.GLOBAL ? null : scopeId;

        UserAuthorization existing = findByNaturalKey(normalizedEmail, role, normalizedScopeType, normalizedScopeId);
        if (existing == null) {
            existing = new UserAuthorization();
            existing.setEmail(normalizedEmail);
            existing.setRole(role);
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
            String email, RoleCode role, AuthorizationScopeType scopeType, Integer scopeId) {
        TypedQuery<UserAuthorization> query = em.createQuery(
                "SELECT ua FROM UserAuthorization ua WHERE ua.email = :email"
                        + " AND ua.role = :role"
                        + " AND ua.scopeType = :scopeType"
                        + " AND ((:scopeId IS NULL AND ua.scopeId IS NULL) OR ua.scopeId = :scopeId)",
                UserAuthorization.class);
        query.setParameter("email", email);
        query.setParameter("role", role);
        query.setParameter("scopeType", scopeType);
        query.setParameter("scopeId", scopeId);
        return query.getResultStream().findFirst().orElse(null);
    }
}

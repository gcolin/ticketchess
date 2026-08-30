package com.github.gcolin.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.gcolin.platform.Config;
import java.util.EnumSet;
import java.util.Set;

public class RoleResolver {

    private final Config config;
    private final UserAuthorizationDao userAuthorizationDao;
    private final Cache<String, Boolean> roleCache;

    public RoleResolver(Config config, UserAuthorizationDao userAuthorizationDao, Cache<String, Boolean> roleCache) {
        this.config = config;
        this.userAuthorizationDao = userAuthorizationDao;
        this.roleCache = roleCache;
    }

    public boolean hasRole(String email, RoleCode required) {
        if (email == null || email.isBlank() || required == null) {
            return false;
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (config.getAdmins().contains(normalizedEmail)) {
            return true;
        }
        String cacheKey = normalizedEmail + "|" + required.name();
        Boolean cached = roleCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }
        Set<RoleCode> granted = userAuthorizationDao.findActiveGlobalRoles(normalizedEmail);
        boolean allowed = RoleCode.satisfies(granted, required);
        roleCache.put(cacheKey, allowed);
        return allowed;
    }

    public boolean hasAnyRole(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (config.getAdmins().contains(normalizedEmail)) {
            return true;
        }
        return !userAuthorizationDao.findActiveGlobalRoles(normalizedEmail).isEmpty();
    }

    public Set<RoleCode> activeRoles(String email) {
        if (email == null || email.isBlank()) {
            return Set.of();
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (config.getAdmins().contains(normalizedEmail)) {
            return EnumSet.allOf(RoleCode.class);
        }
        return userAuthorizationDao.findActiveGlobalRoles(normalizedEmail);
    }
}

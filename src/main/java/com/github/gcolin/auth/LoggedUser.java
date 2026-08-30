package com.github.gcolin.auth;

import com.github.gcolin.platform.Caches;
import com.github.gcolin.platform.Config;
import com.github.gcolin.payment.DebtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggedUser implements Serializable {

    private static final long serialVersionUID = 1L;
    private String username;
    private String email;
    private boolean logged = false;

    private transient Caches caches;
    private transient DebtService debtService;
    private transient HttpServletRequest request;
    private transient Config config;
    private transient RoleResolver roleResolver;

    private static final Logger logger = LoggerFactory.getLogger(LoggedUser.class);

    public static void wire(
            LoggedUser user,
            Caches caches,
            DebtService debtService,
            HttpServletRequest request,
            Config config,
            RoleResolver roleResolver) {
        user.caches = caches;
        user.debtService = debtService;
        user.request = request;
        user.config = config;
        user.roleResolver = roleResolver;
    }

    public void initFromCookies() {
        Claims claims = getClaims();
        if (claims == null) {
            return;
        }
        String newEmail = claims.getSubject();
        boolean identityChanged = newEmail != null && (email == null || !newEmail.equalsIgnoreCase(email));
        setLogged(true);
        setEmail(claims.getSubject());
        setUsername(claims.getIssuer());
        if (request != null && request.getSession(false) != null) {
            request.getSession(false).setAttribute("auth.email", getEmail());
        }
        if (identityChanged) {
            caches.getDebtCache().invalidateAll();
            caches.getRoleCache().invalidateAll();
        }
    }

    public Claims getClaims() {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("remember_me".equals(c.getName())) {
                    try {
                        Claims claims = Jwts.parser()
                                .verifyWith(config.getKeys())
                                .build()
                                .parseSignedClaims(c.getValue())
                                .getPayload();
                        return claims;
                    } catch (JwtException e) {
                        logger.error("jwt error", e);
                    }
                }
            }
        }
        return null;
    }

    public double getDebt() {
        if (email == null) {
            return 0;
        }
        Double result = caches.getDebtCache().getIfPresent(email);
        if (result == null) {
            result = debtService.calculateDebt(email);
            caches.getDebtCache().put(email, result);
        }
        return result;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isLogged() {
        return logged;
    }

    public void setLogged(boolean logged) {
        this.logged = logged;
    }

    public boolean isAdmin() {
        return hasRole(RoleCode.ADMIN);
    }

    /** @deprecated use {@link #hasRole(RoleCode)} */
    @Deprecated
    public void setAdmin(boolean admin) {
        // no-op: admin status is resolved from RoleResolver, not stored on session
    }

    public boolean hasRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return false;
        }
        try {
            return hasRole(RoleCode.valueOf(roleName.trim()));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean hasRole(RoleCode role) {
        if (roleResolver == null || email == null || role == null) {
            return false;
        }
        return roleResolver.hasRole(email, role);
    }

    /** @deprecated use {@link #hasRole(String)} */
    @Deprecated
    public boolean hasPermission(String permissionName) {
        return hasRole(permissionName);
    }

    public boolean canManageEvents() {
        return hasRole(RoleCode.EVENT_ADMIN);
    }

    public boolean canManagePayments() {
        return hasRole(RoleCode.TRESORIER);
    }

    public boolean canManageMemberships() {
        return hasRole(RoleCode.TRESORIER);
    }

    public boolean canSeeAdminMenu() {
        return roleResolver != null && email != null && roleResolver.hasAnyRole(email);
    }

    public boolean canViewEventConfig() {
        return hasRole(RoleCode.ARBITRE);
    }
}

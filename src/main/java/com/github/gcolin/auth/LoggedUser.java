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
    private boolean admin = false;

    private transient Caches caches;
    private transient DebtService debtService;
    private transient HttpServletRequest request;
    private transient Config config;
    private transient UserAuthorizationDao userAuthorizationDao;

    private static final Logger logger = LoggerFactory.getLogger(LoggedUser.class);

    public static void wire(
            LoggedUser user,
            Caches caches,
            DebtService debtService,
            HttpServletRequest request,
            Config config,
            UserAuthorizationDao userAuthorizationDao) {
        user.caches = caches;
        user.debtService = debtService;
        user.request = request;
        user.config = config;
        user.userAuthorizationDao = userAuthorizationDao;
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
        setAdmin(resolveAdmin(claims));
        if (request != null && request.getSession(false) != null) {
            request.getSession(false).setAttribute("auth.email", getEmail());
            request.getSession(false).setAttribute("auth.admin", isAdmin());
        }
        if (identityChanged) {
            caches.getDebtCache().invalidateAll();
            caches.getPermissionCache().invalidateAll();
        }
    }

    private boolean resolveAdmin(Claims claims) {
        Object adminClaim = claims.get("admin");
        if (adminClaim instanceof Boolean admin) {
            return admin;
        }
        if (adminClaim != null) {
            return Boolean.parseBoolean(adminClaim.toString());
        }
        return config.getAdmins().contains(claims.getSubject());
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
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean hasPermission(String permissionName) {
        if (permissionName == null || permissionName.isBlank()) {
            return false;
        }
        try {
            boolean perm = hasPermission(PermissionCode.valueOf(permissionName.trim()));
            return perm;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean hasPermission(PermissionCode permission) {
        if (isAdmin()) {
            return true;
        }
        if (email == null || permission == null) {
            return false;
        }
        String cacheKey = email.trim().toLowerCase() + "|" + permission.name();
        Boolean cached = caches.getPermissionCache().getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }
        boolean allowed = userAuthorizationDao.hasActiveGlobalPermission(email, permission);
        caches.getPermissionCache().put(cacheKey, allowed);
        return allowed;
    }

    public boolean canManageEvents() {
        return hasPermission(PermissionCode.EVENT_CREATE)
                || hasPermission(PermissionCode.EVENT_EDIT)
                || hasPermission(PermissionCode.EVENT_DELETE);
    }

    public boolean canManagePayments() {
        return hasPermission(PermissionCode.PAYMENT_READ) || hasPermission(PermissionCode.PAYMENT_WRITE);
    }

    public boolean canSeeAdminMenu() {
        return hasPermission(PermissionCode.ADMIN_PANEL)
                || canManageEvents()
                || canManagePayments()
                || hasPermission(PermissionCode.USER_IMPERSONATE)
                || hasPermission(PermissionCode.MAIL_SEND);
    }
}

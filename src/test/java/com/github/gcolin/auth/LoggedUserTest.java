package com.github.gcolin.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.gcolin.payment.DebtService;
import com.github.gcolin.platform.Caches;
import com.github.gcolin.platform.Config;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Set;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

public class LoggedUserTest {

    private LoggedUser loggedUser;
    private Caches caches;
    private DebtService debtService;
    private HttpServletRequest request;
    private Config config;
    private UserAuthorizationDao userAuthorizationDao;
    private RoleResolver roleResolver;

    @BeforeEach
    void setUp() {
        caches = Mockito.mock(Caches.class);
        debtService = Mockito.mock(DebtService.class);
        request = Mockito.mock(HttpServletRequest.class);
        config = Mockito.mock(Config.class);
        userAuthorizationDao = Mockito.mock(UserAuthorizationDao.class);

        Cache<String, Boolean> roleCache = Mockito.mock(Cache.class);
        Mockito.when(caches.getRoleCache()).thenReturn(roleCache);
        Mockito.when(caches.getPermissionCache()).thenReturn(roleCache);

        roleResolver = new RoleResolver(config, userAuthorizationDao, roleCache);
        loggedUser = new LoggedUser();
        LoggedUser.wire(loggedUser, caches, debtService, request, config, roleResolver);
        loggedUser.initFromCookies();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetDebtUsesCacheAndDebtService() {
        Cache<String, Double> debtCache = Mockito.mock(Cache.class);

        Mockito.when(caches.getDebtCache()).thenReturn(debtCache);
        Mockito.when(debtCache.getIfPresent("bob@example.com")).thenReturn(null);
        Mockito.when(debtService.calculateDebt("bob@example.com")).thenReturn(42.5);

        loggedUser.setEmail("bob@example.com");

        Assertions.assertEquals(42.5, loggedUser.getDebt());
        Mockito.verify(debtCache).put("bob@example.com", 42.5);
    }

    @Test
    public void testGetDebtReturnsZeroWhenEmailMissing() {
        Assertions.assertEquals(0.0, loggedUser.getDebt());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLegacyAdminClaimDoesNotBypassAuthorization() {
        SecretKey keys =
                Keys.hmacShaKeyFor("9f3b2a8c5d4e7f1b2c9a0d6e3f1b4c7a8d2e5f9c1b0a3d6e4f7c8b9a2d1e6f3b".getBytes());

        Mockito.when(config.getKeys()).thenReturn(keys);
        Mockito.when(config.getAdmins()).thenReturn(Collections.emptySet());
        Mockito.when(userAuthorizationDao.findActiveGlobalRoles("dev@test.com")).thenReturn(Collections.emptySet());

        String jwt = Jwts.builder()
                .subject("dev@test.com")
                .issuer("Dev")
                .claim("admin", true)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30))
                .signWith(config.getKeys(), Config.JWT_ALGORITHM)
                .compact();

        Cookie[] cookies = {new Cookie("remember_me", jwt)};
        Mockito.when(request.getCookies()).thenReturn(cookies);

        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(request.getSession(false)).thenReturn(session);

        Cache<String, Double> debtCache = Mockito.mock(Cache.class);
        Mockito.when(caches.getDebtCache()).thenReturn(debtCache);

        loggedUser = new LoggedUser();
        LoggedUser.wire(loggedUser, caches, debtService, request, config, roleResolver);
        loggedUser.initFromCookies();

        Assertions.assertTrue(loggedUser.isLogged());
        Assertions.assertFalse(loggedUser.isAdmin());
        Assertions.assertEquals("dev@test.com", loggedUser.getEmail());
    }

    @Test
    public void testSyncFromRememberMeCookieRestoresStaleSessionUser() {
        SecretKey keys =
                Keys.hmacShaKeyFor("9f3b2a8c5d4e7f1b2c9a0d6e3f1b4c7a8d2e5f9c1b0a3d6e4f7c8b9a2d1e6f3b".getBytes());

        Mockito.when(config.getKeys()).thenReturn(keys);
        Mockito.when(config.getAdmins()).thenReturn(Set.of("dev@test.com"));

        String jwt = Jwts.builder()
                .subject("dev@test.com")
                .issuer("Dev")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30))
                .signWith(config.getKeys(), Config.JWT_ALGORITHM)
                .compact();

        Cookie[] cookies = {new Cookie("remember_me", jwt)};
        Mockito.when(request.getCookies()).thenReturn(cookies);

        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(request.getSession(false)).thenReturn(session);

        Cache<String, Double> debtCache = Mockito.mock(Cache.class);
        Mockito.when(caches.getDebtCache()).thenReturn(debtCache);

        loggedUser.setLogged(false);

        loggedUser.initFromCookies();

        Assertions.assertTrue(loggedUser.isLogged());
        Assertions.assertTrue(loggedUser.isAdmin());
        Assertions.assertEquals("dev@test.com", loggedUser.getEmail());
    }

    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testInitFromCookiesUsesConfigAdminsNotJwt(boolean isAdmin) {
        SecretKey keys =
                Keys.hmacShaKeyFor("9f3b2a8c5d4e7f1b2c9a0d6e3f1b4c7a8d2e5f9c1b0a3d6e4f7c8b9a2d1e6f3b".getBytes());

        Set<String> admins = isAdmin ? Set.of("alice@example.com") : Collections.emptySet();
        Mockito.when(config.getKeys()).thenReturn(keys);
        Mockito.when(config.getAdmins()).thenReturn(admins);
        Mockito.when(userAuthorizationDao.findActiveGlobalRoles("alice@example.com"))
                .thenReturn(Collections.emptySet());

        String jwt = Jwts.builder()
                .subject("alice@example.com")
                .issuer("Alice")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30))
                .signWith(config.getKeys(), Config.JWT_ALGORITHM)
                .compact();

        Cookie[] cookies = {new Cookie("remember_me", jwt)};
        Mockito.when(request.getCookies()).thenReturn(cookies);

        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(request.getSession(false)).thenReturn(session);

        Cache<String, Double> debtCache = Mockito.mock(Cache.class);
        Mockito.when(caches.getDebtCache()).thenReturn(debtCache);

        loggedUser = new LoggedUser();
        LoggedUser.wire(loggedUser, caches, debtService, request, config, roleResolver);
        loggedUser.initFromCookies();

        Assertions.assertTrue(loggedUser.isLogged());
        Assertions.assertEquals(isAdmin, loggedUser.isAdmin());
        Assertions.assertEquals("alice@example.com", loggedUser.getEmail());
        Assertions.assertEquals("Alice", loggedUser.getUsername());
        Mockito.verify(debtCache).invalidateAll();
        Mockito.verify(session).setAttribute("auth.email", "alice@example.com");
    }

    @Test
    public void testEventAdminInheritsArbitreRole() {
        loggedUser.setEmail("arbiter@example.com");
        Mockito.when(config.getAdmins()).thenReturn(Collections.emptySet());
        Mockito.when(userAuthorizationDao.findActiveGlobalRoles("arbiter@example.com"))
                .thenReturn(EnumSet.of(RoleCode.EVENT_ADMIN));

        Assertions.assertTrue(loggedUser.hasRole(RoleCode.ARBITRE));
        Assertions.assertTrue(loggedUser.canViewEventConfig());
        Assertions.assertFalse(loggedUser.hasRole(RoleCode.TRESORIER));
    }
}

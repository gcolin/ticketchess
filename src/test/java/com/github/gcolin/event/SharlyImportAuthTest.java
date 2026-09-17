package com.github.gcolin.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.gcolin.platform.Config;
import io.jsonwebtoken.Claims;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SharlyImportAuthTest {

    private Config config;
    private SharlyImportTokenService tokenService;
    private SharlyCallbackAllowlist allowlist;

    @BeforeEach
    void setUp() {
        config = mock(Config.class);
        SecretKey key = new SecretKeySpec(
                "integration-test-jwt-key-not-for-production-use-only".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
        when(config.getKeys()).thenReturn(key);
        when(config.getProperty("sharly.callback.origins", "")).thenReturn("https://arbitre.example.org");
        tokenService = new SharlyImportTokenService(config);
        allowlist = new SharlyCallbackAllowlist(config);
    }

    @Test
    void mintAndParseRoundTrip() throws ChessEventException {
        String jwt = tokenService.mint("admin@test.com", "open_2026", "Open 2026", 10, null);
        Claims claims = tokenService.parseSharlyToken(jwt);
        assertEquals("admin@test.com", claims.getSubject());
        assertEquals("sharly", claims.get("scope"));
        assertEquals("open_2026", claims.get("event_id", String.class));
        assertEquals("Open 2026", claims.get("name", String.class));
        assertEquals(10, claims.get("collectionId", Integer.class));
    }

    @Test
    void rejectWrongScope() {
        assertThrows(ChessEventException.class, () -> tokenService.parseSharlyToken("not-a-jwt"));
    }

    @Test
    void allowlistLocalhostAndConfiguredOrigin() {
        assertTrue(allowlist.isAllowed("http://127.0.0.1:8880/ticketchess/callback"));
        assertTrue(allowlist.isAllowed("http://localhost:8880/ticketchess/callback"));
        assertTrue(allowlist.isAllowed("https://arbitre.example.org/ticketchess/callback"));
        assertFalse(allowlist.isAllowed("https://evil.example/callback"));
        assertFalse(allowlist.isAllowed("/relative"));
    }

    @Test
    void extractBearer() {
        assertEquals("abc", SharlyImportTokenService.extractBearer("Bearer abc"));
        assertEquals("abc", SharlyImportTokenService.extractBearer("bearer abc"));
        assertEquals(null, SharlyImportTokenService.extractBearer(null));
    }
}

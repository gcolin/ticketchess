package com.github.gcolin.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.gcolin.platform.Caches;
import com.github.gcolin.platform.Config;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class LoginApiTest {

    @Test
    void shouldRejectExternalRedirectUri() throws Exception {
        LoginApi api = new LoginApi();
        Config config = mock(Config.class);
        LoggedUser loggedUser = new LoggedUser();
        Caches caches = new Caches();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        UriInfo uriInfo = mock(UriInfo.class);

        SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                "01234567890123456789012345678901234567890123456789012".getBytes(StandardCharsets.UTF_8));
        String jwt = Jwts.builder()
                .subject("user@test.com")
                .issuer("User")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();

        when(config.getKeys()).thenReturn(key);
        when(config.getAdmins()).thenReturn(java.util.Set.of());
        when(request.getSession(true)).thenReturn(session);
        when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost:8080/"));
        when(uriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri("http://localhost:8080/"));

        inject(api, "config", config);
        inject(api, "loggedUser", loggedUser);
        inject(api, "caches", caches);
        inject(api, "request", request);
        inject(api, "uriInfo", uriInfo);

        Response response = api.loginByMail(jwt, "https://evil.example/phish");

        assertEquals(303, response.getStatus());
        assertEquals(URI.create("http://localhost:8080/"), response.getLocation());
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

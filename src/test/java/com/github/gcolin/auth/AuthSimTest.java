package com.github.gcolin.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.gcolin.platform.Config;
import com.github.gcolin.platform.Caches;
import com.github.gcolin.auth.LoggedUser;
import io.jsonwebtoken.security.Keys;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class AuthSimTest {

    @Test
    void doGetShouldRejectWhenOauthClientConfigured() throws Exception {
        AuthSim api = new AuthSim();
        Config config = mock(Config.class);
        when(config.isOauthEnabled()).thenReturn(true);

        inject(api, "config", config);

        assertThrows(WebApplicationException.class, () -> api.doGet(null));
    }

    @Test
    void doGetShouldRejectWhenNotInTestMode() throws Exception {
        AuthSim api = new AuthSim();
        Config config = mock(Config.class);
        when(config.isOauthEnabled()).thenReturn(false);
        when(config.isAuthSimEnabled()).thenReturn(false);

        inject(api, "config", config);

        assertThrows(WebApplicationException.class, () -> api.doGet(null));
    }

    @Test
    void doGetShouldRedirectToDecodedState() throws Exception {
        AuthSim api = new AuthSim();
        Properties props = new Properties();
        props.setProperty("auth.USER_EMAIL", "u@test.com");
        props.setProperty("auth.USER_NAME", "User");

        Config config = mock(Config.class);
        when(config.isOauthEnabled()).thenReturn(false);
        when(config.isAuthSimEnabled()).thenReturn(true);
        when(config.getKeys()).thenReturn(Keys.hmacShaKeyFor("12345678901234567890123456789012".getBytes()));

        inject(api, "properties", props);
        inject(api, "config", config);
        inject(api, "loggerUser", new LoggedUser());
        inject(api, "caches", new Caches());
        inject(
                api,
                "uriInfo",
                uriInfoTarget(URI.create("http://localhost:8080/event/my"), URI.create("http://localhost:8080/")));

        String state = Base64.getUrlEncoder().encodeToString("event/my".getBytes(StandardCharsets.UTF_8));
        Response response = api.doGet(state);

        assertEquals(303, response.getStatus());
        assertEquals(URI.create("http://localhost:8080/event/my"), response.getLocation());
        assertNotNull(response.getCookies().get("remember_me"));
    }

    private static UriInfo uriInfoTarget(URI target, URI base) {
        UriInfo uriInfo = mock(UriInfo.class);
        UriBuilder builder = mock(UriBuilder.class);
        when(uriInfo.getBaseUriBuilder()).thenReturn(builder);
        when(uriInfo.getBaseUri()).thenReturn(base);
        when(builder.path("event/my")).thenReturn(builder);
        when(builder.build()).thenReturn(target);
        return uriInfo;
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

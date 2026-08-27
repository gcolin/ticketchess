package com.github.gcolin.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.gcolin.platform.Config;
import com.github.gcolin.auth.ActiveLoggedUsers;
import com.github.gcolin.auth.LoggedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class LogoutApiTest {

    @Test
    void logoutShouldInvalidateSessionAndRedirect() throws Exception {
        LogoutApi api = new LogoutApi();

        HttpSession session = mock(HttpSession.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        LoggedUser loggedUser = mock(LoggedUser.class);
        Config config = mock(Config.class);
        ActiveLoggedUsers activeLoggedUsers = mock(ActiveLoggedUsers.class);
        UriInfo uriInfo = mock(UriInfo.class);

        when(request.getSession(false)).thenReturn(session);
        when(session.getId()).thenReturn("session-id-123");
        when(loggedUser.getClaims()).thenReturn(null);
        when(config.getLoginUrl()).thenReturn("http://localhost:8080");
        when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost:8080"));

        inject(api, "request", request);
        inject(api, "loggerUser", loggedUser);
        inject(api, "config", config);
        inject(api, "activeLoggedUsers", activeLoggedUsers);
        inject(api, "uriInfo", uriInfo);
        inject(api, "properties", new Properties());

        Response response = api.logout();

        assertEquals(303, response.getStatus());
        verify(session).invalidate();
        verify(activeLoggedUsers).remove("session-id-123");
        assertNotNull(response.getCookies().get("remember_me"));
    }

    @Test
    void logoutShouldHandleNullSession() throws Exception {
        LogoutApi api = new LogoutApi();

        HttpServletRequest request = mock(HttpServletRequest.class);
        LoggedUser loggedUser = mock(LoggedUser.class);
        Config config = mock(Config.class);
        UriInfo uriInfo = mock(UriInfo.class);

        when(request.getSession(false)).thenReturn(null);
        when(loggedUser.getClaims()).thenReturn(null);
        when(config.getLoginUrl()).thenReturn("http://localhost:8080");
        when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost:8080"));

        inject(api, "request", request);
        inject(api, "loggerUser", loggedUser);
        inject(api, "config", config);
        inject(api, "activeLoggedUsers", mock(ActiveLoggedUsers.class));
        inject(api, "uriInfo", uriInfo);
        inject(api, "properties", new Properties());

        Response response = api.logout();

        assertEquals(303, response.getStatus());
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

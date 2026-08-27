package com.github.gcolin.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.gcolin.player.CustomPlayer;
import com.github.gcolin.event.Event;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.platform.PagedList;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import com.github.gcolin.platform.Caches;
import com.github.gcolin.player.Find;
import com.github.gcolin.auth.LoggedUser;
import com.github.gcolin.registration.RegisterService;
import com.github.gcolin.player.CustomPlayerDao;
import com.github.gcolin.event.EventDao;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.github.gcolin.platform.JteHtml;

class CustomPlayerApiTest {

    @Test
    void playersShouldPaginateAndReturnModel() throws Exception {
        CustomPlayerApi api = new CustomPlayerApi();
        CustomPlayerDao dao = mock(CustomPlayerDao.class);

        CustomPlayer p = new CustomPlayer();
        p.setName("Test");

        PagedList<CustomPlayer> paged = new PagedList<>(List.of(p), 0, 50);
        when(dao.pageSorted(0, 25)).thenReturn(paged);

        inject(api, "customPlayerService", dao);

        JteHtml html = api.players(1, 25);
        Map<String, Object> model = html.getModel();

        assertEquals("player/players.jte", html.getTemplate());
        assertEquals(1, model.get("currentPage"));
        assertEquals(25, model.get("pageSize"));
        assertEquals(50L, model.get("totalItems"));
        assertTrue((Boolean) model.get("hasNext"));
        assertFalse((Boolean) model.get("hasPrev"));
    }

    @Test
    void newplayerShouldReturnTemplateWithDefaultPlayer() throws Exception {
        CustomPlayerApi api = new CustomPlayerApi();

        JteHtml html = api.newplayer(null);

        assertEquals("player/customplayer.jte", html.getTemplate());
        CustomPlayer player = (CustomPlayer) html.getModel().get("player");
        assertEquals("", player.getName());
        assertEquals("", player.getElo());
    }

    @Test
    void editWithIdShouldReturnPlayerFromDao() throws Exception {
        CustomPlayerApi api = new CustomPlayerApi();
        CustomPlayerDao dao = mock(CustomPlayerDao.class);

        CustomPlayer player = new CustomPlayer();
        player.setId(3);
        player.setName("Doe");

        when(dao.find(3)).thenReturn(player);

        inject(api, "customPlayerService", dao);

        JteHtml html = api.edit(3, null);

        assertEquals("player/customplayer.jte", html.getTemplate());
        assertEquals(player, html.getModel().get("player"));
    }

    @Test
    void saveShouldPersistNewCustomPlayerAndRedirectToMyEvents() throws Exception {
        CustomPlayerApi api = new CustomPlayerApi();
        CustomPlayerDao dao = mock(CustomPlayerDao.class);
        LoggedUser user = mock(LoggedUser.class);

        when(user.getEmail()).thenReturn("user@test.com");
        when(user.isAdmin()).thenReturn(false);

        inject(api, "customPlayerService", dao);
        inject(api, "loggerUser", user);
        inject(api, "caches", new Caches());
        inject(api, "registerService", mock(RegisterService.class));
        inject(api, "eventService", mock(EventDao.class));
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/event/my?success=register")));

        Response response = api.save(null, null, "LIC", "Smith", "1990", "John", "true", "1800");

        assertEquals(303, response.getStatus());
        verify(dao).persist(org.mockito.ArgumentMatchers.any(CustomPlayer.class));
    }

    @Test
    void saveShouldRegisterToEventWhenEventIdProvided() throws Exception {
        CustomPlayerApi api = new CustomPlayerApi();
        CustomPlayerDao dao = mock(CustomPlayerDao.class);
        LoggedUser user = mock(LoggedUser.class);
        RegisterService registerService = mock(RegisterService.class);
        EventDao eventDao = mock(EventDao.class);

        Event event = new Event();
        event.setId(5);

        PlayerSubscription sub = new PlayerSubscription();
        sub.setStatus(PlayerSubscriptionStatus.PAID);

        when(user.getEmail()).thenReturn("user@test.com");
        when(user.isAdmin()).thenReturn(false);
        when(eventDao.find(5)).thenReturn(event);
        when(registerService.registerPlayerToEvent(
                        org.mockito.ArgumentMatchers.eq(event),
                        org.mockito.ArgumentMatchers.any(CustomPlayer.class),
                        org.mockito.ArgumentMatchers.eq("user@test.com")))
                .thenReturn(sub);

        inject(api, "customPlayerService", dao);
        inject(api, "loggerUser", user);
        inject(api, "caches", new Caches());
        inject(api, "registerService", registerService);
        inject(api, "eventService", eventDao);
        inject(api, "find", mock(Find.class));
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/event/5?success=register")));

        Response response = api.save(null, 5, "LIC", "Smith", "1990", "John", "false", "1600");

        assertEquals(303, response.getStatus());
    }

    private static UriInfo mockUriInfo(URI target) {
        UriInfo uriInfo = mock(UriInfo.class);
        UriBuilder uriBuilder = mock(UriBuilder.class);

        when(uriInfo.getBaseUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(org.mockito.ArgumentMatchers.anyString())).thenReturn(uriBuilder);
        when(uriBuilder.queryParam(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(target);

        return uriInfo;
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

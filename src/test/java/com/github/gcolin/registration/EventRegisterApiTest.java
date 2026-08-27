package com.github.gcolin.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

import com.github.gcolin.event.Event;
import com.github.gcolin.event.EventCollection;
import com.github.gcolin.registration.PlayerPendingSubscription;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.player.LuceneDb;
import com.github.gcolin.player.DisplayPlayer;
import com.github.gcolin.player.Player;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import com.github.gcolin.platform.Caches;
import com.github.gcolin.player.Find;
import com.github.gcolin.auth.LoggedUser;
import com.github.gcolin.registration.RegisterService;
import com.github.gcolin.player.CustomPlayerDao;
import com.github.gcolin.event.EventDao;
import com.github.gcolin.registration.PlayerPendingSubscriptionDao;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import com.github.gcolin.registration.PlayerSubscriptionOptionDao;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import com.github.gcolin.platform.JteHtml;

class EventRegisterApiTest {

    @Test
    void registerWithEmptyQueryShouldExposeEmptyPlayersList() throws Exception {
        EventRegisterApi api = new EventRegisterApi();

        EventDao eventDao = mock(EventDao.class);
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        CustomPlayerDao customPlayerDao = mock(CustomPlayerDao.class);
        LoggedUser user = mock(LoggedUser.class);

        Event event = new Event();
        event.setId(1);

        when(user.getEmail()).thenReturn("user@test.com");
        when(eventDao.find(1)).thenReturn(event);
        when(subDao.findByCreationUserWithEvents("user@test.com")).thenReturn(List.of());
        when(customPlayerDao.findByCreationUser("user@test.com")).thenReturn(List.of());

        inject(api, "eventService", eventDao);
        inject(api, "playerSubscriptionService", subDao);
        inject(api, "customPlayerService", customPlayerDao);
        inject(api, "loggerUser", user);
        inject(api, "find", mock(Find.class));

        JteHtml html = api.register(1, "");
        Map<String, Object> model = html.getModel();

        assertEquals("registration/register.jte", html.getTemplate());
        assertTrue(((List<?>) model.get("players")).isEmpty());
    }

    @Test
    void registerWithQueryShouldFilterAndSortLucenePlayers() throws Exception {
        EventRegisterApi api = new EventRegisterApi();

        EventDao eventDao = mock(EventDao.class);
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        CustomPlayerDao customPlayerDao = mock(CustomPlayerDao.class);
        LoggedUser user = mock(LoggedUser.class);
        LuceneDb luceneDb = mock(LuceneDb.class);

        Event event = new Event();
        event.setId(2);

        PlayerSubscription existing = new PlayerSubscription();
        existing.setNrFfe("LIC2");
        existing.setStatus(PlayerSubscriptionStatus.NOT_PAID);

        Player p1 = new Player();
        p1.setNrffe("LIC1");
        p1.setName("Alpha");
        p1.setFirstname("A");
        p1.setCategory("SenM");

        Player p2 = new Player();
        p2.setNrffe("LIC2");
        p2.setName("Filtered");
        p2.setFirstname("B");
        p2.setCategory("SenM");

        Player p3 = new Player();
        p3.setNrffe("LIC3");
        p3.setName("Bravo");
        p3.setFirstname("C");
        p3.setCategory("SenM");

        when(user.getEmail()).thenReturn("user@test.com");
        when(eventDao.find(2)).thenReturn(event);
        when(subDao.findByEvent(event)).thenReturn(List.of(existing));
        when(luceneDb.searchJoueur("abc", 20, null)).thenReturn(List.of(p3, p2, p1));
        when(subDao.findByCreationUserWithEvents("user@test.com")).thenReturn(List.of());
        when(customPlayerDao.findByCreationUser("user@test.com")).thenReturn(List.of());

        inject(api, "eventService", eventDao);
        inject(api, "playerSubscriptionService", subDao);
        inject(api, "customPlayerService", customPlayerDao);
        inject(api, "loggerUser", user);
        inject(api, "luceneDb", luceneDb);
        inject(api, "find", mock(Find.class));

        JteHtml html = api.register(2, "abc");
        Map<String, Object> model = html.getModel();

        assertEquals("abc", model.get("query"));
        @SuppressWarnings("unchecked")
        List<DisplayPlayer> players = (List<DisplayPlayer>) model.get("players");
        assertEquals(2, players.size());
        assertEquals("Alpha", players.get(0).getName());
        assertEquals("Bravo", players.get(1).getName());
    }

    @Test
    void registerWithQueryShouldFilterPlayersAlreadyRegisteredInCollection() throws Exception {
        EventRegisterApi api = new EventRegisterApi();

        EventDao eventDao = mock(EventDao.class);
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        CustomPlayerDao customPlayerDao = mock(CustomPlayerDao.class);
        LoggedUser user = mock(LoggedUser.class);
        LuceneDb luceneDb = mock(LuceneDb.class);

        EventCollection collection = new EventCollection();
        collection.setId(12);

        Event event = new Event();
        event.setId(2);
        event.setEventCollection(collection);

        Player p1 = new Player();
        p1.setNrffe("LIC1");
        p1.setName("Alpha");
        p1.setFirstname("A");
        p1.setCategory("SenM");

        Player p2 = new Player();
        p2.setNrffe("LIC2");
        p2.setName("Filtered");
        p2.setFirstname("B");
        p2.setCategory("SenM");

        when(user.getEmail()).thenReturn("user@test.com");
        when(eventDao.find(2)).thenReturn(event);
        when(subDao.findByEvent(event)).thenReturn(List.of());
        when(subDao.findActiveRefsByEventCollection(12)).thenReturn(Set.of("LIC2"));
        when(luceneDb.searchJoueur("abc", 20, null)).thenReturn(List.of(p2, p1));
        when(subDao.findByCreationUserWithEvents("user@test.com")).thenReturn(List.of());
        when(customPlayerDao.findByCreationUser("user@test.com")).thenReturn(List.of());

        inject(api, "eventService", eventDao);
        inject(api, "playerSubscriptionService", subDao);
        inject(api, "customPlayerService", customPlayerDao);
        inject(api, "loggerUser", user);
        inject(api, "luceneDb", luceneDb);
        inject(api, "find", mock(Find.class));

        JteHtml html = api.register(2, "abc");
        Map<String, Object> model = html.getModel();

        @SuppressWarnings("unchecked")
        List<DisplayPlayer> players = (List<DisplayPlayer>) model.get("players");
        assertEquals(1, players.size());
        assertEquals("LIC1", players.get(0).getLicence());
    }

    @Test
    void registerSaveShouldThrowConflictWhenAlreadyRegistered() throws Exception {
        EventRegisterApi api = new EventRegisterApi();

        RegisterService registerService = mock(RegisterService.class);
        LoggedUser user = mock(LoggedUser.class);
        EventDao eventDao = mock(EventDao.class);
        PlayerPendingSubscriptionDao pendingDao = mock(PlayerPendingSubscriptionDao.class);
        Event event = new Event();
        event.setId(4);

        when(user.getEmail()).thenReturn("user@test.com");
        when(registerService.registerPlayerToEvent(4, "123", "user@test.com")).thenReturn(null);
        when(eventDao.find(4)).thenReturn(event);
        when(pendingDao.findByEventAndNrffe(event, "123")).thenReturn(null);

        inject(api, "registerService", registerService);
        inject(api, "loggerUser", user);
        inject(api, "eventService", eventDao);
        inject(api, "playerPendingSubscriptionDao", pendingDao);
        inject(api, "caches", new Caches());
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/event/my?success=register")));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> api.registerSave(4, "123"));

        assertEquals(409, ex.getResponse().getStatus());
    }

    @Test
    void registerSaveShouldRedirectToMyEventsWhenQueued() throws Exception {
        EventRegisterApi api = new EventRegisterApi();

        RegisterService registerService = mock(RegisterService.class);
        LoggedUser user = mock(LoggedUser.class);
        EventDao eventDao = mock(EventDao.class);
        PlayerPendingSubscriptionDao pendingDao = mock(PlayerPendingSubscriptionDao.class);

        Event event = new Event();
        event.setId(5);
        PlayerPendingSubscription pending = new PlayerPendingSubscription();
        pending.setEvent(event);
        pending.setNrFfe("123");

        when(user.getEmail()).thenReturn("user@test.com");
        when(registerService.registerPlayerToEvent(5, "123", "user@test.com")).thenReturn(null);
        when(eventDao.find(5)).thenReturn(event);
        when(pendingDao.findByEventAndNrffe(event, "123")).thenReturn(pending);

        inject(api, "registerService", registerService);
        inject(api, "loggerUser", user);
        inject(api, "eventService", eventDao);
        inject(api, "playerPendingSubscriptionDao", pendingDao);
        inject(api, "caches", new Caches());
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/event/my?success=pending")));

        Response response = api.registerSave(5, "123");

        assertEquals(303, response.getStatus());
        assertEquals(URI.create("http://localhost:8080/event/my?success=pending"), response.getLocation());
    }

    @Test
    void registerSaveShouldRedirectToEventWhenPaid() throws Exception {
        EventRegisterApi api = new EventRegisterApi();

        RegisterService registerService = mock(RegisterService.class);
        LoggedUser user = mock(LoggedUser.class);

        PlayerSubscription sub = new PlayerSubscription();
        sub.setStatus(PlayerSubscriptionStatus.PAID);
        Event event = new Event();
        event.setId(8);
        sub.setEvent(event);

        when(user.getEmail()).thenReturn("user@test.com");
        when(registerService.registerPlayerToEvent(8, "777", "user@test.com")).thenReturn(sub);

        inject(api, "registerService", registerService);
        inject(api, "loggerUser", user);
        inject(api, "caches", new Caches());
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/event/8?success=register")));

        Response response = api.registerSave(8, "777");

        assertEquals(303, response.getStatus());
        assertEquals(URI.create("http://localhost:8080/event/8?success=register"), response.getLocation());
    }

    @Test
    void registerSaveShouldRedirectToMyEventsWhenNotPaid() throws Exception {
        EventRegisterApi api = new EventRegisterApi();

        RegisterService registerService = mock(RegisterService.class);
        LoggedUser user = mock(LoggedUser.class);

        PlayerSubscription sub = new PlayerSubscription();
        sub.setStatus(PlayerSubscriptionStatus.NOT_PAID);
        Event event = new Event();
        event.setId(9);
        sub.setEvent(event);

        when(user.getEmail()).thenReturn("user@test.com");
        when(registerService.registerPlayerToEvent(9, "555", "user@test.com")).thenReturn(sub);

        inject(api, "registerService", registerService);
        inject(api, "loggerUser", user);
        inject(api, "caches", new Caches());
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/event/my?success=register")));

        Response response = api.registerSave(9, "555");

        assertEquals(303, response.getStatus());
        assertEquals(URI.create("http://localhost:8080/event/my?success=register"), response.getLocation());
    }

    @Test
    void registerEditShouldReturnSubInModel() throws Exception {
        EventRegisterApi api = new EventRegisterApi();
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        PlayerSubscriptionOptionDao optionDao = mock(PlayerSubscriptionOptionDao.class);

        PlayerSubscription sub = new PlayerSubscription();
        sub.setId(7);
        when(subDao.findWithEvent(7)).thenReturn(sub);
        when(optionDao.findByPlayerSubscription(7)).thenReturn(List.of());
        Find find = mock(Find.class);
        when(find.player(any(), any())).thenReturn(new DisplayPlayer());

        inject(api, "playerSubscriptionService", subDao);
        inject(api, "playerSubscriptionOptionService", optionDao);
        inject(api, "find", find);

        JteHtml html = api.registeredit(1, 7);

        assertEquals("registration/subEdit.jte", html.getTemplate());
        assertEquals(sub, html.getModel().get("sub"));
    }

    @Test
    void registerEditSaveToRemoveShouldDeleteAndRedirect() throws Exception {
        EventRegisterApi api = new EventRegisterApi();
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        PlayerSubscriptionOptionDao optionDao = mock(PlayerSubscriptionOptionDao.class);
        RegisterService registerService = mock(RegisterService.class);

        Event event = new Event();
        event.setId(9);
        PlayerSubscription sub = new PlayerSubscription();
        sub.setId(22);
        sub.setEvent(event);
        when(subDao.findWithEvent(22)).thenReturn(sub);

        inject(api, "playerSubscriptionService", subDao);
        inject(api, "playerSubscriptionOptionService", optionDao);
        inject(api, "registerService", registerService);
        inject(api, "caches", new Caches());
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/event/9")));

        Response response = api.registerEditSave(9, 9, 22, "true", "u@test.com", "777", "PAID", null);

        assertEquals(303, response.getStatus());
        assertEquals(URI.create("http://localhost:8080/event/9"), response.getLocation());
        verify(optionDao).removeByPlayerSubscription(22);
        verify(subDao).remove(22);
        verify(registerService).promoteNextPendingSubscription(event);
    }

    @Test
    void markAttendanceShouldSetAttendanceAtAndRedirect() throws Exception {
        EventRegisterApi api = new EventRegisterApi();
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        Caches caches = new Caches();

        Event event = new Event();
        event.setId(9);
        PlayerSubscription sub = new PlayerSubscription();
        sub.setId(22);
        sub.setEvent(event);

        when(subDao.findWithEvent(22)).thenReturn(sub);

        inject(api, "playerSubscriptionService", subDao);
        inject(api, "caches", caches);
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/event/9")));

        Response response = api.markAttendance(9, 22, true);

        assertEquals(303, response.getStatus());
        assertEquals(URI.create("http://localhost:8080/event/9"), response.getLocation());
        assertNotNull(sub.getAttendanceAt());
        verify(subDao).merge(sub);
    }

    @Test
    void markAttendanceShouldClearAttendanceWhenPresentIsFalse() throws Exception {
        EventRegisterApi api = new EventRegisterApi();
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        Caches caches = new Caches();

        Event event = new Event();
        event.setId(9);
        PlayerSubscription sub = new PlayerSubscription();
        sub.setId(22);
        sub.setEvent(event);
        sub.setAttendanceAt(java.time.LocalDateTime.now());

        when(subDao.findWithEvent(22)).thenReturn(sub);

        inject(api, "playerSubscriptionService", subDao);
        inject(api, "caches", caches);
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/event/9")));

        Response response = api.markAttendance(9, 22, false);

        assertEquals(303, response.getStatus());
        assertEquals(null, sub.getAttendanceAt());
        verify(subDao).merge(sub);
    }

    private static UriInfo mockUriInfo(URI target) {
        UriInfo uriInfo = mock(UriInfo.class);
        UriBuilder uriBuilder = mock(UriBuilder.class);

        when(uriInfo.getBaseUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        when(uriBuilder.queryParam(anyString(), anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(target);

        return uriInfo;
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

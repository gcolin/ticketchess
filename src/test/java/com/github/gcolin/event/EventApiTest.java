package com.github.gcolin.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.gcolin.platform.Config;
import com.github.gcolin.event.Event;
import com.github.gcolin.event.EventCollection;
import com.github.gcolin.event.EventCollectionOptionType;
import com.github.gcolin.event.EventInfo;
import com.github.gcolin.event.EventOption;
import com.github.gcolin.event.EventOptionType;
import com.github.gcolin.registration.PlayerPendingSubscription;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.player.DisplayPlayer;
import com.github.gcolin.event.EventStatus;
import com.github.gcolin.player.IPlayer;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import com.github.gcolin.platform.SelectItem;
import com.github.gcolin.platform.Caches;
import com.github.gcolin.club.ClubSeasonFilter;
import com.github.gcolin.club.SeasonScope;
import com.github.gcolin.event.EventGroupFilter;
import com.github.gcolin.player.Find;
import com.github.gcolin.auth.LoggedUser;
import com.github.gcolin.registration.RegisterService;
import com.github.gcolin.event.EventCollectionDao;
import com.github.gcolin.event.EventCollectionOptionDao;
import com.github.gcolin.event.EventDao;
import com.github.gcolin.event.EventGroupDao;
import com.github.gcolin.event.EventInfoDao;
import com.github.gcolin.event.EventOptionDao;
import com.github.gcolin.payment.PaymentDao;
import com.github.gcolin.registration.PlayerPendingSubscriptionDao;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import com.github.gcolin.event.EventCache;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.github.gcolin.platform.JteHtml;

class EventApiTest {

    @Test
    void eventsShouldUseCacheOnSecondCall() throws Exception {
        EventApi api = new EventApi();
        Caches caches = new Caches();
        EventDao eventDao = mock(EventDao.class);
        EventGroupFilter eventGroupFilter = mock(EventGroupFilter.class);
        ClubSeasonFilter clubSeasonFilter = mock(ClubSeasonFilter.class);

        Event event = new Event();
        event.setId(1);

        when(eventDao.findByStatus(eq(EventStatus.ACTIVE), any(SeasonScope.class))).thenReturn(List.of(event));
        when(eventGroupFilter.getAll(null)).thenReturn(List.<SelectItem>of());
        when(clubSeasonFilter.resolve(null)).thenReturn(SeasonScope.all());

        inject(api, "caches", caches);
        inject(api, "eventService", eventDao);
        inject(api, "eventGroupFilter", eventGroupFilter);
        inject(api, "clubSeasonFilter", clubSeasonFilter);

        JteHtml first = api.events("ACTIVE", null);
        JteHtml second = api.events("ACTIVE", null);

        assertEquals("event/events.jte", first.getTemplate());
        assertEquals("event/events.jte", second.getTemplate());
        verify(eventDao, times(1)).findByStatus(eq(EventStatus.ACTIVE), any(SeasonScope.class));
        verify(eventDao, times(1)).detachAll(anyList());
    }

    @Test
    void eventByIdShouldUseCacheOnSecondCall() throws Exception {
        EventApi api = new EventApi();
        Caches caches = new Caches();
        EventDao eventDao = mock(EventDao.class);

        Event event = new Event();
        event.setId(33);
        EventCache eventCache = new EventCache();
        eventCache.event = event;
        eventCache.players = List.of();
        eventCache.eventInfo = "details";

        when(eventDao.buildCache(33)).thenReturn(eventCache);

        inject(api, "caches", caches);
        inject(api, "eventService", eventDao);

        JteHtml first = api.event(33, "ok");
        JteHtml second = api.event(33, "ok");

        assertEquals("event/event.jte", first.getTemplate());
        assertEquals("event/event.jte", second.getTemplate());
        assertEquals(event, first.getModel().get("event"));
        assertEquals(List.of(), first.getModel().get("missingPlayers"));
        verify(eventDao, times(1)).buildCache(33);
    }

    @Test
    void eventByIdShouldHideCancelledPlayers() throws Exception {
        EventApi api = new EventApi();
        Caches caches = new Caches();
        EventDao eventDao = mock(EventDao.class);

        Event event = new Event();
        event.setId(44);
        PlayerSubscription active = new PlayerSubscription();
        active.setId(1);
        active.setStatus(PlayerSubscriptionStatus.PAID);
        PlayerSubscription cancelled = new PlayerSubscription();
        cancelled.setId(2);
        cancelled.setStatus(PlayerSubscriptionStatus.CANCELLED);

        EventCache eventCache = new EventCache();
        eventCache.event = event;
        eventCache.eventInfo = "details";
        eventCache.players = List.of();

        when(eventDao.buildCache(44)).thenReturn(eventCache);

        inject(api, "caches", caches);
        inject(api, "eventService", eventDao);

        JteHtml html = api.event(44, null);

        assertEquals("event/event.jte", html.getTemplate());
        assertEquals(event, html.getModel().get("event"));
    }

    @Test
    void editWithIdShouldIncludeEventInfoAndOptions() throws Exception {
        EventApi api = new EventApi();
        EventDao eventDao = mock(EventDao.class);
        EventInfoDao eventInfoDao = mock(EventInfoDao.class);
        EventOptionDao eventOptionDao = mock(EventOptionDao.class);
        EventCollectionDao eventCollectionDao = mock(EventCollectionDao.class);
        EventGroupDao eventGroupDao = mock(EventGroupDao.class);

        Event event = new Event();
        event.setId(5);
        event.setName("Open");

        EventInfo info = new EventInfo();
        info.setEvent(event);
        info.setDescription("hello");

        EventOption option = new EventOption();
        option.setOptionType(EventOptionType.FFE_ID);
        option.setValue("abc");

        when(eventDao.find(5)).thenReturn(event);
        when(eventInfoDao.findByEventId(5)).thenReturn(info);
        when(eventOptionDao.findByEventId(5)).thenReturn(List.of(option));
        when(eventCollectionDao.allOrdered()).thenReturn(List.of());
        when(eventGroupDao.all()).thenReturn(List.of());

        inject(api, "eventService", eventDao);
        inject(api, "eventInfoService", eventInfoDao);
        inject(api, "eventOptionService", eventOptionDao);
        inject(api, "eventGroupService", eventGroupDao);
        inject(api, "eventCollectionService", eventCollectionDao);

        JteHtml html = api.edit(5, "save");
        Map<String, Object> model = html.getModel();

        assertEquals("event/eventEdit.jte", html.getTemplate());
        assertEquals(event, model.get("event"));
        assertEquals(info, model.get("eventInfo"));
        assertEquals("save", model.get("success"));
        @SuppressWarnings("unchecked")
        Map<String, String> options = (Map<String, String>) model.get("eventOptions");
        assertEquals("abc", options.get("FFE_ID"));
    }

    @Test
    void editWithoutIdShouldReturnEventEditTemplate() {
        EventApi api = new EventApi();

        try {
            EventCollectionDao eventCollectionDao = mock(EventCollectionDao.class);
            EventGroupDao eventGroupDao = mock(EventGroupDao.class);
            when(eventCollectionDao.allOrdered()).thenReturn(List.of());
            when(eventGroupDao.all()).thenReturn(List.of());
            inject(api, "eventGroupService", eventGroupDao);
            inject(api, "eventCollectionService", eventCollectionDao);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        JteHtml html = api.edit(null, null);

        assertEquals("event/eventEdit.jte", html.getTemplate());
        assertTrue(html.getModel().get("event") instanceof Event);
    }

    @Test
    void postEventEditDescriptionOnlyShouldUpdateInfoAndRedirect() throws Exception {
        EventApi api = new EventApi();
        EventInfoDao eventInfoDao = mock(EventInfoDao.class);

        inject(api, "eventInfoService", eventInfoDao);
        inject(api, "eventOptionService", mock(EventOptionDao.class));
        inject(api, "eventService", mock(EventDao.class));
        inject(api, "caches", new Caches());
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/event/9/edit?success=save")));

        Response response = api.postEventEdit(
                true, false, 9, "desc", null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);

        assertEquals(303, response.getStatus());
        assertEquals(URI.create("http://localhost:8080/event/9/edit?success=save"), response.getLocation());
    }

    @Test
    void previewDescriptionShouldRenderMarkdown() {
        EventApi api = new EventApi();

        Response response = api.previewDescription(7, "**bold** text");

        assertEquals(200, response.getStatus());
        assertEquals("<p><strong>bold</strong> text</p>\n", response.getEntity());
    }

    @Test
    void postEventEditOptionsOnlyShouldUpdateOptionsAndRedirect() throws Exception {
        EventApi api = new EventApi();
        EventOptionDao optionDao = mock(EventOptionDao.class);

        inject(api, "eventInfoService", mock(EventInfoDao.class));
        inject(api, "eventOptionService", optionDao);
        inject(api, "eventService", mock(EventDao.class));
        inject(api, "caches", new Caches());
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/event/11/edit?success=save")));

        Response response = api.postEventEdit(
                false, true, 11, null, "login", null, null, "pwd", "ceUser", "cePwd", null, null, null, null, null, null, null,
                null, null, null, null, null, null, "1");

        assertEquals(303, response.getStatus());
        assertEquals(URI.create("http://localhost:8080/event/11/edit?success=save"), response.getLocation());
        verify(optionDao).setOption(11, EventOptionType.FFE_ID, "login");
        verify(optionDao).setOption(11, EventOptionType.FFE_PASSWORD, "pwd");
        verify(optionDao).setOption(11, EventOptionType.CHESS_EVENT_USER, "ceUser");
        verify(optionDao).setOption(11, EventOptionType.CHESS_EVENT_PASSWORD, "cePwd");
        verify(optionDao).setOption(11, EventOptionType.POINTAGE, "1");
    }

    @Test
    void unregisterShouldThrowWhenPlayerNotFound() throws Exception {
        EventApi api = new EventApi();
        Caches caches = new Caches();
        RegisterService registerService = mock(RegisterService.class);
        EventDao eventDao = mock(EventDao.class);
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        LoggedUser user = mock(LoggedUser.class);

        Event event = new Event();
        event.setId(12);

        when(eventDao.find(12)).thenReturn(event);
        when(subDao.findByEventAndNrffe(event, "A1")).thenReturn(null);
        when(user.getEmail()).thenReturn("u@test.com");

        inject(api, "caches", caches);
        inject(api, "registerService", registerService);
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/event/my")));
        inject(api, "eventService", eventDao);
        inject(api, "playerSubscriptionService", subDao);
        inject(api, "loggerUser", user);

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> api.unregister(12, "A1"));

        assertEquals(404, ex.getResponse().getStatus());
        assertNotNull(ex.getMessage());
    }

    @Test
    void unregisterShouldRejectOtherUsersSubscription() throws Exception {
        EventApi api = new EventApi();
        EventDao eventDao = mock(EventDao.class);
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        LoggedUser user = mock(LoggedUser.class);

        Event event = new Event();
        event.setId(12);
        PlayerSubscription sub = new PlayerSubscription();
        sub.setCreationUser("owner@test.com");

        when(eventDao.find(12)).thenReturn(event);
        when(subDao.findByEventAndNrffe(event, "A1")).thenReturn(sub);
        when(user.getEmail()).thenReturn("attacker@test.com");

        inject(api, "eventService", eventDao);
        inject(api, "playerSubscriptionService", subDao);
        inject(api, "loggerUser", user);

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> api.unregister(12, "A1"));

        assertEquals(403, ex.getResponse().getStatus());
    }

    @Test
    void unregisterShouldRedirectToMyEventsWhenSuccess() throws Exception {
        EventApi api = new EventApi();
        Caches caches = new Caches();
        RegisterService registerService = mock(RegisterService.class);
        EventDao eventDao = mock(EventDao.class);
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        LoggedUser user = mock(LoggedUser.class);

        Event event = new Event();
        event.setId(20);
        PlayerSubscription sub = new PlayerSubscription();
        sub.setCreationUser("u@test.com");

        EventCache cache = new EventCache();
        cache.event = event;
        caches.getEvent().put("20", cache);

        when(eventDao.find(20)).thenReturn(event);
        when(subDao.findByEventAndNrffe(event, "B2")).thenReturn(sub);
        when(user.getEmail()).thenReturn("u@test.com");
        when(registerService.unregisterPlayerToEvent(20, "B2")).thenReturn(true);

        inject(api, "caches", caches);
        inject(api, "registerService", registerService);
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/event/my")));
        inject(api, "eventService", eventDao);
        inject(api, "playerSubscriptionService", subDao);
        inject(api, "loggerUser", user);

        Response response = api.unregister(20, "B2");

        assertEquals(303, response.getStatus());
        assertEquals(URI.create("http://localhost:8080/event/my"), response.getLocation());
    }

    @Test
    void myeventsShouldExposePendingQueueEvents() throws Exception {
        EventApi api = new EventApi();

        PlayerSubscriptionDao subscriptionDao = mock(PlayerSubscriptionDao.class);
        PlayerPendingSubscriptionDao pendingDao = mock(PlayerPendingSubscriptionDao.class);
        LoggedUser user = mock(LoggedUser.class);
        Find find = mock(Find.class);
        PaymentDao paymentDao = mock(PaymentDao.class);

        Event event = new Event();
        event.setId(2);
        event.setName("Pending Event");

        PlayerPendingSubscription pending = new PlayerPendingSubscription();
        pending.setEvent(event);
        pending.setNrFfe("123456");
        pending.setCreationUser("u@test.com");

        IPlayer player = mock(IPlayer.class);
        when(player.getName()).thenReturn("Doe");
        when(player.getFirstname()).thenReturn("John");
        when(player.getCategory()).thenReturn("S");
        when(player.getLicence()).thenReturn("123456A");
        when(player.getNrffe()).thenReturn("123456");
        when(player.getFide()).thenReturn("");
        when(player.getRating()).thenReturn("1500");
        when(player.isEditable()).thenReturn(false);

        when(user.getEmail()).thenReturn("u@test.com");
        when(subscriptionDao.findByCreationUserWithEvents("u@test.com")).thenReturn(List.of());
        when(pendingDao.findByCreationUserWithEvent("u@test.com")).thenReturn(List.of(pending));
        when(find.player("123456", null)).thenReturn(player);
        when(paymentDao.findAllPaidNotFreeByUser("u@test.com")).thenReturn(List.of());

        inject(api, "playerSubscriptionService", subscriptionDao);
        inject(api, "playerPendingSubscriptionDao", pendingDao);
        inject(api, "loggerUser", user);
        inject(api, "find", find);
        inject(api, "paymentDao", paymentDao);
        inject(api, "config", mockConfig());
        inject(api, "properties", new java.util.Properties());
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/event/my?display=all")));

        JteHtml html = api.myevents(null, "all");

        assertEquals("event/myevents.jte", html.getTemplate());
        @SuppressWarnings("unchecked")
        List<Event> pendingEvents = (List<Event>) html.getModel().get("pendingEvents");
        assertEquals(1, pendingEvents.size());
        assertEquals("Pending Event", pendingEvents.get(0).getName());
        assertEquals(1, pendingEvents.get(0).getPlayers().size());
    }

    @Test
    void myeventsShouldUseCollectionQueuePositionWhenCollectionHasMaxSubscriptions() throws Exception {
        EventApi api = new EventApi();

        PlayerSubscriptionDao subscriptionDao = mock(PlayerSubscriptionDao.class);
        PlayerPendingSubscriptionDao pendingDao = mock(PlayerPendingSubscriptionDao.class);
        EventCollectionOptionDao eventCollectionOptionDao = mock(EventCollectionOptionDao.class);
        LoggedUser user = mock(LoggedUser.class);
        Find find = mock(Find.class);
        PaymentDao paymentDao = mock(PaymentDao.class);

        EventCollection eventCollection = new EventCollection();
        eventCollection.setId(10);

        Event eventA = new Event();
        eventA.setId(1);
        eventA.setName("Event A");
        eventA.setEventCollection(eventCollection);

        Event eventB = new Event();
        eventB.setId(2);
        eventB.setName("Event B");
        eventB.setEventCollection(eventCollection);

        PlayerPendingSubscription pendingAhead = new PlayerPendingSubscription();
        pendingAhead.setId(100);
        pendingAhead.setEvent(eventA);
        pendingAhead.setNrFfe("111111");
        pendingAhead.setCreationUser("other@test.com");

        PlayerPendingSubscription pendingCurrent = new PlayerPendingSubscription();
        pendingCurrent.setId(101);
        pendingCurrent.setEvent(eventB);
        pendingCurrent.setNrFfe("123456");
        pendingCurrent.setCreationUser("u@test.com");

        IPlayer player = mock(IPlayer.class);
        when(player.getName()).thenReturn("Doe");
        when(player.getFirstname()).thenReturn("John");
        when(player.getCategory()).thenReturn("S");
        when(player.getLicence()).thenReturn("123456A");
        when(player.getNrffe()).thenReturn("123456");
        when(player.getFide()).thenReturn("");
        when(player.getRating()).thenReturn("1500");
        when(player.isEditable()).thenReturn(false);

        when(user.getEmail()).thenReturn("u@test.com");
        when(subscriptionDao.findByCreationUserWithEvents("u@test.com")).thenReturn(List.of());
        when(pendingDao.findByCreationUserWithEvent("u@test.com")).thenReturn(List.of(pendingCurrent));
        when(pendingDao.findByEventCollection(10)).thenReturn(List.of(pendingAhead, pendingCurrent));
        when(eventCollectionOptionDao.findIntOptionValue(10, EventCollectionOptionType.MAX_SUBSCRIPTIONS))
                .thenReturn(5);
        when(find.player("123456", null)).thenReturn(player);
        when(paymentDao.findAllPaidNotFreeByUser("u@test.com")).thenReturn(List.of());

        inject(api, "playerSubscriptionService", subscriptionDao);
        inject(api, "playerPendingSubscriptionDao", pendingDao);
        inject(api, "eventCollectionOptionService", eventCollectionOptionDao);
        inject(api, "loggerUser", user);
        inject(api, "find", find);
        inject(api, "paymentDao", paymentDao);
        inject(api, "config", mockConfig());
        inject(api, "properties", new java.util.Properties());
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/event/my?display=all")));

        JteHtml html = api.myevents(null, "all");

        @SuppressWarnings("unchecked")
        List<Event> pendingEvents = (List<Event>) html.getModel().get("pendingEvents");
        assertEquals(1, pendingEvents.size());
        DisplayPlayer pendingPlayer = pendingEvents.get(0).getPlayers().get(0);
        assertEquals(1, pendingPlayer.getPendingQueueAhead());
    }

    @Test
    void unregisterPendingShouldRemoveAndRedirect() throws Exception {
        EventApi api = new EventApi();

        EventDao eventDao = mock(EventDao.class);
        PlayerPendingSubscriptionDao pendingDao = mock(PlayerPendingSubscriptionDao.class);
        LoggedUser user = mock(LoggedUser.class);

        Event event = new Event();
        event.setId(2);

        PlayerPendingSubscription pending = new PlayerPendingSubscription();
        pending.setEvent(event);
        pending.setNrFfe("123456");
        pending.setCreationUser("u@test.com");

        when(eventDao.find(2)).thenReturn(event);
        when(user.getEmail()).thenReturn("u@test.com");
        when(pendingDao.findByEventAndNrffeAndCreationUser(event, "123456", "u@test.com")).thenReturn(pending);

        inject(api, "eventService", eventDao);
        inject(api, "playerPendingSubscriptionDao", pendingDao);
        inject(api, "loggerUser", user);
        inject(api, "caches", new Caches());
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/event/my")));

        Response response = api.unregisterPending(2, "123456");

        assertEquals(303, response.getStatus());
        assertEquals(URI.create("http://localhost:8080/event/my"), response.getLocation());
        verify(pendingDao).remove(pending);
    }

    private static Config mockConfig() {
        Config config = mock(Config.class);
        when(config.getPendingQueueOffset()).thenReturn(0);
        return config;
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

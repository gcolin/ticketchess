package com.github.gcolin.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.gcolin.event.Event;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.player.IPlayer;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import com.github.gcolin.player.Find;
import com.github.gcolin.event.EventDao;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.github.gcolin.platform.JteHtml;
import com.github.gcolin.platform.SendMail;

class EventMailApiTest {

    @Test
    void showFormShouldReturnEventWithModel() throws Exception {
        EventMailApi api = new EventMailApi();
        EventDao eventDao = mock(EventDao.class);

        Event event = new Event();
        event.setId(1);
        event.setName("Rapid 2024");

        when(eventDao.find(1)).thenReturn(event);

        inject(api, "eventService", eventDao);

        JteHtml html = api.showForm(1, null);

        assertEquals("event/eventMail.jte", html.getTemplate());
        Map<String, Object> model = html.getModel();
        assertEquals(event, model.get("event"));
    }

    @Test
    void showFormShouldIncludeSentCountWhenProvided() throws Exception {
        EventMailApi api = new EventMailApi();
        EventDao eventDao = mock(EventDao.class);

        Event event = new Event();
        event.setId(1);
        event.setName("Blitz 2024");

        when(eventDao.find(1)).thenReturn(event);

        inject(api, "eventService", eventDao);

        JteHtml html = api.showForm(1, 5);

        Map<String, Object> model = html.getModel();
        assertEquals(5, model.get("sent"));
    }

    @Test
    void sendMailsShouldSendToMatchingSubscriptions() throws Exception {
        EventMailApi api = new EventMailApi();
        EventDao eventDao = mock(EventDao.class);
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        Find find = mock(Find.class);

        Event event = new Event();
        event.setId(1);
        event.setName("Test Event");

        PlayerSubscription sub = new PlayerSubscription();
        sub.setStatus(PlayerSubscriptionStatus.PAID);
        sub.setNrFfe("FFE123");
        sub.setCreationUser("user@test.com");

        IPlayer player = mock(IPlayer.class);
        when(player.getFullname()).thenReturn("John Doe");

        when(eventDao.find(1)).thenReturn(event);
        when(subDao.findByEvent(event)).thenReturn(List.of(sub));
        when(find.player("FFE123", null)).thenReturn(player);

        inject(api, "eventService", eventDao);
        inject(api, "playerSubscriptionService", subDao);
        inject(api, "find", find);
        inject(api, "sendMail", mock(com.github.gcolin.platform.SendMail.class));
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/event/1/mail?sent=1")));

        Response response = api.sendMails(1, "Test", "Body", null, null, null, null, new ArrayList<Integer>());

        assertEquals(303, response.getStatus());
    }

    @Test
    void sendMailsInBatchesShouldUseBccBatched() throws Exception {
        EventMailApi api = new EventMailApi();
        EventDao eventDao = mock(EventDao.class);
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        SendMail sendMail = mock(SendMail.class);

        Event event = new Event();
        event.setId(1);
        event.setName("Test Event");

        List<PlayerSubscription> subscriptions = new ArrayList<>();
        for (int i = 0; i < 35; i++) {
            PlayerSubscription sub = new PlayerSubscription();
            sub.setStatus(PlayerSubscriptionStatus.PAID);
            sub.setNrFfe("FFE" + i);
            sub.setCreationUser("user" + i + "@test.com");
            subscriptions.add(sub);
        }

        when(eventDao.find(1)).thenReturn(event);
        when(subDao.findByEvent(event)).thenReturn(subscriptions);
        when(sendMail.sendBccBatched(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.eq("Test"))).thenReturn(35);

        inject(api, "eventService", eventDao);
        inject(api, "playerSubscriptionService", subDao);
        inject(api, "sendMail", sendMail);
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/event/1/mail?sent=35")));

        Response response = api.sendMails(1, "Test", "Body", null, null, "true", null, new ArrayList<Integer>());

        assertEquals(303, response.getStatus());
        org.mockito.Mockito.verify(sendMail).sendBccBatched(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.argThat(list -> list.size() == 35),
                org.mockito.ArgumentMatchers.eq("Test"));
    }

    private static UriInfo mockUriInfo(URI target) {
        UriInfo uriInfo = mock(UriInfo.class);
        UriBuilder uriBuilder = mock(UriBuilder.class);

        when(uriInfo.getBaseUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(org.mockito.ArgumentMatchers.anyString())).thenReturn(uriBuilder);
        when(uriBuilder.queryParam(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(uriBuilder);
        when(uriBuilder.build(org.mockito.ArgumentMatchers.anyInt())).thenReturn(target);

        return uriInfo;
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

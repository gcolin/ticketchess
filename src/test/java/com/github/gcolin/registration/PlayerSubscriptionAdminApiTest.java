package com.github.gcolin.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.gcolin.player.CustomPlayer;
import com.github.gcolin.event.Event;
import com.github.gcolin.registration.PlayerPendingSubscription;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.player.Player;
import com.github.gcolin.platform.Caches;
import com.github.gcolin.player.Find;
import com.github.gcolin.player.CustomPlayerDao;
import com.github.gcolin.registration.PlayerPendingSubscriptionDao;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.github.gcolin.platform.JteHtml;

class PlayerSubscriptionAdminApiTest {

    @Test
    void pageShouldReturnEmptyRowsWhenNoLinkedSubscriptions() throws Exception {
        PlayerSubscriptionAdminApi api = new PlayerSubscriptionAdminApi();
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        PlayerPendingSubscriptionDao pendingDao = mock(PlayerPendingSubscriptionDao.class);
        Find find = mock(Find.class);

        when(subDao.findLinkedToCustomPlayers()).thenReturn(List.of());
        when(pendingDao.findAllWithEvent()).thenReturn(List.of());

        CustomPlayerDao customDao = mock(CustomPlayerDao.class);
        when(customDao.findWithoutSubscription()).thenReturn(List.of());

        inject(api, "playerSubscriptionService", subDao);
        inject(api, "playerPendingSubscriptionService", pendingDao);
        inject(api, "find", find);
        inject(api, "customPlayerService", customDao);
        inject(api, "caches", new Caches());

        JteHtml html = api.page();

        assertEquals("registration/playersubscriptionAdmin.jte", html.getTemplate());
        Map<String, Object> model = html.getModel();
        assertTrue(((List<?>) model.get("rows")).isEmpty());
        assertTrue(((List<?>) model.get("pendingRows")).isEmpty());
        assertEquals(0, model.get("rowCount"));
        assertEquals(0, model.get("pendingRowCount"));
    }

    @Test
    void pageResultShouldIncludeReplacedAndFailedInModel() throws Exception {
        PlayerSubscriptionAdminApi api = new PlayerSubscriptionAdminApi();
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        PlayerPendingSubscriptionDao pendingDao = mock(PlayerPendingSubscriptionDao.class);

        when(subDao.findLinkedToCustomPlayers()).thenReturn(List.of());
        when(pendingDao.findAllWithEvent()).thenReturn(List.of());

        CustomPlayerDao customDao = mock(CustomPlayerDao.class);
        when(customDao.findWithoutSubscription()).thenReturn(List.of());

        inject(api, "playerSubscriptionService", subDao);
        inject(api, "playerPendingSubscriptionService", pendingDao);
        inject(api, "find", mock(Find.class));
        inject(api, "customPlayerService", customDao);
        inject(api, "caches", new Caches());

        JteHtml html = api.pageResult(5, 2, null, null);

        Map<String, Object> model = html.getModel();
        assertEquals(5, model.get("replaced"));
        assertEquals(2, model.get("failed"));
        assertEquals(0, model.get("orphanCount"));
    }

    @Test
    void deleteOrphanCustomPlayersShouldRemoveOrphanPlayers() throws Exception {
        PlayerSubscriptionAdminApi api = new PlayerSubscriptionAdminApi();
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        PlayerPendingSubscriptionDao pendingDao = mock(PlayerPendingSubscriptionDao.class);
        CustomPlayerDao customDao = mock(CustomPlayerDao.class);
        Find find = mock(Find.class);

        CustomPlayer orphan1 = new CustomPlayer();
        orphan1.setId(1);
        CustomPlayer orphan2 = new CustomPlayer();
        orphan2.setId(2);

        when(customDao.findWithoutSubscription()).thenReturn(List.of(orphan1, orphan2));
        when(subDao.findLinkedToCustomPlayers()).thenReturn(List.of());
        when(pendingDao.findAllWithEvent()).thenReturn(List.of());

        inject(api, "playerSubscriptionService", subDao);
        inject(api, "playerPendingSubscriptionService", pendingDao);
        inject(api, "customPlayerService", customDao);
        inject(api, "find", find);
        inject(api, "caches", new Caches());
        inject(
                api,
                "uriInfo",
                mockUriInfo(
                        URI.create(
                                "http://localhost:8080/playersubscription-admin/result?replaced=0&failed=0&deleted=2&deleteFailed=0")));

        Response response = api.replace(null, false, true, null);

        assertEquals(303, response.getStatus());
        verify(customDao).remove(orphan1);
        verify(customDao).remove(orphan2);
    }

    @Test
    void deletePendingSubscriptionShouldRemoveAndRedirect() throws Exception {
        PlayerSubscriptionAdminApi api = new PlayerSubscriptionAdminApi();
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        PlayerPendingSubscriptionDao pendingDao = mock(PlayerPendingSubscriptionDao.class);
        CustomPlayerDao customDao = mock(CustomPlayerDao.class);

        PlayerPendingSubscription pending = new PlayerPendingSubscription();
        pending.setId(7);

        when(pendingDao.find(7)).thenReturn(pending);
        when(subDao.findLinkedToCustomPlayers()).thenReturn(List.of());
        when(pendingDao.findAllWithEvent()).thenReturn(List.of());
        when(customDao.findWithoutSubscription()).thenReturn(List.of());

        inject(api, "playerSubscriptionService", subDao);
        inject(api, "playerPendingSubscriptionService", pendingDao);
        inject(api, "customPlayerService", customDao);
        inject(api, "find", mock(Find.class));
        inject(api, "caches", new Caches());
        inject(
                api,
                "uriInfo",
                mockUriInfo(
                        URI.create(
                                "http://localhost:8080/playersubscription-admin/result?replaced=0&failed=0&deleted=1&deleteFailed=0")));

        Response response = api.replace(null, false, false, 7);

        assertEquals(303, response.getStatus());
        verify(pendingDao).remove(pending);
    }

    @Test
    void replaceSingleSubscriptionShouldUpdateNrFfeAndRedirect() throws Exception {
        PlayerSubscriptionAdminApi api = new PlayerSubscriptionAdminApi();
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        PlayerPendingSubscriptionDao pendingDao = mock(PlayerPendingSubscriptionDao.class);
        CustomPlayerDao customDao = mock(CustomPlayerDao.class);
        Find find = mock(Find.class);

        PlayerSubscription sub = new PlayerSubscription();
        sub.setId(10);
        sub.setNrFfe("@5");
        Event event = new Event();
        event.setId(1);
        sub.setEvent(event);

        CustomPlayer customPlayer = new CustomPlayer();
        customPlayer.setId(5);
        customPlayer.setLicence("LIC123");

        Player lucenePlayer = new Player();
        lucenePlayer.setNrffe("FFE456");
        lucenePlayer.setName("Test");
        lucenePlayer.setFirstname("Player");

        when(subDao.find(10)).thenReturn(sub);
        when(customDao.find(5)).thenReturn(customPlayer);
        when(find.player("LIC123", null)).thenReturn(lucenePlayer);
        when(subDao.findLinkedToCustomPlayers()).thenReturn(List.of(sub));
        when(pendingDao.findAllWithEvent()).thenReturn(List.of());

        inject(api, "playerSubscriptionService", subDao);
        inject(api, "playerPendingSubscriptionService", pendingDao);
        inject(api, "customPlayerService", customDao);
        inject(api, "find", find);
        inject(api, "caches", new Caches());
        inject(
                api,
                "uriInfo",
                mockUriInfo(URI.create("http://localhost:8080/playersubscription-admin/result?replaced=1&failed=0")));

        Response response = api.replace(10, false, false, null);

        assertEquals(303, response.getStatus());
        verify(subDao).merge(sub);
    }

    @Test
    void replaceAllShouldProcessAllLinkedSubscriptions() throws Exception {
        PlayerSubscriptionAdminApi api = new PlayerSubscriptionAdminApi();
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        PlayerPendingSubscriptionDao pendingDao = mock(PlayerPendingSubscriptionDao.class);
        CustomPlayerDao customDao = mock(CustomPlayerDao.class);
        Find find = mock(Find.class);

        PlayerSubscription sub1 = new PlayerSubscription();
        sub1.setId(1);
        sub1.setNrFfe("@1");
        sub1.setEvent(new Event());

        PlayerSubscription sub2 = new PlayerSubscription();
        sub2.setId(2);
        sub2.setNrFfe("invalid");
        sub2.setEvent(new Event());

        CustomPlayer player = new CustomPlayer();
        player.setId(1);
        player.setLicence("LIC");

        Player lucenePlayer = new Player();
        lucenePlayer.setNrffe("FFE");

        when(subDao.findLinkedToCustomPlayers()).thenReturn(List.of(sub1, sub2));
        when(pendingDao.findAllWithEvent()).thenReturn(List.of());
        when(customDao.find(1)).thenReturn(player);
        when(find.player("LIC", null)).thenReturn(lucenePlayer);

        inject(api, "playerSubscriptionService", subDao);
        inject(api, "playerPendingSubscriptionService", pendingDao);
        inject(api, "customPlayerService", customDao);
        inject(api, "find", find);
        inject(api, "caches", new Caches());
        inject(
                api,
                "uriInfo",
                mockUriInfo(URI.create("http://localhost:8080/playersubscription-admin/result?replaced=1&failed=1")));

        Response response = api.replace(null, true, false, null);

        assertEquals(303, response.getStatus());
    }

    @Test
    void pageShouldExposePendingQueuePlayers() throws Exception {
        PlayerSubscriptionAdminApi api = new PlayerSubscriptionAdminApi();
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        PlayerPendingSubscriptionDao pendingDao = mock(PlayerPendingSubscriptionDao.class);
        CustomPlayerDao customDao = mock(CustomPlayerDao.class);
        Find find = mock(Find.class);

        Event event = new Event();
        event.setId(42);
        event.setName("Open de Paris");

        PlayerPendingSubscription pending = new PlayerPendingSubscription();
        pending.setId(99);
        pending.setNrFfe("LIC999");
        pending.setCreationUser("admin@test.com");
        pending.setEvent(event);

        Player player = new Player();
        player.setName("Dupont");
        player.setFirstname("Alice");

        when(subDao.findLinkedToCustomPlayers()).thenReturn(List.of());
        when(pendingDao.findAllWithEvent()).thenReturn(List.of(pending));
        when(customDao.findWithoutSubscription()).thenReturn(List.of());
        when(find.player("LIC999", null)).thenReturn(player);

        inject(api, "playerSubscriptionService", subDao);
        inject(api, "playerPendingSubscriptionService", pendingDao);
        inject(api, "customPlayerService", customDao);
        inject(api, "find", find);
        inject(api, "caches", new Caches());

        JteHtml html = api.page();

        Map<String, Object> model = html.getModel();
        assertEquals(1, model.get("pendingRowCount"));
        Map<?, ?> row = (Map<?, ?>) ((List<?>) model.get("pendingRows")).get(0);
        assertEquals(99, row.get("id"));
        assertEquals("LIC999", row.get("licence"));
        assertEquals("Dupont", row.get("lastname"));
        assertEquals("Alice", row.get("firstname"));
        assertEquals("admin@test.com", row.get("creationUser"));
        assertEquals(42, row.get("eventId"));
        assertEquals("Open de Paris", row.get("eventName"));
    }

    private static UriInfo mockUriInfo(URI target) {
        UriInfo uriInfo = mock(UriInfo.class);
        UriBuilder uriBuilder = mock(UriBuilder.class);

        when(uriInfo.getBaseUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(org.mockito.ArgumentMatchers.anyString())).thenReturn(uriBuilder);
        when(uriBuilder.queryParam(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(uriBuilder);
        when(uriBuilder.build(org.mockito.ArgumentMatchers.any())).thenReturn(target);

        return uriInfo;
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

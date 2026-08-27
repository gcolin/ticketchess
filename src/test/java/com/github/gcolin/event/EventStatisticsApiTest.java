package com.github.gcolin.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.gcolin.event.Event;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.player.IPlayer;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import com.github.gcolin.player.Find;
import com.github.gcolin.event.StatisticsReportService;
import com.github.gcolin.event.EventDao;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.github.gcolin.platform.JteHtml;
import com.github.gcolin.platform.TestContext;

class EventStatisticsApiTest {

    @Test
    void showShouldCountOnlyPaidSubscriptionsWithNrFfe() throws Exception {
        EventStatisticsApi api = new EventStatisticsApi();

        EventDao eventDao = mock(EventDao.class);
        PlayerSubscriptionDao playerSubscriptionDao = mock(PlayerSubscriptionDao.class);
        Find find = mock(Find.class);

        Event event = new Event();
        event.setId(1);

        IPlayer player = mock(IPlayer.class);
        when(player.getClub()).thenReturn("ClubA");
        when(player.getClubRef()).thenReturn("A");
        when(player.getCategory()).thenReturn("S");
        when(player.getFederation()).thenReturn("FFE");

        PlayerSubscription paid = new PlayerSubscription();
        paid.setStatus(PlayerSubscriptionStatus.PAID);
        paid.setNrFfe("A12345");
        paid.setAmountCents(3000L);

        PlayerSubscription notPaid = new PlayerSubscription();
        notPaid.setStatus(PlayerSubscriptionStatus.NOT_PAID);
        notPaid.setNrFfe("B99999");

        PlayerSubscription cancelled = new PlayerSubscription();
        cancelled.setStatus(PlayerSubscriptionStatus.CANCELLED);
        cancelled.setNrFfe("C11111");

        when(eventDao.find(1)).thenReturn(event);
        when(playerSubscriptionDao.findByEvent(event)).thenReturn(List.of(paid, notPaid, cancelled));
        when(find.player("A12345", null)).thenReturn(player);
        when(find.player("B99999", null)).thenReturn(player);

        inject(api, "eventDao", eventDao);
        inject(api, "playerSubscriptionDao", playerSubscriptionDao);
        inject(api, "statisticsReportService", new StatisticsReportService());

        try (TestContext ignored = TestContext.open(find)) {
        JteHtml html = api.show(1);
        Map<String, Object> model = html.getModel();

        assertEquals("event/eventStatistics.jte", html.getTemplate());
        assertEquals(2, model.get("total"));
        assertEquals(30.0, (Double) model.get("totalAmount"), 0.001);
        assertSame(event, model.get("event"));
        }
    }

    @Test
    void showShouldSkipPaidSubscriptionsWithBlankNrFfe() throws Exception {
        EventStatisticsApi api = new EventStatisticsApi();

        EventDao eventDao = mock(EventDao.class);
        PlayerSubscriptionDao playerSubscriptionDao = mock(PlayerSubscriptionDao.class);
        Find find = mock(Find.class);

        Event event = new Event();
        event.setId(2);

        PlayerSubscription noNrFfe = new PlayerSubscription();
        noNrFfe.setStatus(PlayerSubscriptionStatus.PAID);
        noNrFfe.setNrFfe("");

        PlayerSubscription nullNrFfe = new PlayerSubscription();
        nullNrFfe.setStatus(PlayerSubscriptionStatus.PAID);
        nullNrFfe.setNrFfe(null);

        when(eventDao.find(2)).thenReturn(event);
        when(playerSubscriptionDao.findByEvent(event)).thenReturn(List.of(noNrFfe, nullNrFfe));

        inject(api, "eventDao", eventDao);
        inject(api, "playerSubscriptionDao", playerSubscriptionDao);
        inject(api, "statisticsReportService", new StatisticsReportService());

        try (TestContext ignored = TestContext.open(find)) {
        JteHtml html = api.show(2);
        Map<String, Object> model = html.getModel();

        assertEquals(0, model.get("total"));
        assertEquals(0.0, (Double) model.get("totalAmount"), 0.001);
        }
    }

    @Test
    void showShouldSkipPlayerLookupWhenFindReturnsNull() throws Exception {
        EventStatisticsApi api = new EventStatisticsApi();

        EventDao eventDao = mock(EventDao.class);
        PlayerSubscriptionDao playerSubscriptionDao = mock(PlayerSubscriptionDao.class);
        Find find = mock(Find.class);

        Event event = new Event();
        event.setId(3);

        PlayerSubscription sub = new PlayerSubscription();
        sub.setStatus(PlayerSubscriptionStatus.PAID);
        sub.setNrFfe("X00001");
        sub.setAmountCents(1000L);

        when(eventDao.find(3)).thenReturn(event);
        when(playerSubscriptionDao.findByEvent(event)).thenReturn(List.of(sub));
        when(find.player("X00001", null)).thenReturn(null);

        inject(api, "eventDao", eventDao);
        inject(api, "playerSubscriptionDao", playerSubscriptionDao);
        inject(api, "statisticsReportService", new StatisticsReportService());

        try (TestContext ignored = TestContext.open(find)) {
        JteHtml html = api.show(3);
        Map<String, Object> model = html.getModel();

        // total counts the subscription even if player not found
        assertEquals(1, model.get("total"));
        // but byClub/byCategory/byFederation should be empty
        @SuppressWarnings("unchecked")
        Map<String, Integer> byClub = (Map<String, Integer>) model.get("byClub");
        assertEquals(0, byClub.size());
        }
    }

    @Test
    void showShouldFallbackToUnknownWhenPlayerFieldsAreEmpty() throws Exception {
        EventStatisticsApi api = new EventStatisticsApi();

        EventDao eventDao = mock(EventDao.class);
        PlayerSubscriptionDao playerSubscriptionDao = mock(PlayerSubscriptionDao.class);
        Find find = mock(Find.class);

        Event event = new Event();
        event.setId(4);

        IPlayer player = mock(IPlayer.class);
        when(player.getClub()).thenReturn(null);
        when(player.getClubRef()).thenReturn(null);
        when(player.getCategory()).thenReturn("");
        when(player.getFederation()).thenReturn(null);

        PlayerSubscription sub = new PlayerSubscription();
        sub.setStatus(PlayerSubscriptionStatus.PAID);
        sub.setNrFfe("Y00001");
        sub.setAmountCents(null);

        when(eventDao.find(4)).thenReturn(event);
        when(playerSubscriptionDao.findByEvent(event)).thenReturn(List.of(sub));
        when(find.player("Y00001", null)).thenReturn(player);

        inject(api, "eventDao", eventDao);
        inject(api, "playerSubscriptionDao", playerSubscriptionDao);
        inject(api, "statisticsReportService", new StatisticsReportService());

        try (TestContext ignored = TestContext.open(find)) {
        JteHtml html = api.show(4);
        Map<String, Object> model = html.getModel();

        @SuppressWarnings("unchecked")
        Map<String, Integer> byClub = (Map<String, Integer>) model.get("byClub");
        @SuppressWarnings("unchecked")
        Map<String, Integer> byCategory = (Map<String, Integer>) model.get("byCategory");
        @SuppressWarnings("unchecked")
        Map<String, Integer> byFederation = (Map<String, Integer>) model.get("byFederation");

        assertEquals(1, byClub.get("—"));
        assertEquals(1, byCategory.get("—"));
        assertEquals(1, byFederation.get("—"));
        // null amountCents should not add to total amount
        assertEquals(0.0, (Double) model.get("totalAmount"), 0.001);
        }
    }

    @Test
    void showShouldSortByClubByCountDescending() throws Exception {
        EventStatisticsApi api = new EventStatisticsApi();

        EventDao eventDao = mock(EventDao.class);
        PlayerSubscriptionDao playerSubscriptionDao = mock(PlayerSubscriptionDao.class);
        Find find = mock(Find.class);

        Event event = new Event();
        event.setId(5);

        IPlayer playerA = mock(IPlayer.class);
        when(playerA.getClub()).thenReturn("AlphaClub");
        when(playerA.getClubRef()).thenReturn("A");
        when(playerA.getCategory()).thenReturn("S");
        when(playerA.getFederation()).thenReturn("FFE");

        IPlayer playerB = mock(IPlayer.class);
        when(playerB.getClub()).thenReturn("BetaClub");
        when(playerB.getClubRef()).thenReturn("B");
        when(playerB.getCategory()).thenReturn("S");
        when(playerB.getFederation()).thenReturn("FFE");

        PlayerSubscription sub1 = makePaidSub("A1", 1000L);
        PlayerSubscription sub2 = makePaidSub("B1", 1000L);
        PlayerSubscription sub3 = makePaidSub("B2", 1000L);

        when(eventDao.find(5)).thenReturn(event);
        when(playerSubscriptionDao.findByEvent(event)).thenReturn(List.of(sub1, sub2, sub3));
        when(find.player("A1", null)).thenReturn(playerA);
        when(find.player("B1", null)).thenReturn(playerB);
        when(find.player("B2", null)).thenReturn(playerB);

        inject(api, "eventDao", eventDao);
        inject(api, "playerSubscriptionDao", playerSubscriptionDao);
        inject(api, "statisticsReportService", new StatisticsReportService());

        try (TestContext ignored = TestContext.open(find)) {
        JteHtml html = api.show(5);
        @SuppressWarnings("unchecked")
        Map<String, Integer> byClub = (Map<String, Integer>) html.getModel().get("byClub");

        // BetaClub has 2, AlphaClub has 1 → BetaClub should be first
        String firstKey = byClub.keySet().iterator().next();
        assertEquals("BetaClub", firstKey);
        assertEquals(2, byClub.get("BetaClub"));
        assertEquals(1, byClub.get("AlphaClub"));
        }
    }

    private static PlayerSubscription makePaidSub(String nrFfe, Long amountCents) {
        PlayerSubscription sub = new PlayerSubscription();
        sub.setStatus(PlayerSubscriptionStatus.PAID);
        sub.setNrFfe(nrFfe);
        sub.setAmountCents(amountCents);
        return sub;
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

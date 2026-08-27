package com.github.gcolin.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.gcolin.event.Event;
import com.github.gcolin.player.Find;
import com.github.gcolin.event.EventDao;
import com.github.gcolin.payment.PaymentDao;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.github.gcolin.platform.JteHtml;

class StatisticsApiTest {

    @Test
    void pageShouldBuildModelFromDaos() throws Exception {
        StatisticsApi api = new StatisticsApi();

        EventDao eventDao = mock(EventDao.class);
        PaymentDao paymentDao = mock(PaymentDao.class);
        PlayerSubscriptionDao playerSubscriptionDao = mock(PlayerSubscriptionDao.class);
        Find find = mock(Find.class);

        List<Event> closestEvents = List.of(new Event(), new Event());
        List<Object[]> topEvents = new ArrayList<>();
        topEvents.add(new Object[] {new Event(), 5L});

        when(eventDao.findClosestEvents(10)).thenReturn(closestEvents);
        when(eventDao.findTopEventsByParticipants(10)).thenReturn(topEvents);
        when(paymentDao.sumAllPayments()).thenReturn(123.45);
        when(playerSubscriptionDao.findNotPaidWithEvent()).thenReturn(List.of());

        inject(api, "eventDao", eventDao);
        inject(api, "paymentDao", paymentDao);
        inject(api, "playerSubscriptionDao", playerSubscriptionDao);
        inject(api, "find", find);

        JteHtml html = api.page();
        Map<String, Object> model = html.getModel();

        assertEquals("event/statistics.jte", html.getTemplate());
        assertSame(closestEvents, model.get("closestEvents"));
        assertSame(topEvents, model.get("topEvents"));
        assertEquals(123.45, (Double) model.get("totalPayments"));
        assertEquals(0.0, (Double) model.get("unpaidPlayerSubscriptions"));
    }

    @Test
    void pageShouldDefaultTotalPaymentsToZeroWhenNull() throws Exception {
        StatisticsApi api = new StatisticsApi();

        EventDao eventDao = mock(EventDao.class);
        PaymentDao paymentDao = mock(PaymentDao.class);
        PlayerSubscriptionDao playerSubscriptionDao = mock(PlayerSubscriptionDao.class);
        Find find = mock(Find.class);

        when(eventDao.findClosestEvents(10)).thenReturn(List.of());
        when(eventDao.findTopEventsByParticipants(10)).thenReturn(List.of());
        when(paymentDao.sumAllPayments()).thenReturn(null);
        when(playerSubscriptionDao.findNotPaidWithEvent()).thenReturn(List.of());

        inject(api, "eventDao", eventDao);
        inject(api, "paymentDao", paymentDao);
        inject(api, "playerSubscriptionDao", playerSubscriptionDao);
        inject(api, "find", find);

        JteHtml html = api.page();
        Map<String, Object> model = html.getModel();

        assertEquals(0.0, (Double) model.get("totalPayments"));
        assertEquals(0.0, (Double) model.get("unpaidPlayerSubscriptions"));
        assertTrue(((List<?>) model.get("closestEvents")).isEmpty());
        assertTrue(((List<?>) model.get("topEvents")).isEmpty());
    }

    private static void inject(StatisticsApi api, String fieldName, Object value) throws Exception {
        Field field = StatisticsApi.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(api, value);
    }
}

package com.github.gcolin.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import com.github.gcolin.player.Find;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DashboardApiTest {

    @Test
    void pageShouldBuildDashboardWithSubscriptions() throws Exception {
        DashboardApi api = new DashboardApi();
        PlayerSubscriptionDao dao = mock(PlayerSubscriptionDao.class);
        Find find = mock(Find.class);

        PlayerSubscription sub = new PlayerSubscription();
        sub.setNrFfe("FFE123");

        when(dao.findWithoutPaymentWithEvent(PlayerSubscriptionStatus.PAID)).thenReturn(List.of(sub));
        when(dao.findCancelledWithEvent()).thenReturn(List.of());
        when(dao.findNotPaidWithEvent()).thenReturn(List.of());

        inject(api, "playerSubscriptionDao", dao);
        inject(api, "find", find);

        JteHtml html = api.page();

        assertEquals("platform/dashboard.jte", html.getTemplate());
        Map<String, Object> model = html.getModel();
        assertEquals(1, ((List<?>) model.get("subsWithoutPayment")).size());
        assertEquals(0, ((List<?>) model.get("cancelledSubs")).size());
        assertEquals(0, ((List<?>) model.get("notPaidSubs")).size());
    }

    @Test
    void pageWithMultipleSubscriptionsShouldReturnAllLists() throws Exception {
        DashboardApi api = new DashboardApi();
        PlayerSubscriptionDao dao = mock(PlayerSubscriptionDao.class);
        Find find = mock(Find.class);

        PlayerSubscription sub1 = new PlayerSubscription();
        sub1.setNrFfe("FFE1");

        PlayerSubscription sub2 = new PlayerSubscription();
        sub2.setNrFfe("FFE2");

        PlayerSubscription sub3 = new PlayerSubscription();
        sub3.setNrFfe("FFE3");

        when(dao.findWithoutPaymentWithEvent(PlayerSubscriptionStatus.PAID)).thenReturn(List.of(sub1));
        when(dao.findCancelledWithEvent()).thenReturn(List.of(sub2));
        when(dao.findNotPaidWithEvent()).thenReturn(List.of(sub3));

        inject(api, "playerSubscriptionDao", dao);
        inject(api, "find", find);

        JteHtml html = api.page();

        Map<String, Object> model = html.getModel();
        assertEquals(1, ((List<?>) model.get("subsWithoutPayment")).size());
        assertEquals(1, ((List<?>) model.get("cancelledSubs")).size());
        assertEquals(1, ((List<?>) model.get("notPaidSubs")).size());
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

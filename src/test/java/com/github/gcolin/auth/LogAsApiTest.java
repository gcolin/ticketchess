package com.github.gcolin.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.gcolin.platform.Config;
import com.github.gcolin.payment.PaymentDao;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import com.github.gcolin.platform.JteHtml;

class LogAsApiTest {

    @Test
    void pageShouldMergeEmailsFromBothDaos() throws Exception {
        LogAsApi api = new LogAsApi();

        PlayerSubscriptionDao psd = mock(PlayerSubscriptionDao.class);
        PaymentDao paymentDao = mock(PaymentDao.class);
        Config config = mock(Config.class);

        when(psd.findDistinctCreationUsers()).thenReturn(List.of("alice@example.com", "bob@example.com"));
        when(paymentDao.findDistinctUserEmails()).thenReturn(List.of("charlie@example.com", "bob@example.com"));
        when(config.getAdmins()).thenReturn(Set.of("admin@example.com"));

        inject(api, "playerSubscriptionDao", psd);
        inject(api, "paymentDao", paymentDao);
        inject(api, "config", config);

        JteHtml html = api.page();
        Map<String, Object> model = html.getModel();

        assertEquals("auth/logas.jte", html.getTemplate());

        @SuppressWarnings("unchecked")
        List<String> users = (List<String>) model.get("users");
        // duplicates should be removed → 3 unique emails
        assertEquals(3, users.size());
        // should be sorted (TreeSet order)
        assertEquals("alice@example.com", users.get(0));
        assertEquals("bob@example.com", users.get(1));
        assertEquals("charlie@example.com", users.get(2));
    }

    @Test
    void pageShouldDeduplicateOverlappingEmails() throws Exception {
        LogAsApi api = new LogAsApi();

        PlayerSubscriptionDao psd = mock(PlayerSubscriptionDao.class);
        PaymentDao paymentDao = mock(PaymentDao.class);
        Config config = mock(Config.class);

        when(psd.findDistinctCreationUsers()).thenReturn(List.of("shared@example.com"));
        when(paymentDao.findDistinctUserEmails()).thenReturn(List.of("shared@example.com"));
        when(config.getAdmins()).thenReturn(Set.of());

        inject(api, "playerSubscriptionDao", psd);
        inject(api, "paymentDao", paymentDao);
        inject(api, "config", config);

        JteHtml html = api.page();
        @SuppressWarnings("unchecked")
        List<String> users = (List<String>) html.getModel().get("users");

        assertEquals(1, users.size());
        assertEquals("shared@example.com", users.get(0));
    }

    @Test
    void pageShouldExposeAdminsInModel() throws Exception {
        LogAsApi api = new LogAsApi();

        PlayerSubscriptionDao psd = mock(PlayerSubscriptionDao.class);
        PaymentDao paymentDao = mock(PaymentDao.class);
        Config config = mock(Config.class);

        Set<String> admins = Set.of("admin1@example.com", "admin2@example.com");
        when(psd.findDistinctCreationUsers()).thenReturn(List.of());
        when(paymentDao.findDistinctUserEmails()).thenReturn(List.of());
        when(config.getAdmins()).thenReturn(admins);

        inject(api, "playerSubscriptionDao", psd);
        inject(api, "paymentDao", paymentDao);
        inject(api, "config", config);

        JteHtml html = api.page();
        assertSame(admins, html.getModel().get("admins"));
    }

    @Test
    void pageShouldReturnEmptyUsersWhenBothDaosEmpty() throws Exception {
        LogAsApi api = new LogAsApi();

        PlayerSubscriptionDao psd = mock(PlayerSubscriptionDao.class);
        PaymentDao paymentDao = mock(PaymentDao.class);
        Config config = mock(Config.class);

        when(psd.findDistinctCreationUsers()).thenReturn(List.of());
        when(paymentDao.findDistinctUserEmails()).thenReturn(List.of());
        when(config.getAdmins()).thenReturn(Set.of());

        inject(api, "playerSubscriptionDao", psd);
        inject(api, "paymentDao", paymentDao);
        inject(api, "config", config);

        JteHtml html = api.page();
        @SuppressWarnings("unchecked")
        List<String> users = (List<String>) html.getModel().get("users");
        assertEquals(0, users.size());
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

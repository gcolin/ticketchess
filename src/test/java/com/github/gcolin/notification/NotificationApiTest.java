package com.github.gcolin.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.gcolin.notification.Notification;
import com.github.gcolin.platform.Caches;
import com.github.gcolin.notification.NotificationDao;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.github.gcolin.platform.JteHtml;

class NotificationApiTest {

    @Test
    void listShouldReturnAllNotifications() throws Exception {
        NotificationApi api = new NotificationApi();
        NotificationDao dao = mock(NotificationDao.class);

        Notification notif = new Notification();
        notif.setId(1);
        notif.setContent("Test notification");

        when(dao.all()).thenReturn(List.of(notif));

        inject(api, "notificationService", dao);

        JteHtml html = api.list();

        assertEquals("notification/notification.jte", html.getTemplate());
        @SuppressWarnings("unchecked")
        List<Notification> notifs = (List<Notification>) html.getModel().get("notifs");
        assertEquals(1, notifs.size());
    }

    @Test
    void newEShouldReturnEmptyNotificationTemplate() throws Exception {
        NotificationApi api = new NotificationApi();
        NotificationDao dao = mock(NotificationDao.class);

        inject(api, "notificationService", dao);

        JteHtml html = api.newE();

        assertEquals("notification/notificationEdit.jte", html.getTemplate());
        Notification notif = (Notification) html.getModel().get("notif");
        assertEquals("", notif.getContent());
    }

    @Test
    void editWithIdShouldReturnNotificationFromDao() throws Exception {
        NotificationApi api = new NotificationApi();
        NotificationDao dao = mock(NotificationDao.class);

        Notification notif = new Notification();
        notif.setId(5);
        notif.setContent("Existing notification");

        when(dao.find(5)).thenReturn(notif);

        inject(api, "notificationService", dao);

        JteHtml html = api.edit(5);

        assertEquals("notification/notificationEdit.jte", html.getTemplate());
        Notification result = (Notification) html.getModel().get("notif");
        assertEquals(5, result.getId());
    }

    @Test
    void saveShouldRemoveNotificationWhenToRemoveIsTrue() throws Exception {
        NotificationApi api = new NotificationApi();
        NotificationDao dao = mock(NotificationDao.class);

        inject(api, "notificationService", dao);
        inject(api, "caches", new Caches());
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/notification")));

        Response response = api.save(3, "true", null, null, "content");

        assertEquals(303, response.getStatus());
        verify(dao).remove(3);
    }

    @Test
    void saveShouldSetNotificationWhenToRemoveIsFalse() throws Exception {
        NotificationApi api = new NotificationApi();
        NotificationDao dao = mock(NotificationDao.class);

        inject(api, "notificationService", dao);
        inject(api, "caches", new Caches());
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/notification")));

        Response response = api.save(null, "false", 10, 5, "New notification");

        assertEquals(303, response.getStatus());
        verify(dao).setNotification(null, 10, 5, "New notification");
    }

    private static UriInfo mockUriInfo(URI target) {
        UriInfo uriInfo = mock(UriInfo.class);
        UriBuilder uriBuilder = mock(UriBuilder.class);

        when(uriInfo.getBaseUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(org.mockito.ArgumentMatchers.anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(target);

        return uriInfo;
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

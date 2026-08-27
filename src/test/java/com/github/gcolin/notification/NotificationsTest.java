package com.github.gcolin.notification;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.gcolin.platform.Caches;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationsTest {

    private Notifications notifications;
    private Caches caches;
    private NotificationDao notificationService;

    @BeforeEach
    void setUp() {
        caches = mock(Caches.class);
        notificationService = mock(NotificationDao.class);
        notifications = new Notifications();
        notifications.setCaches(caches);
        notifications.setNotificationDao(notificationService);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testGetGlobal() {
        Cache<String, List<Notification>> cache = mock(Cache.class);

        when(caches.getNotifications()).thenReturn(cache);
        when(cache.getIfPresent("all")).thenReturn(null);

        Notification notification = new Notification();
        notification.setContent("Hello **world** <script>");

        when(notificationService.findGlobal()).thenReturn(List.of(notification));

        List<Notification> result = notifications.getGlobal();

        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.get(0).getContent().contains("<strong>world</strong>"));
        Assertions.assertTrue(result.get(0).getContent().contains("&lt;script&gt;"));

        verify(cache).put("all", result);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testNoNotifs() {
        Cache<String, List<Notification>> cache = mock(Cache.class);

        when(caches.getNotifications()).thenReturn(cache);
        when(cache.getIfPresent("all")).thenReturn(null);

        when(notificationService.findGlobal()).thenReturn(Collections.emptyList());

        List<Notification> result = notifications.getGlobal();

        Assertions.assertEquals(0, result.size());

        verify(cache).put("all", result);
    }
}

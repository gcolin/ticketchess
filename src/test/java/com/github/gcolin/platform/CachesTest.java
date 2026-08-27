package com.github.gcolin.platform;

import com.github.gcolin.event.EventCache;
import com.github.gcolin.event.EventsCache;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CachesTest {

    @Test
    void testAllCachesAreInitialized() {
        Caches caches = new Caches();

        Assertions.assertNotNull(caches.getDebtCache());
        Assertions.assertNotNull(caches.getAllEvents());
        Assertions.assertNotNull(caches.getEvent());
        Assertions.assertNotNull(caches.getNotifications());
        Assertions.assertNotNull(caches.getEventGroups());
    }

    @Test
    void testDebtCachePutAndGet() {
        Caches caches = new Caches();

        caches.getDebtCache().put("alice@example.com", 18.75);

        Assertions.assertEquals(18.75, caches.getDebtCache().getIfPresent("alice@example.com"));
    }

    @Test
    void testEventCachesPutAndGet() {
        Caches caches = new Caches();
        EventsCache eventsCache = new EventsCache();
        EventCache eventCache = new EventCache();

        caches.getAllEvents().put("all", eventsCache);
        caches.getEvent().put("event-1", eventCache);

        Assertions.assertSame(eventsCache, caches.getAllEvents().getIfPresent("all"));
        Assertions.assertSame(eventCache, caches.getEvent().getIfPresent("event-1"));
    }
}

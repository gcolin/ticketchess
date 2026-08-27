package com.github.gcolin.platform;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.gcolin.notification.Notification;
import com.github.gcolin.platform.SelectItem;
import com.github.gcolin.event.EventCache;
import com.github.gcolin.event.EventsCache;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Caches {
    private final Cache<String, Double> debtCache;
    private final Cache<String, Boolean> permissionCache;
    private final Cache<String, EventsCache> allEvents;
    private final Cache<String, EventCache> event;
    private final Cache<String, List<Notification>> notifications;
    private final Cache<String, List<SelectItem>> eventGroups;

    public Caches() {
        debtCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(20, TimeUnit.MINUTES)
                .build();

        permissionCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(20, TimeUnit.MINUTES)
                .build();

        allEvents = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(20, TimeUnit.MINUTES)
                .build();

        event = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(20, TimeUnit.MINUTES)
                .build();

        notifications = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(20, TimeUnit.MINUTES)
                .build();

        eventGroups = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(20, TimeUnit.MINUTES)
                .build();
    }

    public Cache<String, EventsCache> getAllEvents() {
        return allEvents;
    }

    public Cache<String, Double> getDebtCache() {
        return debtCache;
    }

    public Cache<String, Boolean> getPermissionCache() {
        return permissionCache;
    }

    public Cache<String, EventCache> getEvent() {
        return event;
    }

    public Cache<String, List<Notification>> getNotifications() {
        return notifications;
    }

    public Cache<String, List<SelectItem>> getEventGroups() {
        return eventGroups;
    }
}

package com.github.gcolin.notification;

import com.github.gcolin.platform.Caches;
import java.util.List;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class Notifications {

    private Caches caches;
    private NotificationDao notificationService;

    public void setCaches(Caches caches) {
        this.caches = caches;
    }

    public void setNotificationDao(NotificationDao notificationService) {
        this.notificationService = notificationService;
    }

    public List<Notification> getGlobal() {
        List<Notification> notifications = caches.getNotifications().getIfPresent("all");

        if (notifications == null) {
            notifications = notificationService.findGlobal();
            for (Notification notification : notifications) {
                Parser parser = Parser.builder().build();
                Node document = parser.parse(notification.getContent());
                HtmlRenderer renderer = HtmlRenderer.builder().escapeHtml(true).build();
                notification.setContent(renderer.render(document));
            }
            caches.getNotifications().put("all", notifications);
        }

        return notifications;
    }
}

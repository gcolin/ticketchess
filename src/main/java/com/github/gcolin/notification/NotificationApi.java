package com.github.gcolin.notification;

import com.github.gcolin.auth.RequirePermission;
import com.github.gcolin.notification.Notification;
import com.github.gcolin.auth.PermissionCode;
import com.github.gcolin.platform.Caches;
import com.github.gcolin.notification.NotificationDao;
import jakarta.inject.Inject;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.Map;
import com.github.gcolin.platform.JteHtml;

@Path("notification")
@RequirePermission(PermissionCode.EVENT_EDIT)
public class NotificationApi {

    @Inject
    private NotificationDao notificationService;

    @Inject
    private Caches caches;

    @Context
    UriInfo uriInfo;

    @GET
    public JteHtml list() {
        return new JteHtml(Collections.singletonMap("notifs", notificationService.all()), "notification/notification.jte");
    }

    @GET
    @Path("new")
    public JteHtml newE() {
        return edit(null);
    }

    @GET
    @Path("{id:\\d+}")
    public JteHtml edit(@PathParam("id") Integer id) {
        Map<String, Object> model;
        if (id != null) {
            model = Collections.singletonMap("notif", notificationService.find(id));
        } else {
            Notification notif = new Notification();
            notif.setContent("");
            model = Collections.singletonMap("notif", notif);
        }
        return new JteHtml(model, "notification/notificationEdit.jte");
    }

    @POST
    public Response save(
            @FormParam("id") Integer id,
            @FormParam("toRemove") String toRemove,
            @FormParam("eventId") Integer eventId,
            @FormParam("eventGroupId") Integer eventGroupId,
            @FormParam("content") String content) {
        if (toRemove.equals("true")) {
            notificationService.remove(id);
            caches.getNotifications().invalidateAll();
            return Response.seeOther(
                            uriInfo.getBaseUriBuilder().path("notification").build())
                    .build();
        }

        notificationService.setNotification(id, eventId, eventGroupId, content);

        caches.getNotifications().invalidateAll();
        return Response.seeOther(
                        uriInfo.getBaseUriBuilder().path("notification").build())
                .build();
    }
}

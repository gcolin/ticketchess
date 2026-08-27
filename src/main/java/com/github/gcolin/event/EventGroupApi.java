package com.github.gcolin.event;

import com.github.gcolin.auth.RequirePermission;
import com.github.gcolin.event.EventGroup;
import com.github.gcolin.auth.PermissionCode;
import com.github.gcolin.platform.Caches;
import com.github.gcolin.event.EventGroupDao;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.github.gcolin.platform.JteHtml;

@RequirePermission(PermissionCode.EVENT_EDIT)
@Path("eventgroup")
public class EventGroupApi {

    @Inject
    private EventGroupDao eventGroupService;

    @Context
    UriInfo uriInfo;

    @Inject
    private Caches caches;

    @GET
    public JteHtml list() {
        List<EventGroup> eventGroups = eventGroupService.all();
        eventGroups.sort((p1, p2) -> {
            return p1.getName().compareTo(p2.getName());
        });
        return new JteHtml(Collections.singletonMap("eventGroups", eventGroups), "event/eventgroup.jte");
    }

    @GET
    @Path("new")
    public JteHtml newEventGroup() {
        List<EventGroup> eventGroups = eventGroupService.all();
        eventGroups.sort((p1, p2) -> {
            return p1.getName().compareTo(p2.getName());
        });
        return new JteHtml(Collections.singletonMap("eventGroup", new EventGroup()), "event/eventgroupEdit.jte");
    }

    @GET
    @Path("{id:\\d+}")
    public JteHtml edit(@PathParam("id") Integer id) {
        Map<String, Object> model = new HashMap<String, Object>();

        EventGroup eventGroup = eventGroupService.findDetachedForEdit(id);
        model.put("events", eventGroup.getEvents());
        model.put("egnotifications", eventGroup.getNotifications());
        model.put("eventGroup", eventGroup);

        return new JteHtml(model, "event/eventgroupEdit.jte");
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response save(
            @FormParam("id") Integer id, @FormParam("shortname") String shortname, @FormParam("name") String name) {
        EventGroup eventGroup = new EventGroup();
        if (id != null) {
            eventGroup.setId(id);
        }
        eventGroup.setName(name);
        eventGroup.setShortname(shortname);
        if (eventGroup.getId() == null) {
            eventGroupService.persist(eventGroup);
        } else {
            eventGroupService.merge(eventGroup);
        }

        caches.getEvent().invalidateAll();
        caches.getAllEvents().invalidateAll();
        caches.getEventGroups().invalidateAll();

        URI redirect = uriInfo.getBaseUriBuilder()
                .path("eventgroup")
                .path(eventGroup.getId().toString())
                .build();

        return Response.seeOther(redirect).build();
    }

    @DELETE
    @Path("{id:\\d+}")
    public Response removeById(@PathParam("id") Integer id) {
        EventGroup eventGroup = eventGroupService.find(id);
        if (eventGroup == null) {
            throw new NotFoundException();
        }
        // Do NOT cascade remove - manually delete without affecting related entities
        eventGroupService.remove(eventGroup);
        caches.getEvent().invalidateAll();
        caches.getAllEvents().invalidateAll();
        caches.getEventGroups().invalidateAll();
        return Response.ok().build();
    }

    @POST
    public Response remove(@FormParam("id") Integer id) {
        EventGroup eventGroup = eventGroupService.find(id);
        if (eventGroup == null) {
            throw new NotFoundException();
        }
        // Do NOT cascade remove - manually delete without affecting related entities
        eventGroupService.remove(eventGroup);
        caches.getEvent().invalidateAll();
        caches.getAllEvents().invalidateAll();
        caches.getEventGroups().invalidateAll();
        return Response.seeOther(uriInfo.getBaseUriBuilder().path("eventgroup").build())
                .build();
    }
}

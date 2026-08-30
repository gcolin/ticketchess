package com.github.gcolin.event;

import com.github.gcolin.auth.RequirePermission;
import com.github.gcolin.event.EventCollection;
import com.github.gcolin.event.EventCollectionOptionType;
import com.github.gcolin.auth.PermissionCode;
import com.github.gcolin.platform.Caches;
import com.github.gcolin.registration.RegisterService;
import com.github.gcolin.event.EventCollectionDao;
import com.github.gcolin.event.EventCollectionOptionDao;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.github.gcolin.platform.JteHtml;

@Path("eventcollection")
@RequirePermission(PermissionCode.EVENT_EDIT)
public class EventCollectionApi {

    @Inject
    private EventCollectionDao eventCollectionService;

    @Inject
    private Caches caches;

    @Inject
    private EventCollectionOptionDao eventCollectionOptionService;

    @Inject
    private RegisterService registerService;

    @Context
    private UriInfo uriInfo;

    @GET
    public JteHtml page(
            @QueryParam("created") String created,
            @QueryParam("updated") String updated,
            @QueryParam("removed") String removed,
            @QueryParam("error") String error) {
        Map<String, Object> model = new HashMap<>();
        List<EventCollection> eventCollections = eventCollectionService.allOrdered();
        model.put("eventCollections", eventCollections);
        model.put("created", "1".equals(created));
        model.put("updated", "1".equals(updated));
        model.put("removed", "1".equals(removed));
        model.put("error", error);
        return new JteHtml(model, "event/eventcollection.jte");
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response post(
            @FormParam("action") String action,
            @FormParam("id") Integer id,
            @FormParam("name") String name,
            @FormParam("maxSubscribe") String maxSubscribe,
            @FormParam("chessEventId") String chessEventId) {

        String normalizedMaxSubscribe = normalizeOptionalInteger(maxSubscribe);
        if (maxSubscribe != null && normalizedMaxSubscribe == null) {
            if ("update".equals(action) && id != null) {
                return redirectToEditError(id, "invalidMaxSubscribe");
            }
            return redirect("error", "invalidMaxSubscribe");
        }

        if ("update".equals(action)) {
            if (id == null) {
                return redirect("error", "missingId");
            }
            EventCollection eventCollection = eventCollectionService.find(id);
            if (eventCollection == null) {
                return redirect("error", "notFound");
            }
            String trimName = trim(name);
            if (trimName == null) {
                return redirect("error", "emptyName");
            }
            eventCollection.setName(trimName);
            eventCollectionService.merge(eventCollection);
            eventCollectionOptionService.setOption(
                    eventCollection.getId(),
                    EventCollectionOptionType.MAX_SUBSCRIPTIONS,
                    normalizedMaxSubscribe);
            eventCollectionOptionService.setOption(
                    eventCollection.getId(),
                    EventCollectionOptionType.CHESS_EVENT_ID,
                    trim(chessEventId));
            invalidateEventCaches();
            registerService.promoteNextPendingSubscriptionInCollection(eventCollection);
            return redirectToEdit(eventCollection.getId(), "save");
        }

        if ("remove".equals(action)) {
            if (id == null) {
                return redirect("error", "missingId");
            }
            EventCollection eventCollection = eventCollectionService.find(id);
            if (eventCollection == null) {
                return redirect("error", "notFound");
            }
            if (eventCollectionService.countLinkedEvents(id) > 0) {
                return redirect("error", "linkedEvents");
            }
            eventCollectionService.remove(eventCollection);
            invalidateEventCaches();
            return redirectToAdminEvents();
        }

        String trimName = trim(name);
        if (trimName == null) {
            return redirect("error", "emptyName");
        }

        EventCollection eventCollection = new EventCollection();
        eventCollection.setName(trimName);
        eventCollectionService.persist(eventCollection);
        eventCollectionOptionService.setOption(
                eventCollection.getId(),
                EventCollectionOptionType.MAX_SUBSCRIPTIONS,
                normalizedMaxSubscribe);
        eventCollectionOptionService.setOption(
                eventCollection.getId(), EventCollectionOptionType.CHESS_EVENT_ID, trim(chessEventId));
        invalidateEventCaches();
        return redirectToEdit(eventCollection.getId(), "save");
    }

    @GET
    @Path("new")
    public JteHtml create(@QueryParam("error") String error) {
        Map<String, Object> model = new HashMap<>();
        model.put("eventCollection", new EventCollection());
        model.put("createMode", true);
        model.put("error", error);
        model.put("chessEventId", "");
        return new JteHtml(model, "event/eventcollectionEdit.jte");
    }

    @GET
    @Path("{id:\\d+}")
    public JteHtml edit(
            @PathParam("id") Integer id,
            @QueryParam("success") String success,
            @QueryParam("error") String error) {
        EventCollection eventCollection = eventCollectionService.find(id);
        if (eventCollection == null) {
            throw new NotFoundException();
        }
        eventCollectionService.fillSubscriptionLimits(eventCollection);
        // Force lazy loading of events
        eventCollection.getEvents().size();

        Map<String, Object> model = new HashMap<>();
        model.put("eventCollection", eventCollection);
        model.put("success", success);
        model.put("error", error);
        model.put("createMode", false);
        model.put(
                "chessEventId",
                eventCollectionOptionService.findOptionValue(
                        eventCollection.getId(), EventCollectionOptionType.CHESS_EVENT_ID));
        return new JteHtml(model, "event/eventcollectionEdit.jte");
    }

    private void invalidateEventCaches() {
        caches.getEvent().invalidateAll();
        caches.getAllEvents().invalidateAll();
    }

    private Response redirectToAdminEvents() {
        URI redirect = uriInfo.getBaseUriBuilder().path("admin/events").build();
        return Response.seeOther(redirect).build();
    }

    private Response redirect(String key, String value) {
        URI redirect = uriInfo.getBaseUriBuilder()
                .path("eventcollection")
                .queryParam(key, value)
                .build();
        return Response.seeOther(redirect).build();
    }

    private Response redirectToEdit(Integer id, String success) {
        URI redirect = uriInfo.getBaseUriBuilder()
                .path("eventcollection")
                .path(id.toString())
                .queryParam("success", success)
                .build();
        return Response.seeOther(redirect).build();
    }

    private Response redirectToEditError(Integer id, String error) {
        URI redirect = uriInfo.getBaseUriBuilder()
                .path("eventcollection")
                .path(id.toString())
                .queryParam("error", error)
                .build();
        return Response.seeOther(redirect).build();
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeOptionalInteger(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        try {
            int parsed = Integer.parseInt(trimmed);
            return parsed < 0 ? null : Integer.toString(parsed);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

package com.github.gcolin.event;

import com.github.gcolin.auth.LoggedUser;
import com.github.gcolin.auth.RequireRole;
import com.github.gcolin.auth.RoleCode;
import com.github.gcolin.platform.JteHtml;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("sharly/select")
@RequireRole(RoleCode.EVENT_ADMIN)
@Produces(MediaType.TEXT_HTML + ";charset=UTF-8")
public class SharlySelectApi {

    @Inject
    private SharlySelectService sharlySelectService;

    @Inject
    private SharlyImportTokenService sharlyImportTokenService;

    @Inject
    private SharlyCallbackAllowlist sharlyCallbackAllowlist;

    @Inject
    private LoggedUser loggedUser;

    @GET
    public JteHtml page(@QueryParam("callback") String callback) {
        if (!sharlyCallbackAllowlist.isAllowed(callback)) {
            Map<String, Object> model = new HashMap<>();
            model.put("error", "invalidCallback");
            model.put("callback", callback == null ? "" : callback);
            model.put("items", List.of());
            return new JteHtml(model, "event/sharlySelect.jte");
        }
        Map<String, Object> model = new HashMap<>();
        model.put("callback", callback.trim());
        model.put("items", sharlySelectService.listOpenItems());
        model.put("error", null);
        return new JteHtml(model, "event/sharlySelect.jte");
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response select(
            @FormParam("callback") String callback,
            @FormParam("kind") String kind,
            @FormParam("collectionId") Integer collectionId,
            @FormParam("eventId") Integer eventId) {
        if (!sharlyCallbackAllowlist.isAllowed(callback)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid callback").build();
        }
        SharlySelectItem item = sharlySelectService.resolveSelection(kind, collectionId, eventId);
        if (item == null) {
            Map<String, Object> model = new HashMap<>();
            model.put("callback", callback.trim());
            model.put("items", sharlySelectService.listOpenItems());
            model.put("error", "notFound");
            return Response.ok(new JteHtml(model, "event/sharlySelect.jte"))
                    .type(MediaType.TEXT_HTML_TYPE.withCharset("UTF-8"))
                    .build();
        }
        String jwt = sharlyImportTokenService.mint(
                loggedUser.getEmail(),
                item.getChessEventId(),
                item.getName(),
                item.getCollectionId(),
                item.getEventId());
        URI redirect = UriBuilder.fromUri(callback.trim())
                .queryParam("jwt", jwt)
                .queryParam("event_id", item.getChessEventId())
                .queryParam("name", item.getName())
                .build();
        return Response.seeOther(redirect).build();
    }
}

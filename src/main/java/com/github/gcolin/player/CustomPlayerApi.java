package com.github.gcolin.player;

import com.github.gcolin.auth.LoggedOnly;
import com.github.gcolin.auth.RequirePermission;
import com.github.gcolin.player.CustomPlayer;
import com.github.gcolin.auth.PermissionCode;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.platform.PagedList;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import com.github.gcolin.platform.Caches;
import com.github.gcolin.player.Find;
import com.github.gcolin.auth.LoggedUser;
import com.github.gcolin.registration.RegisterService;
import com.github.gcolin.player.CustomPlayerDao;
import com.github.gcolin.event.EventDao;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import com.github.gcolin.platform.JteHtml;

@Path("customplayer")
public class CustomPlayerApi {

    @Inject
    private CustomPlayerDao customPlayerService;

    @Inject
    private Caches caches;

    @Inject
    private LoggedUser loggerUser;

    @Inject
    private RegisterService registerService;

    @Inject
    private EventDao eventService;

    @Inject
    private Find find;

    @Context
    UriInfo uriInfo;

    @GET
    @RequirePermission(PermissionCode.EVENT_EDIT)
    public JteHtml players(
            @QueryParam("page") @DefaultValue("1") @Min(1) Integer page,
            @QueryParam("size") @DefaultValue("25") @Max(100) @Min(1) Integer size) {
        int start = (page - 1) * size;

        PagedList<CustomPlayer> paged = customPlayerService.pageSorted(start, size);
        long totalItems = paged.getTotal();
        int totalPages = (int) Math.max(1, (totalItems + size - 1) / size);
        if (page > totalPages) {
            page = totalPages;
            start = (page - 1) * size;
            paged = customPlayerService.pageSorted(start, size);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("players", paged.getElements());
        model.put("currentPage", page);
        model.put("pageSize", size);
        model.put("totalItems", totalItems);
        model.put("totalPages", totalPages);
        model.put("hasPrev", page > 1);
        model.put("hasNext", page < totalPages);
        model.put("prevPage", Math.max(1, page - 1));
        model.put("nextPage", Math.min(totalPages, page + 1));
        model.put("startIndex", start);
        return new JteHtml(model, "player/players.jte");
    }

    @GET
    @LoggedOnly
    @Path("new")
    public JteHtml newplayer(
            @QueryParam("eventId") Integer eventId)
            throws ServletException, IOException {
        return edit(null, eventId);
    }

    @GET
    @LoggedOnly
    @Path("{id:\\d+}")
    public JteHtml edit(
            @PathParam("id") Integer id,
            @QueryParam("eventId") Integer eventId)
            throws ServletException, IOException {
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("eventId", eventId);

        if (id != null) {
            model.put("player", customPlayerService.find(id));
        } else {
            CustomPlayer player = new CustomPlayer();
            player.setLicence("");
            player.setName("");
            player.setFirstname("");
            player.setBirthDate("");
            player.setElo("");
            player.setCreationUser("");
            model.put("player", player);
        }
        return new JteHtml(model, "player/customplayer.jte");
    }

    @POST
    @LoggedOnly
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response save(
            @FormParam("id") Integer id,
            @FormParam("eventId") Integer eventId,
            @FormParam("licence") String licence,
            @FormParam("name") String name,
            @FormParam("birthdate") String birthdate,
            @FormParam("firstname") String firstname,
            @FormParam("gender") String gender,
            @FormParam("elo") String elo) {

        if (id == null && eventId != null) {
            String trimmedLicence = trim(licence);
            if (trimmedLicence != null) {
                var existingPlayer = find.player(trimmedLicence, null);
                if (existingPlayer != null && !existingPlayer.isEditable()) {
                    PlayerSubscription sub = registerService.registerPlayerToEvent(
                            eventService.find(eventId), existingPlayer, loggerUser.getEmail());
                    return redirectAfterRegister(eventId, sub);
                }
            }
        }

        CustomPlayer player = new CustomPlayer();
        player.setLicence(licence);
        player.setName(name);
        player.setBirthDate(birthdate);
        player.setFirstname(firstname);
        player.setGender("true".equals(gender));
        player.setElo(elo);

        if (id != null) {
            player.setId(id);
            if (loggerUser.hasPermission(PermissionCode.EVENT_EDIT)) {
                player.setCreationUser(loggerUser.getEmail());
            }
        } else {
            player.setCreationUser(loggerUser.getEmail());
        }
        caches.getDebtCache().invalidateAll();
        if (id == null) {
            customPlayerService.persist(player);
        } else {
            customPlayerService.merge(player);
        }

        if (eventId != null) {
            PlayerSubscription sub =
                    registerService.registerPlayerToEvent(eventService.find(eventId), player, loggerUser.getEmail());
            return redirectAfterRegister(eventId, sub);
        }

        URI uri = uriInfo.getBaseUriBuilder()
                .path("event")
                .path("my")
                .queryParam("success", "register")
                .build();
        return Response.seeOther(uri).build();
    }

    private Response redirectAfterRegister(Integer eventId, PlayerSubscription sub) {
        if (sub != null && sub.getStatus() == PlayerSubscriptionStatus.PAID) {
            URI uri = uriInfo.getBaseUriBuilder()
                    .path("event")
                    .path(eventId.toString())
                    .queryParam("success", "register")
                    .build();
            return Response.seeOther(uri).build();
        }
        URI uri = uriInfo.getBaseUriBuilder()
                .path("event")
                .path("my")
                .queryParam("success", "register")
                .build();
        return Response.seeOther(uri).build();
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

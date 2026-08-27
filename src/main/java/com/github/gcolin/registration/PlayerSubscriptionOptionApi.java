package com.github.gcolin.registration;

import com.github.gcolin.auth.RequirePermission;
import com.github.gcolin.auth.PermissionCode;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.registration.PlayerSubscriptionOption;
import com.github.gcolin.platform.Transactional;
import com.github.gcolin.registration.PlayerSubscriptionOptionStatus;
import com.github.gcolin.platform.Caches;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import com.github.gcolin.registration.PlayerSubscriptionOptionDao;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import com.github.gcolin.platform.JteHtml;

@Path("event/{id:\\d+}/register/{subId:\\d+}/option")
@RequirePermission(PermissionCode.EVENT_EDIT)
public class PlayerSubscriptionOptionApi {

    @Inject
    private PlayerSubscriptionDao playerSubscriptionService;

    @Inject
    private PlayerSubscriptionOptionDao playerSubscriptionOptionService;

    @Inject
    private Caches caches;

    @Context
    UriInfo uriInfo;

    @GET
    @Path("new")
    public JteHtml createForm(@PathParam("id") Integer eventId, @PathParam("subId") Integer subId) {
        PlayerSubscription sub = requireSubscription(eventId, subId);
        Map<String, Object> model = new HashMap<>();
        model.put("sub", sub);
        model.put("option", null);
        model.put("isNew", true);
        return new JteHtml(model, "registration/subOptionEdit.jte");
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response create(
            @PathParam("id") Integer eventId,
            @PathParam("subId") Integer subId,
            @FormParam("description") String description,
            @FormParam("amountCents") Long amountCents,
            @FormParam("status") String status) {
        PlayerSubscription sub = requireSubscription(eventId, subId);
        PlayerSubscriptionOption option = new PlayerSubscriptionOption();
        option.setPlayerSubscription(sub);
        option.setDescription(description);
        option.setAmountCents(amountCents != null ? amountCents : 0L);
        option.setStatus(parseStatus(status));
        if (option.getAmountCents() == 0L) {
            option.setStatus(PlayerSubscriptionOptionStatus.PAID);
        }
        playerSubscriptionOptionService.persist(option);
        caches.getEvent().invalidateAll();
        caches.getDebtCache().invalidateAll();
        return redirectToSubEdit(eventId, subId);
    }

    @GET
    @Path("{optionId:\\d+}")
    public JteHtml editForm(
            @PathParam("id") Integer eventId,
            @PathParam("subId") Integer subId,
            @PathParam("optionId") Integer optionId) {
        PlayerSubscription sub = requireSubscription(eventId, subId);
        PlayerSubscriptionOption option = requireOption(subId, optionId);
        Map<String, Object> model = new HashMap<>();
        model.put("sub", sub);
        model.put("option", option);
        model.put("isNew", false);
        return new JteHtml(model, "registration/subOptionEdit.jte");
    }

    @POST
    @Path("{optionId:\\d+}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response save(
            @PathParam("id") Integer eventId,
            @PathParam("subId") Integer subId,
            @PathParam("optionId") Integer optionId,
            @FormParam("toRemove") String toRemove,
            @FormParam("description") String description,
            @FormParam("amountCents") Long amountCents,
            @FormParam("status") String status) {
        requireSubscription(eventId, subId);
        PlayerSubscriptionOption option = requireOption(subId, optionId);
        if ("true".equals(toRemove)) {
            playerSubscriptionOptionService.remove(option);
            caches.getEvent().invalidateAll();
            caches.getDebtCache().invalidateAll();
            return redirectToSubEdit(eventId, subId);
        }
        option.setDescription(description);
        option.setAmountCents(amountCents != null ? amountCents : 0L);
        option.setStatus(parseStatus(status));
        playerSubscriptionOptionService.merge(option);
        caches.getEvent().invalidateAll();
        caches.getDebtCache().invalidateAll();
        return redirectToSubEdit(eventId, subId);
    }

    private PlayerSubscription requireSubscription(Integer eventId, Integer subId) {
        PlayerSubscription sub = playerSubscriptionService.findWithEvent(subId);
        if (sub == null || sub.getEvent() == null || !eventId.equals(sub.getEvent().getId())) {
            throw new WebApplicationException("PlayerSubscription not found", Status.NOT_FOUND);
        }
        return sub;
    }

    private PlayerSubscriptionOption requireOption(Integer subId, Integer optionId) {
        PlayerSubscriptionOption option = playerSubscriptionOptionService.findWithSubscription(optionId);
        if (option == null
                || option.getPlayerSubscription() == null
                || !subId.equals(option.getPlayerSubscription().getId())) {
            throw new WebApplicationException("PlayerSubscriptionOption not found", Status.NOT_FOUND);
        }
        return option;
    }

    private PlayerSubscriptionOptionStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return PlayerSubscriptionOptionStatus.NOT_PAID;
        }
        return PlayerSubscriptionOptionStatus.valueOf(status);
    }

    private Response redirectToSubEdit(Integer eventId, Integer subId) {
        URI uri = uriInfo.getBaseUriBuilder()
                .path("event")
                .path(eventId.toString())
                .path("register")
                .path(subId.toString())
                .build();
        return Response.seeOther(uri).build();
    }
}

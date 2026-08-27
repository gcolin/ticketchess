package com.github.gcolin.registration;

import com.github.gcolin.auth.LoggedOnly;
import com.github.gcolin.auth.RequirePermission;
import com.github.gcolin.player.CustomPlayer;
import com.github.gcolin.event.Event;
import com.github.gcolin.auth.PermissionCode;
import com.github.gcolin.registration.PlayerPendingSubscription;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.platform.Transactional;
import com.github.gcolin.player.LuceneDb;
import com.github.gcolin.player.DisplayPlayer;
import com.github.gcolin.event.EventType;
import com.github.gcolin.player.IPlayer;
import com.github.gcolin.player.Player;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import com.github.gcolin.platform.Caches;
import com.github.gcolin.player.Find;
import com.github.gcolin.auth.LoggedUser;
import com.github.gcolin.registration.RegisterService;
import com.github.gcolin.platform.ServiceUtils;
import com.github.gcolin.player.CustomPlayerDao;
import com.github.gcolin.event.EventDao;
import com.github.gcolin.registration.PlayerPendingSubscriptionDao;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import com.github.gcolin.registration.PlayerSubscriptionOptionDao;
import com.github.gcolin.payment.PaymentMail;
import com.github.gcolin.platform.SendMail;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.queryparser.classic.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.gcolin.platform.JteHtml;

@Path("event/{id:\\d+}/register")
public class EventRegisterApi {

    @Inject
    private RegisterService registerService;

    @Inject
    private CustomPlayerDao customPlayerService;

    @Inject
    private LuceneDb luceneDb;

    @Inject
    private EventDao eventService;

    @Inject
    private PlayerSubscriptionDao playerSubscriptionService;

    @Inject
    private PlayerSubscriptionOptionDao playerSubscriptionOptionService;

    @Inject
    private PlayerPendingSubscriptionDao playerPendingSubscriptionDao;

    @Inject
    private LoggedUser loggerUser;

    @Inject
    private Find find;

    @Inject
    private Caches caches;

    @Context
    UriInfo uriInfo;

    @Inject
    private SendMail mail;

    private static final Logger logger = LoggerFactory.getLogger(EventRegisterApi.class);

    @GET
    @LoggedOnly
    public JteHtml register(@PathParam("id") Integer eventId, @QueryParam("query") String query) {
        Map<String, Object> model = new HashMap<String, Object>();
        Event event = eventService.find(eventId);
        eventService.fillSubscriptionLimits(event);
        model.put("event", event);
        String registrationClosedMessageKey = getRegistrationClosedMessageKey(event);
        model.put("registrationClosed", registrationClosedMessageKey != null);
        model.put("registrationClosedMessageKey", registrationClosedMessageKey);
        Set<String> subscriptionsRefs = new HashSet<>();
        List<PlayerSubscription> subscriptions = playerSubscriptionService.findByEvent(event);
        for (PlayerSubscription sub : subscriptions) {
            if (sub.getStatus() != PlayerSubscriptionStatus.CANCELLED) {
                subscriptionsRefs.add(sub.getNrFfe());
            }
        }
        if (event.getEventCollection() != null && event.getEventCollection().getId() != null) {
            subscriptionsRefs.addAll(
                    playerSubscriptionService.findActiveRefsByEventCollection(event.getEventCollection().getId()));
        }

        if (query != null && !query.isEmpty()) {
            List<Player> players;
            try {
                players = luceneDb.searchJoueur(query, 20, null);
            } catch (ParseException | IOException e) {
                throw new WebApplicationException(e);
            }

            List<DisplayPlayer> playersFiltered = new ArrayList<>();
            for (Player player : players) {
                DisplayPlayer p = new DisplayPlayer(player);
                if (!subscriptionsRefs.contains(player.getNrffe())) {
                    p.setPrice(ServiceUtils.toEuros(ServiceUtils.calculatePrice(player, event)));
                    if (event.getEventType() == EventType.RAPID) {
                        p.setRating(player.getRapidRating());
                    }
                    if (event.getEventType() == EventType.BLITZ) {
                        p.setRating(player.getBlitzRating());
                    } else {
                        p.setRating(player.getRating());
                    }
                    playersFiltered.add(p);
                }
            }
            playersFiltered.sort((p1, p2) -> {
                int nameCompare = p1.getName().compareTo(p2.getName());
                if (nameCompare != 0) {
                    return nameCompare;
                }
                return p1.getFirstname().compareTo(p2.getFirstname());
            });
            model.put("players", playersFiltered);
            model.put("query", query);
        } else if (query != null) {
            model.put("players", Collections.emptyList());
        }

        List<PlayerSubscription> userSubscriptions =
                playerSubscriptionService.findByCreationUserWithEvents(loggerUser.getEmail());
        List<DisplayPlayer> favoritePlayers = new ArrayList<>();
        Set<String> ids = new HashSet<String>();
        ids.addAll(subscriptionsRefs);
        for (PlayerSubscription sub : userSubscriptions) {
            if (!sub.getEvent().getId().equals(eventId) && !ids.contains(sub.getNrFfe())) {
                IPlayer player = find.player(sub.getNrFfe(), null);
                ids.add(sub.getNrFfe());
                if (player != null) {
                    DisplayPlayer p = new DisplayPlayer(player);
                    p.setPrice(ServiceUtils.toEuros(ServiceUtils.calculatePrice(player, event)));
                    if (event.getEventType() == EventType.RAPID) {
                        p.setRating(player.getRapidRating());
                    }
                    if (event.getEventType() == EventType.BLITZ) {
                        p.setRating(player.getBlitzRating());
                    } else {
                        p.setRating(player.getRating());
                    }
                    favoritePlayers.add(p);
                }
            }
            if (sub.getEvent().getId().equals(eventId)) {
                ids.add(sub.getNrFfe());
            }
        }

        List<CustomPlayer> internalPlayers = customPlayerService.findByCreationUser(loggerUser.getEmail());
        for (CustomPlayer customPlayer : internalPlayers) {
            if (!ids.contains(customPlayer.getNrffe())) {
                DisplayPlayer p = new DisplayPlayer(customPlayer);
                p.setPrice(ServiceUtils.toEuros(ServiceUtils.calculatePrice(customPlayer, event)));
                p.setRating(customPlayer.getRating());
                favoritePlayers.add(p);
            }
        }

        favoritePlayers.sort((p1, p2) -> {
            int nameCompare = p1.getName().compareTo(p2.getName());
            if (nameCompare != 0) {
                return nameCompare;
            }
            return p1.getFirstname().compareTo(p2.getFirstname());
        });
        model.put("favoritePlayers", favoritePlayers);

        return new JteHtml(model, "registration/register.jte");
    }

    private String getRegistrationClosedMessageKey(Event event) {
        if (event == null) {
            return null;
        }
        if (event.getEventCollection() != null
                && event.getEventCollection().getMaxSubscribe() != null
                && event.getEventCollection().getMaxSubscribe() > 0
                && event.getEventCollection().getNbSubscriptions() != null
                && event.getEventCollection().getNbSubscriptions() >= event.getEventCollection().getMaxSubscribe()) {
            return "event.collectionFull";
        }
        if (event.getMaxSubscriptions() != null && event.getMaxSubscriptions() > 0) {
            long currentSubscriptions = event.getSubscriptions() == null
                    ? 0
                    : event.getSubscriptions().stream()
                            .filter(sub -> sub.getStatus() != PlayerSubscriptionStatus.CANCELLED)
                            .count();
            if (currentSubscriptions >= event.getMaxSubscriptions()) {
                return "event.full";
            }
        }
        return null;
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @LoggedOnly
    public Response registerSave(@PathParam("id") Integer eventId, @FormParam("nrffe") @NotNull String nrffe) {
        PlayerSubscription sub = registerService.registerPlayerToEvent(eventId, nrffe, loggerUser.getEmail());
        caches.getEvent().invalidateAll();
        caches.getDebtCache().invalidateAll();
        if (sub == null) {
            Event event = eventService.find(eventId);
            PlayerPendingSubscription pending = playerPendingSubscriptionDao.findByEventAndNrffe(event, nrffe);
            if (pending == null) {
                throw new WebApplicationException(Status.CONFLICT);
            }

            URI uri = uriInfo.getBaseUriBuilder()
                    .path("event")
                    .path("my")
                    .queryParam("success", "pending")
                    .build();
            return Response.seeOther(uri).build();
        }
        if (sub.getStatus() == PlayerSubscriptionStatus.PAID) {
            URI uri = uriInfo.getBaseUriBuilder()
                    .path("event")
                    .path(sub.getEvent().getId().toString())
                    .queryParam("success", "register")
                    .build();
            return Response.seeOther(uri).build();
        } else {
            URI uri = uriInfo.getBaseUriBuilder()
                    .path("event")
                    .path("my")
                    .queryParam("success", "register")
                    .build();
            return Response.seeOther(uri).build();
        }
    }

    @GET
    @Path("{subId}")
    @RequirePermission(PermissionCode.EVENT_EDIT)
    public JteHtml registeredit(@PathParam("id") Integer eventId, @PathParam("subId") Integer subId) {
        PlayerSubscription sub = playerSubscriptionService.findWithEvent(subId);
        Map<String, Object> model = new HashMap<>();
        model.put("sub", sub);
        model.put("player", find.player(sub.getNrFfe(), null));
        model.put("options", playerSubscriptionOptionService.findByPlayerSubscription(subId));
        return new JteHtml(model, "registration/subEdit.jte");
    }

    @POST
    @Path("{subId}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RequirePermission(PermissionCode.EVENT_EDIT)
    @Transactional
    public Response registerEditSave(
            @FormParam("eventId") Integer eventId,
            @PathParam("id") Integer eventIdPath,
            @PathParam("subId") Integer subId,
            @FormParam("toRemove") String toRemove,
            @FormParam("creationUser") String creationUser,
            @FormParam("nrFFE") String nrFFE,
            @FormParam("status") String status,
            @FormParam("amountCents") Long amountCents) {
        if (toRemove.equals("true")) {
            PlayerSubscription removed = playerSubscriptionService.findWithEvent(subId);
            playerSubscriptionOptionService.removeByPlayerSubscription(subId);
            playerSubscriptionService.remove(subId);
            caches.getEvent().invalidateAll();
            caches.getDebtCache().invalidateAll();
            if (removed != null && removed.getEvent() != null) {
                registerService.promoteNextPendingSubscription(removed.getEvent());
            }
            URI uri = uriInfo.getBaseUriBuilder()
                    .path("event")
                    .path(eventId.toString())
                    .build();
            return Response.seeOther(uri).build();
        }
        PlayerSubscription prevSub = playerSubscriptionService.find(subId);

        PlayerSubscription sub = new PlayerSubscription();
        sub.setId(subId);
        sub.setNrFfe(nrFFE);
        sub.setEvent(eventService.find(eventId));
        sub.setCreationUser(creationUser);
        sub.setStatus(PlayerSubscriptionStatus.valueOf(status));
        sub.setPayment(prevSub == null ? null : prevSub.getPayment());
        sub.setAmountCents(amountCents != null ? amountCents : (prevSub == null ? null : prevSub.getAmountCents()));
        sub.setAttendanceAt(prevSub == null ? null : prevSub.getAttendanceAt());

        PlayerSubscription prevSubDetached = playerSubscriptionService.detach(prevSub);
        playerSubscriptionService.merge(sub);

        if (prevSubDetached != null
                && prevSubDetached.getStatus() == PlayerSubscriptionStatus.NOT_PAID
                && sub.getStatus() == PlayerSubscriptionStatus.PAID) {
            IPlayer player = find.player(sub.getNrFfe(), null);
            Event event = sub.getEvent();

            SimpleDateFormat sdf = new SimpleDateFormat("EEEE dd MMMM yyyy", Locale.FRANCE);
            PaymentMail registrationMail = new PaymentMail();
            registrationMail.setName(player.getFirstname() + " " + player.getName());
            double eventPrice = ServiceUtils.toEuros(ServiceUtils.calculatePrice(player, event));
            if (eventPrice > 0) {
                registrationMail.setAmount(eventPrice + " Euros");
            }
            registrationMail.setEvenDate(sdf.format(event.getStartDateAsDate()));
            registrationMail.setEventName(event.getName());
            registrationMail.setReference(sub.getCreationUser());

            try {
                mail.send(registrationMail, sub.getCreationUser(), "Confirmation paiement " + event.getName());
            } catch (Exception e) {
                logger.error(
                        "[email={},player={},event={}] cannot send email. {}",
                        sub.getCreationUser(),
                        sub.getNrFfe(),
                        event.getId(),
                        e.toString());
            }
        }

        caches.getEvent().invalidateAll();
        caches.getDebtCache().invalidateAll();

        URI uri = uriInfo.getBaseUriBuilder()
                .path("event")
                .path(eventIdPath.toString())
                .build();
        return Response.seeOther(uri).build();
    }

    @POST
    @Path("{subId}/attendance")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RequirePermission(PermissionCode.EVENT_EDIT)
    @Transactional
    public Response markAttendance(
            @PathParam("id") Integer eventId,
            @PathParam("subId") Integer subId,
            @FormParam("present") Boolean present) {
        PlayerSubscription sub = playerSubscriptionService.findWithEvent(subId);
        if (sub == null || sub.getEvent() == null || !eventId.equals(sub.getEvent().getId())) {
            throw new WebApplicationException("PlayerSubscription not found", Status.NOT_FOUND);
        }
        boolean markPresent = present == null || present;
        sub.setAttendanceAt(markPresent ? LocalDateTime.now() : null);
        playerSubscriptionService.merge(sub);
        caches.getEvent().invalidateAll();

        URI uri = uriInfo.getBaseUriBuilder()
                .path("event")
                .path(eventId.toString())
                .build();
        return Response.seeOther(uri).build();
    }
}

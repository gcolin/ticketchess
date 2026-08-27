package com.github.gcolin.event;

import com.github.gcolin.platform.Config;
import com.github.gcolin.auth.LoggedOnly;
import com.github.gcolin.auth.RequirePermission;
import com.github.gcolin.player.CustomPlayer;
import com.github.gcolin.event.Event;
import com.github.gcolin.event.EventCollectionOptionType;
import com.github.gcolin.event.EventInfo;
import com.github.gcolin.event.EventOption;
import com.github.gcolin.event.EventOptionType;
import com.github.gcolin.auth.PermissionCode;
import com.github.gcolin.registration.PlayerPendingSubscription;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.platform.Transactional;
import com.github.gcolin.player.DisplayPlayer;
import com.github.gcolin.event.EventStatus;
import com.github.gcolin.player.IPlayer;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import com.github.gcolin.platform.Caches;
import com.github.gcolin.desk.EventDeskHub;
import com.github.gcolin.desk.EventDeskService;
import com.github.gcolin.event.EventGroupFilter;
import com.github.gcolin.player.Find;
import com.github.gcolin.auth.LoggedUser;
import com.github.gcolin.registration.RegisterService;
import com.github.gcolin.platform.ServiceUtils;
import com.github.gcolin.event.EventCollectionDao;
import com.github.gcolin.event.EventCollectionOptionDao;
import com.github.gcolin.event.EventDao;
import com.github.gcolin.event.EventGroupDao;
import com.github.gcolin.event.EventInfoDao;
import com.github.gcolin.event.EventOptionDao;
import com.github.gcolin.payment.PaymentDao;
import com.github.gcolin.registration.PlayerPendingSubscriptionDao;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import com.github.gcolin.event.EventCache;
import com.github.gcolin.event.EventsCache;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
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
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.gcolin.platform.JteHtml;
import com.github.gcolin.player.Player;

@Path("event")
public class EventApi {

    @Inject
    private Caches caches;

    @Inject
    EventDao eventService;

    @Inject
    private EventInfoDao eventInfoService;

    @Inject
    private EventOptionDao eventOptionService;

    @Inject
    EventGroupDao eventGroupService;

    @Inject
    EventCollectionDao eventCollectionService;

    @Inject
    private EventCollectionOptionDao eventCollectionOptionService;

    @Inject
    EventGroupFilter eventGroupFilter;

    @Inject
    private PlayerSubscriptionDao playerSubscriptionService;

    @Inject
    private PlayerPendingSubscriptionDao playerPendingSubscriptionDao;

    @Inject
    private LoggedUser loggerUser;

    @Inject
    private Find find;

    @Inject
    private Properties properties;

    @Context
    UriInfo uriInfo;

    @Inject
    private RegisterService registerService;

    @Inject
    private PaymentDao paymentDao;

    @Inject
    private Config config;

    @Inject
    private EventDeskHub eventDeskHub;

    @Inject
    private EventDeskService eventDeskService;

    private static final Logger logger = LoggerFactory.getLogger(EventApi.class);

    @GET
    public JteHtml events(@QueryParam("status") @DefaultValue("ACTIVE") String status) throws IOException {
        return events(status, null);
    }

    @GET
    @LoggedOnly
    @Path("my")
    public JteHtml myevents(
            @QueryParam("success") String success,
            @QueryParam("display") String display)
            throws IOException {

        List<PlayerSubscription> subs = playerSubscriptionService.findByCreationUserWithEvents(loggerUser.getEmail());
        List<PlayerPendingSubscription> pendingSubs =
            playerPendingSubscriptionDao.findByCreationUserWithEvent(loggerUser.getEmail());

        List<Event> events = new ArrayList<Event>();
        for (PlayerSubscription s : subs) {
            Event event = null;
            for (Event evt : events) {
                if (evt.getId() == s.getEvent().getId()) {
                    event = evt;
                    break;
                }
            }
            if (event == null) {
                events.add(s.getEvent());
                event = s.getEvent();
                event.setPlayers(new ArrayList<DisplayPlayer>());
                eventService.fillSubscriptionLimits(event);
            }
            IPlayer p = find.player(s.getNrFfe(), null);
            if (p == null) {
                logger.warn(
                        "cannot find player {} subscribed to event {}",
                        s.getNrFfe(),
                        s.getEvent().getName());
                continue;
            }
            DisplayPlayer player = new DisplayPlayer(p);
            if (s.getStatus() == PlayerSubscriptionStatus.NOT_PAID) {
                player.setPrice(ServiceUtils.toEuros(ServiceUtils.calculatePrice(p, event)));
            } else if (s.getStatus() == PlayerSubscriptionStatus.PAID && s.getAmountCents() != null) {
                player.setPrice(ServiceUtils.toEuros(s.getAmountCents()));
            }
            player.setRating(p, event.getEventType());
            if (p.isEditable()
                    && (loggerUser.hasPermission(PermissionCode.EVENT_EDIT)
                            || (p instanceof CustomPlayer
                                    && loggerUser.getEmail().equals(((CustomPlayer) p).getCreationUser())))) {
                player.setEditable(true);
            }
            player.setStatus(s.getStatus());
            player.setSubId(s.getId());
            player.setAttendanceAt(s.getAttendanceAt());
            event.getPlayers().add(player);
        }
        boolean showFinished = "all".equalsIgnoreCase(display);
        LocalDateTime now = LocalDateTime.now();
        boolean hasFinishedEvents = events.stream()
                .anyMatch(event -> event.getEndDate() != null && event.getEndDate().plusDays(1).isBefore(now));
        if (!showFinished) {
            events.removeIf(event -> event.getEndDate() != null && event.getEndDate().plusDays(1).isBefore(now));
        }

        // Group events by their collection while keeping the original start date order.
        events.sort(Comparator.comparing(Event::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        List<Event> pendingEvents = new ArrayList<Event>();
        Map<Integer, Boolean> collectionQueueEnabledById = new HashMap<>();
        Map<Integer, Map<Integer, Integer>> pendingPositionsByEventId = new HashMap<>();
        Map<Integer, Map<Integer, Integer>> pendingPositionsByCollectionId = new HashMap<>();
        for (PlayerPendingSubscription pending : pendingSubs) {
            Event event = null;
            for (Event evt : pendingEvents) {
                if (evt.getId() == pending.getEvent().getId()) {
                    event = evt;
                    break;
                }
            }
            if (event == null) {
                Event sourceEvent = pending.getEvent();
                Event pendingEventView = new Event();
                pendingEventView.setId(sourceEvent.getId());
                pendingEventView.setName(sourceEvent.getName());
                pendingEventView.setStartDate(sourceEvent.getStartDate());
                pendingEventView.setEndDate(sourceEvent.getEndDate());
                pendingEventView.setEventType(sourceEvent.getEventType());
                pendingEventView.setEventCollection(sourceEvent.getEventCollection());
                pendingEvents.add(pendingEventView);
                event = pendingEventView;
                event.setPlayers(new ArrayList<DisplayPlayer>());
            }

            IPlayer p = find.player(pending.getNrFfe(), null);
            if (p == null) {
                logger.warn(
                        "cannot find player {} pending for event {}",
                        pending.getNrFfe(),
                        pending.getEvent().getName());
                continue;
            }

            DisplayPlayer player = new DisplayPlayer(p);
            player.setRating(p, event.getEventType());
            Integer ahead = resolvePendingQueueAhead(
                    pending,
                    collectionQueueEnabledById,
                    pendingPositionsByEventId,
                    pendingPositionsByCollectionId) - config.getPendingQueueOffset();
            if(ahead < 0) {
                ahead = 0;
            }
            player.setPendingQueueAhead(ahead);
            if (p.isEditable()
                    && (loggerUser.hasPermission(PermissionCode.EVENT_EDIT)
                            || (p instanceof CustomPlayer
                                    && loggerUser.getEmail().equals(((CustomPlayer) p).getCreationUser())))) {
                player.setEditable(true);
            }
            event.getPlayers().add(player);
        }

        hasFinishedEvents = hasFinishedEvents
                || pendingEvents.stream()
                        .anyMatch(event -> event.getEndDate() != null && event.getEndDate().plusDays(1).isBefore(now));
        if (!showFinished) {
            pendingEvents.removeIf(event -> event.getEndDate() != null && event.getEndDate().plusDays(1).isBefore(now));
        }

        pendingEvents.sort(
                Comparator.comparing(Event::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        // Build toggle URLs
        var toggleDisplayUrlBuilder = uriInfo.getBaseUriBuilder().path("event").path("my");
        if (!showFinished) {
        	toggleDisplayUrlBuilder.queryParam("display", "all");
        }
        String toggleDisplayUrl = toggleDisplayUrlBuilder.build().toString();

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("events", events);
        model.put("pendingEvents", pendingEvents);
        model.put("showFinished", showFinished);
        model.put("hasFinishedEvents", hasFinishedEvents);
        model.put("toggleDisplayUrl", toggleDisplayUrl);
        model.put("stripePublic", Config.getStripePublicKey(properties));
        model.put("stripeSimulated", Config.isStripeSimulated(properties));
        model.put("success", success);
        model.put("paidPayments", paymentDao.findAllPaidNotFreeByUser(loggerUser.getEmail()));

        return new JteHtml(model, "event/myevents.jte");
    }

    private Integer resolvePendingQueueAhead(
            PlayerPendingSubscription pending,
            Map<Integer, Boolean> collectionQueueEnabledById,
            Map<Integer, Map<Integer, Integer>> pendingPositionsByEventId,
            Map<Integer, Map<Integer, Integer>> pendingPositionsByCollectionId) {
        Event pendingEvent = pending.getEvent();
        if (pendingEvent == null || pendingEvent.getId() == null) {
            return 0;
        }

        if (useEventCollectionPendingQueue(pendingEvent, collectionQueueEnabledById)) {
            Integer collectionId = pendingEvent.getEventCollection().getId();
            Map<Integer, Integer> pendingPositions = pendingPositionsByCollectionId.computeIfAbsent(
                    collectionId,
                    id -> buildPendingPositionsById(playerPendingSubscriptionDao.findByEventCollection(id)));
            return pending.getId() == null ? 0 : pendingPositions.getOrDefault(pending.getId(), 0);
        }

        Map<Integer, Integer> pendingPositions = pendingPositionsByEventId.computeIfAbsent(
                pendingEvent.getId(),
                id -> buildPendingPositionsById(playerPendingSubscriptionDao.findByEvent(pendingEvent)));
        return pending.getId() == null ? 0 : pendingPositions.getOrDefault(pending.getId(), 0);
    }

    private Map<Integer, Integer> buildPendingPositionsById(List<PlayerPendingSubscription> pendingSubscriptions) {
        Map<Integer, Integer> positionsById = new HashMap<>();
        int index = 0;
        for (PlayerPendingSubscription pendingSubscription : pendingSubscriptions) {
            if (pendingSubscription.getId() != null) {
                positionsById.put(pendingSubscription.getId(), index);
            }
            index++;
        }
        return positionsById;
    }

    private boolean useEventCollectionPendingQueue(Event event, Map<Integer, Boolean> collectionQueueEnabledById) {
        if (event == null || event.getEventCollection() == null || event.getEventCollection().getId() == null) {
            return false;
        }
        if (eventCollectionOptionService == null) {
            return false;
        }

        Integer collectionId = event.getEventCollection().getId();
        return collectionQueueEnabledById.computeIfAbsent(collectionId, id -> {
            Integer maxCollectionSubscriptions =
                    eventCollectionOptionService.findIntOptionValue(id, EventCollectionOptionType.MAX_SUBSCRIPTIONS);
            return maxCollectionSubscriptions != null && maxCollectionSubscriptions > 0;
        });
    }

    @GET
    @LoggedOnly
    @Path("my/{playersubscriptionId}/eventcollection/{eventCollectionId}")
    public JteHtml selectEventCollectionVariant(
            @PathParam("playersubscriptionId") Integer playersubscriptionId,
            @PathParam("eventCollectionId") Integer eventCollectionId) {

        PlayerSubscription sub = playerSubscriptionService.find(playersubscriptionId);
        if (sub == null) {
            throw new WebApplicationException("PlayerSubscription not found", Response.Status.NOT_FOUND);
        }

        if (!sub.getEvent().getEventCollection().getId().equals(eventCollectionId)) {
            throw new WebApplicationException("EventCollection does not match", Response.Status.BAD_REQUEST);
        }

        // Get player for rating validation
        IPlayer player = find.player(sub.getNrFfe(), null);
        if (player == null) {
            throw new WebApplicationException("Player not found", Response.Status.NOT_FOUND);
        }

        Integer currentEventId = sub.getEvent().getId();

        // Build map of selectable events and ineligibility reasons.
        Map<String, Boolean> canSelectEvent = new HashMap<>();
        Map<String, String> ineligibleReason = new HashMap<>();
        for (Event event : sub.getEvent().getEventCollection().getEvents()) {
            boolean canSelect = true;
            String reasonKey = null;

            // Get MIN_ELO and MAX_ELO options
            List<EventOption> options = eventOptionService.findByEventId(event.getId());
            Integer minElo = null;
            Integer maxElo = null;
            Integer maxSubscriptions = null;

            for (EventOption option : options) {
                try {
                    if (option.getOptionType() == EventOptionType.MIN_ELO) {
                        minElo = Integer.parseInt(option.getValue());
                    } else if (option.getOptionType() == EventOptionType.MAX_ELO) {
                        maxElo = Integer.parseInt(option.getValue());
                    } else if (option.getOptionType() == EventOptionType.MAX_SUBSCRIPTIONS) {
                        maxSubscriptions = Integer.parseInt(option.getValue());
                    }
                } catch (NumberFormatException e) {
                    logger.debug("Invalid ELO value for event {}: {}", event.getId(), option.getValue());
                }
            }

            // Get player rating for this event type
            String ratingStr = "0";
            if (event.getEventType() == com.github.gcolin.event.EventType.RAPID) {
                ratingStr = player.getRapidRating() != null ? player.getRapidRating() : "0";
            } else if (event.getEventType() == com.github.gcolin.event.EventType.BLITZ) {
                ratingStr = player.getBlitzRating() != null ? player.getBlitzRating() : "0";
            } else {
                ratingStr = player.getRating() != null ? player.getRating() : "0";
            }
            // Extract numeric part only (handles ratings like "1550F" or "1550N")
            String numericRating = ratingStr.replaceAll("[^0-9]", "");
            Integer playerRating = 0;
            try {
                playerRating = Integer.parseInt(numericRating.isEmpty() ? "0" : numericRating);
            } catch (NumberFormatException e) {
                logger.debug("Invalid player rating for {}: {}", sub.getNrFfe(), ratingStr);
            }

            // Check if rating is within range
            if (minElo != null && playerRating < minElo) {
                canSelect = false;
                reasonKey = "eloTooLow";
            }
            if (maxElo != null && playerRating > maxElo) {
                canSelect = false;
                reasonKey = "eloTooHigh";
            }

            // Check if event has remaining subscriptions
            if (maxSubscriptions != null && maxSubscriptions > 0) {
                int currentCount = event.getSubscriptions() != null
                        ? event.getSubscriptions().size()
                        : 0;
                if (currentCount >= maxSubscriptions) {
                    canSelect = false;
                    reasonKey = "eventFull";
                }
            }

            // Keep current selection enabled even if it no longer matches constraints.
            if (event.getId().equals(currentEventId)) {
                canSelect = true;
                reasonKey = null;
            }

            canSelectEvent.put(String.valueOf(event.getId()), canSelect);
            if (!canSelect && reasonKey != null) {
                ineligibleReason.put(String.valueOf(event.getId()), reasonKey);
            }
        }

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("playerSubscription", sub);
        model.put("eventCollection", sub.getEvent().getEventCollection());
        model.put("events", sub.getEvent().getEventCollection().getEvents());
        model.put("currentEventId", currentEventId);
        model.put("canSelectEvent", canSelectEvent);
        model.put("ineligibleReason", ineligibleReason);

        return new JteHtml(model, "event/selectEventCollectionVariant.jte");
    }

    @POST
    @LoggedOnly
    @Transactional
    @Path("my/{playersubscriptionId}/eventcollection/{eventCollectionId}")
    public Response updateEventCollectionVariant(
            @PathParam("playersubscriptionId") Integer playersubscriptionId,
            @PathParam("eventCollectionId") Integer eventCollectionId,
            @FormParam("eventId") @NotNull Integer eventId) {

        PlayerSubscription sub = playerSubscriptionService.find(playersubscriptionId);
        if (sub == null) {
            throw new WebApplicationException("PlayerSubscription not found", Response.Status.NOT_FOUND);
        }

        if (!sub.getEvent().getEventCollection().getId().equals(eventCollectionId)) {
            throw new WebApplicationException("EventCollection does not match", Response.Status.BAD_REQUEST);
        }

        Event newEvent = eventService.find(eventId);
        if (newEvent == null || !newEvent.getEventCollection().getId().equals(eventCollectionId)) {
            throw new WebApplicationException("Event not found or not in EventCollection", Response.Status.BAD_REQUEST);
        }

        // Update the subscription to point to the new event
        sub.setEvent(newEvent);
        playerSubscriptionService.merge(sub);

        // Invalidate caches
        caches.getEvent().invalidateAll();
        caches.getAllEvents().invalidateAll();

        URI uri = uriInfo.getBaseUriBuilder()
                .path("event")
                .path("my")
                .queryParam("success", "changed")
                .build();
        return Response.seeOther(uri).build();
    }

    @GET
    @Path("{id:\\d+}")
    public JteHtml event(@PathParam("id") Integer id, @QueryParam("success") String success) throws IOException {

        EventCache cached = caches.getEvent().getIfPresent(id.toString());

        if (cached == null) {
            cached = eventService.buildCache(id);
            caches.getEvent().put(id.toString(), cached);
        }

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("page", "event");
        model.put("event", cached.event);
        model.put("players", cached.players);
        model.put("missingPlayers", cached.missingPlayerCodes);
        model.put("eventInfo", cached.eventInfo);
        model.put("success", success);

        return new JteHtml(model, "event/event.jte");
    }

    @GET
    @Path("new")
    @RequirePermission(PermissionCode.EVENT_CREATE)
    public JteHtml newE(@PathParam("id") Integer id) {
        return edit(null, null);
    }

    @GET
    @Path("{group}")
    public JteHtml events(
            @QueryParam("status") @DefaultValue("ACTIVE") String statusString, @PathParam("group") String group)
            throws IOException {
        EventsCache eventsCache = caches.getAllEvents().getIfPresent(statusString + group);

        if (eventsCache == null) {
            eventsCache = new EventsCache();
            if (group != null) {
                eventsCache.group = eventGroupService.findByShortname(group);
            }
            List<Event> events = group == null
                    ? eventService.findByStatus(EventStatus.valueOf(statusString))
                    : eventService.findByStatus(EventStatus.valueOf(statusString), eventsCache.group);
            
            events.forEach(eventService::fillSubscriptionLimits);
            eventService.fillNbSubscriptions(events);
            eventService.detachAll(events);

            eventsCache.events = events;
            logger.info(events.toString());
            if (eventsCache.group != null) {
                eventGroupService.detach(eventsCache.group);
            }

            caches.getAllEvents().put(statusString + group, eventsCache);
        }

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("page", "events");
        model.put("status", statusString);
        model.put("events", eventsCache.events);
        model.put("eventGroup", eventsCache.group);
        model.put("eventgroups", eventGroupFilter.getAll(group));
        if (eventsCache.group != null) {
            model.put("filterUrl", "/event/" + eventsCache.group.getShortname());
        } else {
            model.put("filterUrl", "/event");
        }
        return new JteHtml(model, "event/events.jte");
    }

    @GET
    @Path("{id:\\d+}/edit")
    @RequirePermission(PermissionCode.EVENT_EDIT)
    public JteHtml edit(@PathParam("id") Integer id, @QueryParam("success") String success) {
        Map<String, Object> model = new HashMap<String, Object>();
        if (id != null && id != 0) {
            Event event = eventService.find(id);
            EventInfo eventInfo = eventInfoService.findByEventId(id);
            if (eventInfo != null) {
                model.put("eventInfo", eventInfo);
            }
            model.put("eventOptions", loadEventOptions(id));
            model.put("event", event);
        } else {
            model.put("event", new Event());
        }
        model.put("eventGroups", eventGroupService.all());
        model.put("eventCollections", eventCollectionService.allOrdered());
        model.put("success", success);
        return new JteHtml(model, "event/eventEdit.jte");
    }

    @POST
    @RequirePermission(PermissionCode.EVENT_EDIT)
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.MULTIPART_FORM_DATA})
    public Response postEventEdit(
            @FormParam("descriptionOnly") @DefaultValue("false") boolean descriptionOnly,
            @FormParam("optionsOnly") @DefaultValue("false") boolean optionsOnly,
            @FormParam("eventId") Integer eventId,
            @FormParam("description") String description,
            @FormParam("ffeId") String ffeId,
            @FormParam("minElo") String minElo,
            @FormParam("maxElo") String maxElo,
            @FormParam("ffePassword") String ffePassword,
            @FormParam("name") String name,
            @FormParam("startDate") String startDate,
            @FormParam("endDate") String endDate,
            @FormParam("status") String status,
            @FormParam("eventType") String eventType,
            @FormParam("price") String price,
            @FormParam("youngprice") String youngprice,
            @FormParam("eventgroup") String eventgroup,
            @FormParam("eventcollection") String eventCollection,
            @FormParam("rondes") String rondes,
            @FormParam("cadence") String cadence,
            @FormParam("pairing") String pairing,
            @FormParam("clubRef") String clubRef,
            @FormParam("maxSubscriptions") String maxSubscriptions,
            @FormParam("pointage") String pointage) {
        if (descriptionOnly) {
            if (eventId != null) {
                eventInfoService.setEventInfo(eventId, description);
            }
        } else if (optionsOnly) {
            if (eventId != null) {
                eventOptionService.setOption(eventId, EventOptionType.FFE_ID, ffeId != null ? ffeId : "");
                eventOptionService.setOption(eventId, EventOptionType.MIN_ELO, minElo != null ? minElo : "");
                eventOptionService.setOption(eventId, EventOptionType.MAX_ELO, maxElo != null ? maxElo : "");
                eventOptionService.setOption(
                        eventId, EventOptionType.FFE_PASSWORD, ffePassword != null ? ffePassword : "");
                eventOptionService.setOption(
                        eventId, EventOptionType.POINTAGE, "1".equals(pointage) ? "1" : "0");
            }
        } else {
            eventId = eventService.saveEvent(
                    eventId,
                    name,
                    startDate,
                    endDate,
                    status,
                    eventType,
                    price,
                    youngprice,
                    eventgroup,
                    eventCollection,
                    rondes,
                    cadence,
                    pairing,
                    clubRef,
                    maxSubscriptions);
        }
        caches.getEvent().invalidateAll();
        caches.getAllEvents().invalidateAll();
        URI uri = uriInfo.getBaseUriBuilder()
                .path("event")
                .path(eventId.toString())
                .path("edit")
                .queryParam("success", "save")
                .build();
        return Response.seeOther(uri).build();
    }

    private Map<String, String> loadEventOptions(int eventId) {
        Map<String, String> options = new HashMap<>();
        for (EventOption option : eventOptionService.findByEventId(eventId)) {
            options.put(option.getOptionType().name(), option.getValue());
        }
        return options;
    }

    @GET
    @Path("{id:\\d+}/pointe")
    @LoggedOnly
    public Response pointe(
            @PathParam("id") Integer eventId, @QueryParam("subId") @NotNull Integer subId) {
        PlayerSubscription sub = playerSubscriptionService.findWithEvent(subId);
        if (sub == null || sub.getEvent() == null || !eventId.equals(sub.getEvent().getId())) {
            throw new WebApplicationException("PlayerSubscription not found", Response.Status.NOT_FOUND);
        }
        if (!loggerUser.getEmail().equals(sub.getCreationUser())) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        if (sub.getStatus() != PlayerSubscriptionStatus.PAID) {
            throw new WebApplicationException("subscription not paid", Response.Status.BAD_REQUEST);
        }
        eventService.fillSubscriptionLimits(sub.getEvent());
        if (!sub.getEvent().isPointageEnabled()) {
            throw new WebApplicationException("pointage not enabled", Response.Status.BAD_REQUEST);
        }
        if (sub.getAttendanceAt() == null) {
            // Dao @Transactional commits before desk snapshot so a fresh EM sees attendance.
            sub.setAttendanceAt(LocalDateTime.now());
            playerSubscriptionService.merge(sub);
            caches.getEvent().invalidateAll();
            eventDeskHub.publishSnapshot(eventId, eventDeskService.snapshot(eventId), null);
        }
        URI uri = uriInfo.getBaseUriBuilder().path("event").path("my").build();
        return Response.seeOther(uri).build();
    }

    @GET
    @Path("{id:\\d+}/unregister")
    @LoggedOnly
    public Response unregister(@PathParam("id") Integer eventId, @QueryParam("nrffe") @NotNull String nrffe)
            throws IOException {

        Event event = eventService.find(eventId);
        if (event == null) {
            throw new WebApplicationException("event not found", Response.Status.NOT_FOUND);
        }

        PlayerSubscription sub = playerSubscriptionService.findByEventAndNrffe(event, nrffe);
        if (sub == null) {
            throw new WebApplicationException(nrffe + " is not present in event " + eventId, Response.Status.NOT_FOUND);
        }
        if (!loggerUser.getEmail().equals(sub.getCreationUser())) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        EventCache cached = caches.getEvent().getIfPresent(eventId.toString());

        if (cached == null) {
            cached = eventService.buildCache(eventId);
            caches.getEvent().put(eventId.toString(), cached);
        }

        if (!registerService.unregisterPlayerToEvent(eventId, nrffe)) {
            throw new WebApplicationException(nrffe + " is not present in event " + eventId, Response.Status.NOT_FOUND);
        }
        URI uri = uriInfo.getBaseUriBuilder().path("event").path("my").build();
        return Response.seeOther(uri).build();
    }

    @GET
    @Path("{id:\\d+}/unregister-pending")
    @LoggedOnly
    public Response unregisterPending(
            @PathParam("id") Integer eventId,
            @QueryParam("nrffe") @NotNull String nrffe) {

        Event event = eventService.find(eventId);
        if (event == null) {
            throw new WebApplicationException("event not found", Response.Status.NOT_FOUND);
        }

        var pending = playerPendingSubscriptionDao
                .findByEventAndNrffeAndCreationUser(event, nrffe, loggerUser.getEmail());

        if (pending == null) {
            throw new WebApplicationException("pending subscription not found", Response.Status.NOT_FOUND);
        }

        playerPendingSubscriptionDao.remove(pending);
        caches.getEvent().invalidateAll();
        caches.getDebtCache().invalidateAll();

        URI uri = uriInfo.getBaseUriBuilder().path("event").path("my").build();
        return Response.seeOther(uri).build();
    }
}

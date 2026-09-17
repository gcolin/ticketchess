package com.github.gcolin.event;

import com.github.gcolin.club.SeasonScope;
import com.github.gcolin.event.Event;
import com.github.gcolin.event.EventCollection;
import com.github.gcolin.event.EventCollectionOptionType;
import com.github.gcolin.event.EventGroup;
import com.github.gcolin.event.EventInfo;
import com.github.gcolin.event.EventOption;
import com.github.gcolin.event.EventOptionType;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.player.DisplayPlayer;
import com.github.gcolin.event.EventStatus;
import com.github.gcolin.event.EventType;
import com.github.gcolin.player.IPlayer;
import com.github.gcolin.player.Find;
import com.github.gcolin.platform.ServiceUtils;
import com.github.gcolin.event.EventCache;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.gcolin.platform.AbstractDao;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class EventDao extends AbstractDao<Event> {

    private Find finder;
    private Supplier<EventInfoDao> eventInfoService;
    private Supplier<EventGroupDao> eventGroupService;
    private Supplier<EventOptionDao> eventOptionDao;
    private Supplier<EventCollectionOptionDao> eventCollectionOptionDao;
    private Supplier<PlayerSubscriptionDao> playerSubscriptionDao;

    private static Logger logger = LoggerFactory.getLogger(EventDao.class);

    public EventDao() {
        super(Event.class);
    }

    public void setFinder(Find finder) {
        this.finder = finder;
    }

    public void setEventInfoDao(Supplier<EventInfoDao> eventInfoService) {
        this.eventInfoService = eventInfoService;
    }

    public void setEventGroupDao(Supplier<EventGroupDao> eventGroupService) {
        this.eventGroupService = eventGroupService;
    }

    public void setEventOptionDao(Supplier<EventOptionDao> eventOptionDao) {
        this.eventOptionDao = eventOptionDao;
    }

    public void setEventCollectionOptionDao(Supplier<EventCollectionOptionDao> eventCollectionOptionDao) {
        this.eventCollectionOptionDao = eventCollectionOptionDao;
    }

    public void setPlayerSubscriptionDao(Supplier<PlayerSubscriptionDao> playerSubscriptionDao) {
        this.playerSubscriptionDao = playerSubscriptionDao;
    }

    public List<Event> findByStatus(EventStatus status) {
        return findByStatus(status, SeasonScope.all());
    }

    /** All events with the given status, including those in an event group. */
    public List<Event> findAllByStatus(EventStatus status) {
        TypedQuery<Event> query = em.createQuery(
                "SELECT e FROM Event e WHERE e.status = :status ORDER BY e.startDate ASC, e.name ASC",
                Event.class);
        query.setParameter("status", status);
        return query.getResultList();
    }

    public List<Event> findByStatus(EventStatus status, SeasonScope scope) {
        String jpql =
                "SELECT e FROM Event e where e.status = :status and e.eventGroup is null";
        if (scope.isFiltered()) {
            jpql += " and e.startDate >= :seasonStart and e.startDate <= :seasonEnd";
        }
        jpql += " order by e.startDate ASC, e.name ASC";
        TypedQuery<Event> query = em.createQuery(jpql, Event.class);
        query.setParameter("status", status);
        bindSeasonScope(query, scope);
        return query.getResultList();
    }

    public List<Event> findByStatus(EventStatus status, EventGroup eventGroup) {
        return findByStatus(status, eventGroup, SeasonScope.all());
    }

    public List<Event> findByStatus(EventStatus status, EventGroup eventGroup, SeasonScope scope) {
        String jpql =
                "SELECT e FROM Event e where e.status = :status and e.eventGroup = :eventGroup";
        if (scope.isFiltered()) {
            jpql += " and e.startDate >= :seasonStart and e.startDate <= :seasonEnd";
        }
        jpql += " order by e.startDate ASC, e.name DESC";
        TypedQuery<Event> query = em.createQuery(jpql, Event.class);
        query.setParameter("status", status);
        query.setParameter("eventGroup", eventGroup);
        bindSeasonScope(query, scope);
        return query.getResultList();
    }

    public String buildCsv(Integer eventId) {
        EventCache cache = buildCache(eventId);
        Event event = find(eventId);
        List<PlayerSubscription> subscriptions = event.getSubscriptions();
        Map<Integer, PlayerSubscription> map = new HashMap<Integer, PlayerSubscription>();
        for (var sub : subscriptions) {
            map.put(sub.getId(), sub);
        }
        StringBuilder str = new StringBuilder();
        str.append("licence;name;rating;status;email;subId;club;title;amountPaid;createdAt;updatedAt\n");
        for (var player : cache.players) {
            str.append(player.getNrffe()).append(";");
            str.append(player.getName());
            if (player.getFirstname() != null && !player.getFirstname().isEmpty()) {
                str.append(" ").append(player.getFirstname());
            }
            str.append(";");
            str.append(player.getRating()).append(";");
            str.append(player.getStatus().name()).append(";");
            PlayerSubscription sub = map.get(player.getSubId());
            str.append(sub.getCreationUser()).append(";");
            str.append(player.getSubId() + "").append(";");
            str.append(player.getClubRef()).append(";");
            str.append(player.getFideTitre() == null ? "" : player.getFideTitre().toUpperCase()).append(";");
            str.append(sub.getAmountCents() == null ? "" : ServiceUtils.toEuros(sub.getAmountCents()) + "").append(";");
            str.append(sub.getCreatedAt() == null ? "" : sub.getCreatedAt()).append(";");
            str.append(sub.getUpdatedAt() == null ? "" : sub.getUpdatedAt()).append("\n");
        }
        return str.toString();
    }

    public EventCache buildCache(Integer eventId) {
        EventCache cached = new EventCache();
        Event event = find(eventId);
        fillSubscriptionLimits(event);
        cached.event = event;
        List<PlayerSubscription> subscriptions = event.getSubscriptions();
        List<DisplayPlayer> players = new ArrayList<>();
        List<String> missingPlayerCodes = new ArrayList<>();
        for (PlayerSubscription sub : subscriptions) {
            if (sub.getStatus() == com.github.gcolin.registration.PlayerSubscriptionStatus.CANCELLED) {
                continue;
            }
            DisplayPlayer player = resolveDisplayPlayer(sub, event, missingPlayerCodes);
            if (player != null) {
                players.add(player);
            }
        }
        players.sort((p1, p2) -> {
            int nameCompare = p1.getName().compareTo(p2.getName());
            if (nameCompare != 0) {
                return nameCompare;
            }
            return p1.getFirstname().compareTo(p2.getFirstname());
        });
        cached.players = players;
        cached.missingPlayerCodes = missingPlayerCodes;

        EventInfo eventInfo = eventInfoService.get().find(event);
        if (eventInfo != null) {

            Parser parser = Parser.builder().build();
            Node document = parser.parse(eventInfo.getDescription());
            HtmlRenderer renderer = HtmlRenderer.builder().escapeHtml(true).build();

            String html = renderer.render(document);
            cached.eventInfo = html;
        }
        detach(cached.event);
        event.setSubscriptions(new ArrayList<>());

        return cached;
    }

    /**
     * Resolves a subscription to a DisplayPlayer. Custom / amical players (@id) and FFE
     * licences missing from Lucene are still exported so Sharly receives every registration.
     */
    private DisplayPlayer resolveDisplayPlayer(
            PlayerSubscription sub, Event event, List<String> missingPlayerCodes) {
        String code = sub.getNrFfe();
        IPlayer p = null;
        try {
            p = finder.player(code, event.getEventType());
        } catch (RuntimeException e) {
            logger.error("cannot resolve player with code {}", code, e);
        }
        if (p == null) {
            logger.error("cannot find player with code {} — exporting placeholder", code);
            if (code != null) {
                missingPlayerCodes.add(code);
            }
            return finalizeDisplayPlayer(placeholderPlayer(sub), sub, event, null);
        }
        try {
            DisplayPlayer player = new DisplayPlayer(p);
            return finalizeDisplayPlayer(player, sub, event, p);
        } catch (RuntimeException e) {
            logger.error("cannot map player with code {} — exporting placeholder", code, e);
            if (code != null) {
                missingPlayerCodes.add(code);
            }
            return finalizeDisplayPlayer(placeholderPlayer(sub), sub, event, null);
        }
    }

    private static DisplayPlayer finalizeDisplayPlayer(
            DisplayPlayer player, PlayerSubscription sub, Event event, IPlayer source) {
        player.setStatus(sub.getStatus());
        player.setAttendanceAt(sub.getAttendanceAt());
        player.setSubId(sub.getId());
        if (source != null) {
            player.setBirthDate(source.getBirthDate());
            player.setClubRef(source.getClubRef());
            player.setRating(source, event.getEventType());
        }
        if (player.getFederation() == null || player.getFederation().isBlank()) {
            player.setFederation("FRA");
        }
        return player;
    }

    private static DisplayPlayer placeholderPlayer(PlayerSubscription sub) {
        DisplayPlayer player = new DisplayPlayer();
        String ref = sub.getNrFfe() == null ? "" : sub.getNrFfe().trim();
        if (ref.startsWith("@") && ref.length() > 1) {
            player.setName("JOUEUR");
            player.setFirstname(ref.substring(1));
        } else if (!ref.isEmpty()) {
            player.setName(ref);
            player.setFirstname("");
        } else {
            player.setName("JOUEUR");
            player.setFirstname(String.valueOf(sub.getId()));
        }
        player.setNrffe(ref);
        player.setNrffeId(ref);
        player.setFederation("FRA");
        player.setCategory("SenM");
        player.setRating("1199E");
        player.setStandardRating("1199E");
        player.setRapidRating("1199E");
        player.setBlitzRating("1199E");
        player.setBirthDate("1990-01-01T00:00:00");
        player.setEditable(true);
        return player;
    }

    public Integer saveEvent(
            Integer id,
            String name,
            String startDate,
            String endDate,
            String status,
            String eventType,
            String price,
            String youngprice,
            String eventgroupId,
            String eventCollectionId,
            String rondesStr,
            String cadence,
            String pairing,
            String eventMaxSubscriptionsStr) {
        Event event = new Event();
        event.setName(name);
        LocalDate startDateLocal = LocalDate.parse(startDate);
        event.setStartDate(startDateLocal.atStartOfDay());
        LocalDate endDateLocal = LocalDate.parse(endDate);
        event.setEndDate(endDateLocal.atStartOfDay());
        event.setStatus(EventStatus.valueOf(status));
        event.setEventType(EventType.valueOf(eventType));
        event.setPriceCents(Math.round(Double.parseDouble(price) * 100d));
        event.setYoungPriceCents(Math.round(Double.parseDouble(youngprice) * 100d));
        if (rondesStr != null && !rondesStr.isEmpty()) {
            // Rondes now saved via EventOptionType.ROUNDS
        }
        if (id != null) {
            event.setId(id);
        }
        if (eventgroupId != null && !eventgroupId.isEmpty()) {
            event.setEventGroup(eventGroupService.get().find(Integer.valueOf(eventgroupId)));
        }
        if (eventCollectionId != null && !eventCollectionId.isEmpty()) {
            event.setEventCollection(em.find(EventCollection.class, Integer.valueOf(eventCollectionId)));
        } else {
            event.setEventCollection(null);
        }
        if (event.getId() == null) {
            persist(event);
        } else {
            merge(event);
        }
        eventOptionDao.get().setOption(event.getId(), EventOptionType.ROUNDS, rondesStr != null ? rondesStr : "");
        eventOptionDao.get().setOption(event.getId(), EventOptionType.CADENCE, cadence != null ? cadence : "");
        eventOptionDao.get().setOption(event.getId(), EventOptionType.PAIRING, pairing != null ? pairing : "");
        eventOptionDao.get().setOption(
                event.getId(),
                EventOptionType.MAX_SUBSCRIPTIONS,
                eventMaxSubscriptionsStr != null ? eventMaxSubscriptionsStr : "");
        return event.getId();
    }

    public void fillSubscriptionLimits(Event event) {
        if (event == null || event.getId() == null) {
            return;
        }
        // Load all event options into the map
        Map<EventOptionType, EventOption> options = new HashMap<>();
        for (EventOption option : eventOptionDao.get().findByEventId(event.getId())) {
            options.put(option.getOptionType(), option);
        }
        event.setEventOptions(options);
        fillEventCollectionLimits(event.getEventCollection());
    }

    public void fillNbSubscriptions(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        List<Integer> eventIds = events.stream()
                .map(Event::getId)
                .filter(id -> id != null)
                .toList();
        if (eventIds.isEmpty()) {
            return;
        }
        Map<Integer, Long> counts = playerSubscriptionDao.get().countByEventIds(eventIds);
        for (Event event : events) {
            if (event.getId() == null) {
                continue;
            }
            event.setNbSubscriptions(counts.getOrDefault(event.getId(), 0L).intValue());
            event.setSubscriptions(new ArrayList<>());
        }
    }

    private void fillEventCollectionLimits(EventCollection eventCollection) {
        if (eventCollection == null || eventCollection.getId() == null) {
            return;
        }
        eventCollection.setMaxSubscribe(eventCollectionOptionDao.get()
                .findIntOptionValue(eventCollection.getId(), EventCollectionOptionType.MAX_SUBSCRIPTIONS));
        eventCollection.setNbSubscriptions((int) playerSubscriptionDao.get().countByEventCollection(eventCollection.getId()));
    }

    public List<Event> findAllForAdmin() {
        return findAllForAdmin(SeasonScope.all());
    }

    public List<Event> findAllForAdmin(SeasonScope scope) {
        String jpql = "SELECT e FROM Event e "
                + "LEFT JOIN FETCH e.eventGroup "
                + "LEFT JOIN FETCH e.eventCollection ";
        if (scope.isFiltered()) {
            jpql += "WHERE e.startDate >= :seasonStart AND e.startDate <= :seasonEnd ";
        }
        jpql += "ORDER BY e.startDate ASC, e.name ASC";
        TypedQuery<Event> query = em.createQuery(jpql, Event.class);
        bindSeasonScope(query, scope);
        return query.getResultList();
    }

    public List<Event> findClosestEvents(int limit) {
        return findClosestEvents(limit, SeasonScope.all());
    }

    public List<Event> findClosestEvents(int limit, SeasonScope scope) {
        String jpql = "SELECT e FROM Event e WHERE e.startDate > CURRENT_TIMESTAMP";
        if (scope.isFiltered()) {
            jpql += " AND e.startDate >= :seasonStart AND e.startDate <= :seasonEnd";
        }
        jpql += " ORDER BY e.startDate ASC";
        TypedQuery<Event> query = em.createQuery(jpql, Event.class);
        bindSeasonScope(query, scope);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    public List<Object[]> findTopEventsByParticipants(int limit) {
        return findTopEventsByParticipants(limit, SeasonScope.all());
    }

    public List<Object[]> findTopEventsByParticipants(int limit, SeasonScope scope) {
        String jpql = "SELECT e, COUNT(ps) as participantCount FROM Event e "
                + "LEFT JOIN e.subscriptions ps ";
        if (scope.isFiltered()) {
            jpql += "WHERE e.startDate >= :seasonStart AND e.startDate <= :seasonEnd ";
        }
        jpql += "GROUP BY e.id ORDER BY participantCount DESC";
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
        bindSeasonScope(query, scope);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    private void bindSeasonScope(TypedQuery<?> query, SeasonScope scope) {
        if (scope.isFiltered()) {
            query.setParameter("seasonStart", scope.getStart());
            query.setParameter("seasonEnd", scope.getEnd());
        }
    }
}

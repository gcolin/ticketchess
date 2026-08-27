package com.github.gcolin.event;

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
        TypedQuery<Event> query = em.createQuery(
                "SELECT e FROM Event e where e.status = :status and e.eventGroup is null order by e.startDate ASC, e.name ASC",
                Event.class);
        query.setParameter("status", status);
        return query.getResultList();
    }

    public List<Event> findByStatus(EventStatus status, EventGroup eventGroup) {
        TypedQuery<Event> query = em.createQuery(
                "SELECT e FROM Event e where e.status = :status and e.eventGroup = :eventGroup order by e.startDate ASC, e.name DESC",
                Event.class);
        query.setParameter("status", status);
        query.setParameter("eventGroup", eventGroup);
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
            IPlayer p = finder.player(sub.getNrFfe(), event.getEventType());
            if (p == null) {
                String code = sub.getNrFfe();
                logger.error("cannot find player with code {}", code);
                missingPlayerCodes.add(code);
            } else {
                DisplayPlayer player = new DisplayPlayer(p);
                player.setStatus(sub.getStatus());
                player.setAttendanceAt(sub.getAttendanceAt());
                player.setSubId(sub.getId());
                player.setRapidRating(p.getRapidRating());
                player.setBlitzRating(p.getBlitzRating());
                player.setBirthDate(p.getBirthDate());
                player.setClubRef(p.getClubRef());
                if (event.getEventType() == EventType.RAPID) {
                    player.setRating(p.getRapidRating());
                } else if (event.getEventType() == EventType.BLITZ) {
                    player.setRating(p.getBlitzRating());
                } else {
                    player.setRating(p.getRating());
                }
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
            String clubRefStr,
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
        eventOptionDao.get().setOption(event.getId(), EventOptionType.CLUB_REF, clubRefStr != null ? clubRefStr : "");
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

    public List<Event> findClosestEvents(int limit) {
        TypedQuery<Event> query = em.createQuery(
                "SELECT e FROM Event e WHERE e.startDate > CURRENT_TIMESTAMP " + "ORDER BY e.startDate ASC",
                Event.class);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    public List<Object[]> findTopEventsByParticipants(int limit) {
        TypedQuery<Object[]> query = em.createQuery(
                "SELECT e, COUNT(ps) as participantCount FROM Event e "
                        + "LEFT JOIN e.subscriptions ps "
                        + "GROUP BY e.id "
                        + "ORDER BY participantCount DESC",
                Object[].class);
        query.setMaxResults(limit);
        return query.getResultList();
    }
}

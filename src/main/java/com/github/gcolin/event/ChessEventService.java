package com.github.gcolin.event;

import com.github.gcolin.player.DisplayPlayer;
import com.github.gcolin.player.LuceneDb;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ChessEventService {

    private final EventDao eventDao;
    private final EventCollectionDao eventCollectionDao;
    private final EventCollectionOptionDao eventCollectionOptionDao;
    private final EventOptionDao eventOptionDao;
    private final PlayerSubscriptionDao playerSubscriptionDao;
    private final ChessEventMapper mapper;

    public ChessEventService(
            EventDao eventDao,
            EventCollectionDao eventCollectionDao,
            EventCollectionOptionDao eventCollectionOptionDao,
            EventOptionDao eventOptionDao,
            PlayerSubscriptionDao playerSubscriptionDao,
            LuceneDb luceneDb) {
        this.eventDao = eventDao;
        this.eventCollectionDao = eventCollectionDao;
        this.eventCollectionOptionDao = eventCollectionOptionDao;
        this.eventOptionDao = eventOptionDao;
        this.playerSubscriptionDao = playerSubscriptionDao;
        this.mapper = new ChessEventMapper(luceneDb);
    }

    public List<String> listTournaments(String userId, String password, String eventId)
            throws ChessEventException {
        ResolvedScope scope = resolveAuthorizedScope(userId, password, eventId);
        return scope.events().stream().map(Event::getName).toList();
    }

    public Map<String, Object> downloadTournament(
            String userId, String password, String eventId, String tournamentName) throws ChessEventException {
        ResolvedScope scope = resolveAuthorizedScope(userId, password, eventId);
        if (tournamentName == null || tournamentName.isBlank()) {
            throw new ChessEventException(498, "Tournament not found");
        }
        String trimmedName = tournamentName.trim();
        Event event = scope.events().stream()
                .filter(e -> trimmedName.equals(e.getName()))
                .findFirst()
                .orElse(null);
        if (event == null) {
            throw new ChessEventException(498, "Tournament not found");
        }
        eventDao.fillSubscriptionLimits(event);
        EventCache cache = eventDao.buildCache(event.getId());
        List<DisplayPlayer> players = filterPlayers(cache.players, cache.event);
        Map<Integer, PlayerSubscription> subscriptions = loadSubscriptions(event);
        return mapper.mapTournament(cache.event, players, subscriptions);
    }

    private ResolvedScope resolveAuthorizedScope(String userId, String password, String eventId)
            throws ChessEventException {
        if (userId == null || userId.isBlank()) {
            throw new ChessEventException(497, "User not found");
        }
        if (password == null) {
            throw new ChessEventException(401, "Unauthorized");
        }

        List<Integer> authenticatedEventIds =
                eventOptionDao.findEventIdsByChessEventCredentials(userId.trim(), password);
        if (authenticatedEventIds.isEmpty()) {
            if (eventOptionDao.chessEventUserExists(userId.trim())) {
                throw new ChessEventException(401, "Unauthorized");
            }
            throw new ChessEventException(497, "User not found");
        }

        ResolvedScope scope = resolveEventId(eventId);
        if (!hasAccess(authenticatedEventIds, scope)) {
            throw new ChessEventException(403, "Access forbidden");
        }
        return scope;
    }

    private boolean hasAccess(List<Integer> authenticatedEventIds, ResolvedScope scope) {
        Set<Integer> allowed = expandAccessibleEventIds(authenticatedEventIds);
        for (Event event : scope.events()) {
            if (allowed.contains(event.getId())) {
                return true;
            }
        }
        return false;
    }

    private Set<Integer> expandAccessibleEventIds(List<Integer> authenticatedEventIds) {
        Set<Integer> allowed = new HashSet<>(authenticatedEventIds);
        for (Integer eventId : authenticatedEventIds) {
            Event event = eventDao.find(eventId);
            if (event == null) {
                continue;
            }
            EventCollection collection = event.getEventCollection();
            if (collection != null) {
                collection.getEvents().size();
                for (Event sibling : collection.getEvents()) {
                    allowed.add(sibling.getId());
                }
            }
        }
        return allowed;
    }

    private ResolvedScope resolveEventId(String eventId) throws ChessEventException {
        if (eventId == null || eventId.isBlank()) {
            throw new ChessEventException(499, "Event not found");
        }
        String trimmed = eventId.trim();

        EventCollection bySlug = eventCollectionOptionDao.findByOptionValue(
                EventCollectionOptionType.CHESS_EVENT_ID, trimmed);
        if (bySlug != null) {
            return loadCollectionScope(bySlug);
        }

        try {
            int numericId = Integer.parseInt(trimmed);
            EventCollection collection = eventCollectionDao.find(numericId);
            if (collection != null) {
                return loadCollectionScope(collection);
            }
            Event event = eventDao.find(numericId);
            if (event != null) {
                return new ResolvedScope(List.of(event));
            }
        } catch (NumberFormatException ignored) {
            // not numeric
        }

        throw new ChessEventException(499, "Event not found");
    }

    private ResolvedScope loadCollectionScope(EventCollection collection) throws ChessEventException {
        collection.getEvents().size();
        List<Event> events = new ArrayList<>(collection.getEvents());
        if (events.isEmpty()) {
            throw new ChessEventException(499, "Event not found");
        }
        events.sort(Comparator.comparing(Event::getStartDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Event::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        return new ResolvedScope(events);
    }

    private List<DisplayPlayer> filterPlayers(List<DisplayPlayer> players, Event event) {
        if (players == null) {
            return List.of();
        }
        return players.stream()
                .filter(player -> player.getStatus() != PlayerSubscriptionStatus.CANCELLED)
                .filter(player -> !event.isPointageEnabled() || player.getAttendanceAt() != null)
                .collect(Collectors.toList());
    }

    private Map<Integer, PlayerSubscription> loadSubscriptions(Event event) {
        Map<Integer, PlayerSubscription> map = new HashMap<>();
        for (PlayerSubscription sub : playerSubscriptionDao.findByEvent(event)) {
            if (sub.getId() != null) {
                map.put(sub.getId(), sub);
            }
        }
        return map;
    }

    private record ResolvedScope(List<Event> events) {}
}

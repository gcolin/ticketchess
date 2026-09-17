package com.github.gcolin.event;

import com.github.gcolin.player.DisplayPlayer;
import com.github.gcolin.player.LuceneDb;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChessEventService {

    private final EventDao eventDao;
    private final EventCollectionDao eventCollectionDao;
    private final EventCollectionOptionDao eventCollectionOptionDao;
    private final PlayerSubscriptionDao playerSubscriptionDao;
    private final ChessEventMapper mapper;

    public ChessEventService(
            EventDao eventDao,
            EventCollectionDao eventCollectionDao,
            EventCollectionOptionDao eventCollectionOptionDao,
            PlayerSubscriptionDao playerSubscriptionDao,
            LuceneDb luceneDb) {
        this.eventDao = eventDao;
        this.eventCollectionDao = eventCollectionDao;
        this.eventCollectionOptionDao = eventCollectionOptionDao;
        this.playerSubscriptionDao = playerSubscriptionDao;
        this.mapper = new ChessEventMapper(luceneDb);
    }

    public List<String> listTournamentsWithSharlyToken(String tokenEventId, String requestedEventId)
            throws ChessEventException {
        ResolvedScope scope = resolveSharlyTokenScope(tokenEventId, requestedEventId);
        return scope.events().stream().map(Event::getName).toList();
    }

    public Map<String, Object> downloadTournamentWithSharlyToken(
            String tokenEventId, String requestedEventId, String tournamentName) throws ChessEventException {
        ResolvedScope scope = resolveSharlyTokenScope(tokenEventId, requestedEventId);
        return downloadFromScope(scope, tournamentName);
    }

    private Map<String, Object> downloadFromScope(ResolvedScope scope, String tournamentName)
            throws ChessEventException {
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
        List<DisplayPlayer> players = filterPlayers(cache.players);
        Map<Integer, PlayerSubscription> subscriptions = subscriptionsById(cache.event);
        return mapper.mapTournament(cache.event, players, subscriptions);
    }

    private ResolvedScope resolveSharlyTokenScope(String tokenEventId, String requestedEventId)
            throws ChessEventException {
        if (tokenEventId == null || tokenEventId.isBlank()) {
            throw new ChessEventException(401, "Unauthorized");
        }
        String effectiveEventId = tokenEventId.trim();
        if (requestedEventId != null && !requestedEventId.isBlank()
                && !effectiveEventId.equals(requestedEventId.trim())) {
            throw new ChessEventException(403, "Access forbidden");
        }
        return resolveEventId(effectiveEventId);
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

    private List<DisplayPlayer> filterPlayers(List<DisplayPlayer> players) {
        if (players == null) {
            return List.of();
        }
        // Always export all non-cancelled registrations. Attendance (pointage) is
        // mapped to Sharly check_in, so organizers keep the full list.
        return players.stream()
                .filter(player -> player.getStatus() != PlayerSubscriptionStatus.CANCELLED)
                .collect(Collectors.toList());
    }

    private Map<Integer, PlayerSubscription> subscriptionsById(Event event) {
        Map<Integer, PlayerSubscription> map = new HashMap<>();
        List<PlayerSubscription> subscriptions = event.getSubscriptions();
        if (subscriptions == null) {
            subscriptions = playerSubscriptionDao.findByEvent(event);
        }
        for (PlayerSubscription sub : subscriptions) {
            if (sub.getId() != null) {
                map.put(sub.getId(), sub);
            }
        }
        return map;
    }

    private record ResolvedScope(List<Event> events) {}
}

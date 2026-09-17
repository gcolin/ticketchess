package com.github.gcolin.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.gcolin.player.DisplayPlayer;
import com.github.gcolin.player.LuceneDb;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChessEventServiceTest {

    private EventDao eventDao;
    private EventCollectionDao eventCollectionDao;
    private EventCollectionOptionDao eventCollectionOptionDao;
    private PlayerSubscriptionDao playerSubscriptionDao;
    private ChessEventService service;

    @BeforeEach
    void setUp() {
        eventDao = mock(EventDao.class);
        eventCollectionDao = mock(EventCollectionDao.class);
        eventCollectionOptionDao = mock(EventCollectionOptionDao.class);
        playerSubscriptionDao = mock(PlayerSubscriptionDao.class);
        service = new ChessEventService(
                eventDao,
                eventCollectionDao,
                eventCollectionOptionDao,
                playerSubscriptionDao,
                mock(LuceneDb.class));
    }

    @Test
    void listTournamentsWithSharlyTokenReturnsNamesForCollection() throws ChessEventException {
        Event eventA = event(1, "Tournoi A");
        Event eventB = event(2, "Tournoi B");
        EventCollection collection = new EventCollection();
        collection.setId(10);
        collection.setEvents(new ArrayList<>(List.of(eventA, eventB)));

        when(eventCollectionOptionDao.findByOptionValue(EventCollectionOptionType.CHESS_EVENT_ID, "fest"))
                .thenReturn(collection);

        List<String> tournaments = service.listTournamentsWithSharlyToken("fest", "fest");

        assertEquals(List.of("Tournoi A", "Tournoi B"), tournaments);
    }

    @Test
    void downloadIncludesAllNonCancelledPlayersAndMapsCheckIn() throws ChessEventException {
        Event event = event(3, "Open");
        event.setEventOptions(Map.of(EventOptionType.POINTAGE, option(EventOptionType.POINTAGE, "1")));

        DisplayPlayer present = player(10, "Present", LocalDateTime.now());
        DisplayPlayer absent = player(11, "Absent", null);
        EventCache cache = new EventCache();
        cache.event = event;
        cache.players = List.of(present, absent);

        when(eventDao.find(3)).thenReturn(event);
        when(eventDao.buildCache(3)).thenReturn(cache);
        when(playerSubscriptionDao.findByEvent(event)).thenReturn(List.of());

        Map<String, Object> tournament =
                service.downloadTournamentWithSharlyToken("3", "3", "Open");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> players = (List<Map<String, Object>>) tournament.get("players");
        assertEquals(2, players.size());
        assertEquals("Present", players.get(0).get("last_name"));
        assertEquals(true, players.get(0).get("check_in"));
        assertEquals("Absent", players.get(1).get("last_name"));
        assertEquals(false, players.get(1).get("check_in"));
        verify(eventDao).fillSubscriptionLimits(event);
    }

    @Test
    void tournamentNotFoundReturns498() {
        Event event = event(5, "Open");
        when(eventDao.find(5)).thenReturn(event);

        ChessEventException ex = assertThrows(
                ChessEventException.class,
                () -> service.downloadTournamentWithSharlyToken("5", "5", "Missing"));
        assertEquals(498, ex.getStatus());
    }

    @Test
    void sharlyTokenRejectsMismatchedEventId() {
        ChessEventException ex = assertThrows(
                ChessEventException.class,
                () -> service.listTournamentsWithSharlyToken("fest", "other"));
        assertEquals(403, ex.getStatus());
    }

    private static Event event(int id, String name) {
        Event event = new Event();
        event.setId(id);
        event.setName(name);
        event.setStartDate(LocalDateTime.of(2026, 1, 1, 9, 0));
        event.setEndDate(LocalDateTime.of(2026, 1, 1, 18, 0));
        event.setEventType(EventType.STANDARD);
        return event;
    }

    private static EventOption option(EventOptionType type, String value) {
        EventOption option = new EventOption();
        option.setOptionType(type);
        option.setValue(value);
        return option;
    }

    private static DisplayPlayer player(int subId, String name, LocalDateTime attendanceAt) {
        DisplayPlayer player = new DisplayPlayer();
        player.setSubId(subId);
        player.setName(name);
        player.setFirstname("Test");
        player.setCategory("SenM");
        player.setStatus(PlayerSubscriptionStatus.NOT_PAID);
        player.setAttendanceAt(attendanceAt);
        player.setRating("1500E");
        return player;
    }
}

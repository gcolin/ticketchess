package com.github.gcolin.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.gcolin.player.DisplayPlayer;
import com.github.gcolin.player.LuceneDb;
import com.github.gcolin.registration.PlayerSubscription;
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
    private EventOptionDao eventOptionDao;
    private PlayerSubscriptionDao playerSubscriptionDao;
    private ChessEventService service;

    @BeforeEach
    void setUp() {
        eventDao = mock(EventDao.class);
        eventCollectionDao = mock(EventCollectionDao.class);
        eventCollectionOptionDao = mock(EventCollectionOptionDao.class);
        eventOptionDao = mock(EventOptionDao.class);
        playerSubscriptionDao = mock(PlayerSubscriptionDao.class);
        service = new ChessEventService(
                eventDao,
                eventCollectionDao,
                eventCollectionOptionDao,
                eventOptionDao,
                playerSubscriptionDao,
                mock(LuceneDb.class));
    }

    @Test
    void listTournamentsReturnsNamesForCollection() throws ChessEventException {
        Event eventA = event(1, "Tournoi A");
        Event eventB = event(2, "Tournoi B");
        EventCollection collection = new EventCollection();
        collection.setId(10);
        collection.setEvents(new ArrayList<>(List.of(eventA, eventB)));

        when(eventOptionDao.findEventIdsByChessEventCredentials("C69548", "secret"))
                .thenReturn(List.of(1));
        when(eventCollectionOptionDao.findByOptionValue(EventCollectionOptionType.CHESS_EVENT_ID, "fest"))
                .thenReturn(collection);
        when(eventDao.find(1)).thenReturn(eventA);

        List<String> tournaments = service.listTournaments("C69548", "secret", "fest");

        assertEquals(List.of("Tournoi A", "Tournoi B"), tournaments);
    }

    @Test
    void downloadFiltersCheckedInPlayersWhenPointageEnabled() throws ChessEventException {
        Event event = event(3, "Open");
        event.setEventOptions(Map.of(EventOptionType.POINTAGE, option(EventOptionType.POINTAGE, "1")));

        DisplayPlayer present = player(10, "Present", LocalDateTime.now());
        DisplayPlayer absent = player(11, "Absent", null);
        EventCache cache = new EventCache();
        cache.event = event;
        cache.players = List.of(present, absent);

        when(eventOptionDao.findEventIdsByChessEventCredentials("arb", "pwd")).thenReturn(List.of(3));
        when(eventDao.find(3)).thenReturn(event);
        when(eventDao.buildCache(3)).thenReturn(cache);
        when(playerSubscriptionDao.findByEvent(event)).thenReturn(List.of());

        Map<String, Object> tournament = service.downloadTournament("arb", "pwd", "3", "Open");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> players = (List<Map<String, Object>>) tournament.get("players");
        assertEquals(1, players.size());
        assertEquals("Present", players.get(0).get("last_name"));
        verify(eventDao).fillSubscriptionLimits(event);
    }

    @Test
    void unauthorizedWhenPasswordWrong() {
        when(eventOptionDao.findEventIdsByChessEventCredentials("arb", "bad")).thenReturn(List.of());
        when(eventOptionDao.chessEventUserExists("arb")).thenReturn(true);

        ChessEventException ex =
                assertThrows(ChessEventException.class, () -> service.listTournaments("arb", "bad", "1"));
        assertEquals(401, ex.getStatus());
    }

    @Test
    void userNotFoundWhenUnknownUser() {
        when(eventOptionDao.findEventIdsByChessEventCredentials("unknown", "pwd")).thenReturn(List.of());
        when(eventOptionDao.chessEventUserExists("unknown")).thenReturn(false);

        ChessEventException ex =
                assertThrows(ChessEventException.class, () -> service.listTournaments("unknown", "pwd", "1"));
        assertEquals(497, ex.getStatus());
    }

    @Test
    void tournamentNotFoundReturns498() {
        Event event = event(5, "Open");
        when(eventOptionDao.findEventIdsByChessEventCredentials("arb", "pwd")).thenReturn(List.of(5));
        when(eventDao.find(5)).thenReturn(event);

        ChessEventException ex = assertThrows(
                ChessEventException.class, () -> service.downloadTournament("arb", "pwd", "5", "Missing"));
        assertEquals(498, ex.getStatus());
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

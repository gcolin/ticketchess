package com.github.gcolin.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.gcolin.player.DisplayPlayer;
import com.github.gcolin.player.LuceneDb;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChessEventMapperTest {

    private ChessEventMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ChessEventMapper(mock(LuceneDb.class));
    }

    @Test
    void mapPairingMapsKnownValues() {
        assertEquals(1, ChessEventMapper.mapPairing("Standard"));
        assertEquals(2, ChessEventMapper.mapPairing("Haley"));
        assertEquals(3, ChessEventMapper.mapPairing("HaleySoft"));
        assertEquals(6, ChessEventMapper.mapPairing("Berger"));
    }

    @Test
    void mapCategoryMapsAgeCategories() {
        assertEquals(8, ChessEventMapper.mapCategory("SenM"));
        assertEquals(3, ChessEventMapper.mapCategory("PupF"));
        assertEquals(0, ChessEventMapper.mapCategory(null));
    }

    @Test
    void mapPlayerIncludesPaymentAndCheckIn() {
        Event event = new Event();
        event.setPriceCents(2500L);
        event.setYoungPriceCents(1500L);

        DisplayPlayer player = new DisplayPlayer();
        player.setName("DUPONT");
        player.setFirstname("Jean");
        player.setCategory("SenM");
        player.setNrffe("X12345");
        player.setRefffe("1368865");
        player.setRating("1650F");
        player.setStatus(PlayerSubscriptionStatus.PAID);
        player.setAttendanceAt(LocalDateTime.of(2026, 6, 1, 9, 0));
        player.setSubId(42);

        PlayerSubscription sub = new PlayerSubscription();
        sub.setId(42);
        sub.setCreationUser("player@example.com");
        sub.setAmountCents(2500L);

        Map<String, Object> row = mapper.mapPlayer(event, player, sub);

        assertEquals("DUPONT", row.get("last_name"));
        assertEquals("Jean", row.get("first_name"));
        assertEquals(2, row.get("gender"));
        assertEquals(1368865, row.get("ffe_id"));
        assertEquals("X12345", row.get("ffe_license_number"));
        assertEquals(1650, row.get("standard_rating"));
        assertEquals(3, row.get("standard_rating_type"));
        assertEquals("player@example.com", row.get("email"));
        assertTrue((Boolean) row.get("check_in"));
        assertEquals(25.0, row.get("initial_fee"));
        assertEquals(25.0, row.get("paid_before"));
    }

    @Test
    void mapTournamentBuildsMetadata() {
        Event event = new Event();
        event.setName("Open Rapide");
        event.setStartDate(LocalDateTime.of(2026, 6, 1, 9, 30));
        event.setEndDate(LocalDateTime.of(2026, 6, 1, 18, 0));
        event.setEventType(EventType.RAPID);
        event.setEventOptions(Map.of(
                EventOptionType.ROUNDS, option(EventOptionType.ROUNDS, "5"),
                EventOptionType.CADENCE, option(EventOptionType.CADENCE, "G15+2"),
                EventOptionType.PAIRING, option(EventOptionType.PAIRING, "SAD")));

        Map<String, Object> tournament = mapper.mapTournament(event, List.of(), Map.of());

        assertEquals("Open Rapide", tournament.get("name"));
        assertEquals(5, tournament.get("rounds"));
        assertEquals(4, tournament.get("pairing"));
        assertEquals("G15+2", tournament.get("time_control"));
        assertEquals(2, tournament.get("rating"));
        assertEquals(
                LocalDateTime.of(2026, 6, 1, 9, 30).atZone(ZoneId.systemDefault()).toEpochSecond(),
                tournament.get("start"));
        assertTrue(((List<?>) tournament.get("players")).isEmpty());
    }

    private static EventOption option(EventOptionType type, String value) {
        EventOption option = new EventOption();
        option.setOptionType(type);
        option.setValue(value);
        return option;
    }
}

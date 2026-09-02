package com.github.gcolin.event;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.gcolin.event.Event;
import com.github.gcolin.event.EventOption;
import com.github.gcolin.event.EventOptionType;
import com.github.gcolin.player.LuceneDb;
import com.github.gcolin.player.Club;
import com.github.gcolin.player.DisplayPlayer;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PapiServiceTest {

    @Test
    void testExtractRating() throws Exception {
        PapiService service = new PapiService();
        Method method = PapiService.class.getDeclaredMethod("extractRating", String.class);
        method.setAccessible(true);

        Assertions.assertEquals(0, method.invoke(service, (String) null));
        Assertions.assertEquals(0, method.invoke(service, ""));
        Assertions.assertEquals(1234, method.invoke(service, "1234"));
        Assertions.assertEquals(1234, method.invoke(service, "1234N"));
        Assertions.assertEquals(0, method.invoke(service, "abcd"));
    }

    @Test
    void testExtractFideFlag() throws Exception {
        PapiService service = new PapiService();
        Method method = PapiService.class.getDeclaredMethod("extractFideFlag", String.class);
        method.setAccessible(true);

        Assertions.assertEquals("N", method.invoke(service, (String) null));
        Assertions.assertEquals("N", method.invoke(service, ""));
        Assertions.assertEquals("N", method.invoke(service, "1800"));
        Assertions.assertEquals("F", method.invoke(service, "1800F"));
    }

    @Test
    void testFormatBirthDate() throws Exception {
        PapiService service = new PapiService();
        Method method = PapiService.class.getDeclaredMethod("formatBirthDate", String.class);
        method.setAccessible(true);

        Assertions.assertNull(method.invoke(service, (String) null));
        Assertions.assertNull(method.invoke(service, ""));

        LocalDateTime iso = (LocalDateTime) method.invoke(service, "2020-01-02T10:15:30");
        Assertions.assertEquals(LocalDateTime.of(2020, 1, 2, 10, 15, 30), iso);

        LocalDateTime yearOnly = (LocalDateTime) method.invoke(service, "2012");
        Assertions.assertEquals(LocalDateTime.of(2012, 1, 1, 0, 0), yearOnly);

        Assertions.assertNull(method.invoke(service, "not-a-date"));
    }

    @Test
    void testParseClubRef() throws Exception {
        PapiService service = new PapiService();
        Method method = PapiService.class.getDeclaredMethod("parseClubRef", String.class);
        method.setAccessible(true);

        Assertions.assertEquals(0, method.invoke(service, (String) null));
        Assertions.assertEquals(0, method.invoke(service, ""));
        Assertions.assertEquals(12345, method.invoke(service, "12345"));
    }

    @Test
    void testExtractSexeAndCat() throws Exception {
        PapiService service = new PapiService();
        Method extractSexe = PapiService.class.getDeclaredMethod("extractSexe", String.class);
        Method extractCat = PapiService.class.getDeclaredMethod("extractCat", String.class);
        extractSexe.setAccessible(true);
        extractCat.setAccessible(true);

        Assertions.assertEquals("M", extractSexe.invoke(service, (String) null));
        Assertions.assertEquals("F", extractSexe.invoke(service, "U10F"));
        Assertions.assertEquals("M", extractSexe.invoke(service, "U10M"));

        Assertions.assertEquals("U10", extractCat.invoke(service, "U10F"));
        Assertions.assertEquals("Sen", extractCat.invoke(service, "SenM"));
        Assertions.assertEquals("Sen", extractCat.invoke(service, "Sen"));
        Assertions.assertNull(extractCat.invoke(service, (String) null));
    }

    @Test
    void testGeneratePapiFileCreatesAFile() throws Exception {
        PapiService service = new PapiService();
        Event event = new Event();
        event.setName("Open Test");
        event.setStartDate(LocalDateTime.of(2026, 5, 23, 0, 0));
        event.setPriceCents(1000L);
        event.setYoungPriceCents(500L);

        File file = service.generatePapiFile(event, List.of());

        Assertions.assertNotNull(file);
        Assertions.assertTrue(file.exists());
        Assertions.assertTrue(file.length() > 0);
        file.delete();
    }

    @Test
    void testUpdateInfoTableSetsNomAndDates() throws Exception {
        PapiService service = new PapiService();
        Method method = PapiService.class.getDeclaredMethod("updateInfoTable", Database.class, Event.class);
        method.setAccessible(true);

        Database db = mock(Database.class);
        Table infoTable = mock(Table.class);
        Row rowNom = mock(Row.class);
        Row rowDebut = mock(Row.class);
        Row rowFin = mock(Row.class);
        Row rowRounds = mock(Row.class);
        Row rowCadence = mock(Row.class);
        Row rowPairing = mock(Row.class);
        Row rowClassElo = mock(Row.class);
        Row rowOther = mock(Row.class);

        when(db.getTable("INFO")).thenReturn(infoTable);
        when(infoTable.iterator())
                .thenReturn(List.of(
                                rowNom,
                                rowDebut,
                                rowFin,
                                rowRounds,
                                rowCadence,
                                rowPairing,
                                rowClassElo,
                                rowOther)
                        .iterator());
        when(rowNom.get("Variable")).thenReturn("Nom");
        when(rowDebut.get("Variable")).thenReturn("DateDebut");
        when(rowFin.get("Variable")).thenReturn("DateFin");
        when(rowRounds.get("Variable")).thenReturn("NbrRondes");
        when(rowCadence.get("Variable")).thenReturn("Cadence");
        when(rowPairing.get("Variable")).thenReturn("Pairing");
        when(rowClassElo.get("Variable")).thenReturn("ClassElo");
        when(rowOther.get("Variable")).thenReturn("Other");

        Event event = new Event();
        event.setName("Tournoi A");
        event.setStartDate(LocalDateTime.of(2026, 6, 1, 0, 0));
        event.setEventType(com.github.gcolin.event.EventType.RAPID);
        EventOption rounds = new EventOption();
        rounds.setOptionType(EventOptionType.ROUNDS);
        rounds.setValue("7");
        EventOption cadence = new EventOption();
        cadence.setOptionType(EventOptionType.CADENCE);
        cadence.setValue("15min+10sec");
        EventOption pairing = new EventOption();
        pairing.setOptionType(EventOptionType.PAIRING);
        pairing.setValue("Haley");
        event.setEventOptions(Map.of(
                EventOptionType.ROUNDS, rounds,
                EventOptionType.CADENCE, cadence,
                EventOptionType.PAIRING, pairing));

        method.invoke(service, db, event);

        verify(rowNom).put("Value", "Tournoi A");
        verify(rowDebut).put("Value", "01/06/2026");
        verify(rowFin).put("Value", "01/06/2026");
        verify(rowRounds).put("Value", "7");
        verify(rowCadence).put("Value", "15min+10sec");
        verify(rowPairing).put("Value", "Haley");
        verify(rowClassElo).put("Value", "Rapide");
    }

    @Test
    void testTournamentRatingUsesRapidForRapidEvents() throws Exception {
        PapiService service = new PapiService();
        Method method = PapiService.class.getDeclaredMethod(
                "tournamentRating", DisplayPlayer.class, com.github.gcolin.event.EventType.class);
        method.setAccessible(true);

        DisplayPlayer player = new DisplayPlayer();
        player.setStandardRating("2100F");
        player.setRapidRating("1950F");
        player.setBlitzRating("1800F");

        Assertions.assertEquals(
                "1950F",
                method.invoke(service, player, com.github.gcolin.event.EventType.RAPID));
        Assertions.assertEquals(
                "1800F",
                method.invoke(service, player, com.github.gcolin.event.EventType.BLITZ));
        Assertions.assertEquals(
                "2100F",
                method.invoke(service, player, com.github.gcolin.event.EventType.STANDARD));
    }

    @Test
    void testToClassElo() throws Exception {
        PapiService service = new PapiService();
        Method method = PapiService.class.getDeclaredMethod(
                "toClassElo", com.github.gcolin.event.EventType.class);
        method.setAccessible(true);

        Assertions.assertEquals(
                "Elo", method.invoke(service, com.github.gcolin.event.EventType.STANDARD));
        Assertions.assertEquals(
                "Rapide", method.invoke(service, com.github.gcolin.event.EventType.RAPID));
        Assertions.assertEquals(
                "Blitz", method.invoke(service, com.github.gcolin.event.EventType.BLITZ));
        Assertions.assertNull(method.invoke(service, (Object) null));
    }

    @Test
    void testUpdateInfoTableTruncatesLongNom() throws Exception {
        PapiService service = new PapiService();
        Method method = PapiService.class.getDeclaredMethod("updateInfoTable", Database.class, Event.class);
        method.setAccessible(true);

        Database db = mock(Database.class);
        Table infoTable = mock(Table.class);
        Row rowNom = mock(Row.class);

        when(db.getTable("INFO")).thenReturn(infoTable);
        when(infoTable.iterator()).thenReturn(List.of(rowNom).iterator());
        when(rowNom.get("Variable")).thenReturn("Nom");

        String longName = "Open International de Perros Guirec - Open A - 2400 Elo+";
        Assertions.assertTrue(longName.length() > 50);

        Event event = new Event();
        event.setName(longName);
        event.setStartDate(LocalDateTime.of(2026, 6, 1, 0, 0));

        method.invoke(service, db, event);

        verify(rowNom).put("Value", longName.substring(0, 50));
    }

    @Test
    void testUpdateJoueursTableCoversPaidAndUnpaidBranches() throws Exception {
        PapiService service = new PapiService();
        Method method =
                PapiService.class.getDeclaredMethod("updateJoueursTable", Database.class, List.class, Event.class);
        method.setAccessible(true);

        LuceneDb luceneDb = mock(LuceneDb.class);
        Field luceneField = PapiService.class.getDeclaredField("luceneDb");
        luceneField.setAccessible(true);
        luceneField.set(service, luceneDb);

        Club club = new Club();
        club.setNom("Club Rennes");
        club.setLigue("BRE");
        when(luceneDb.searchClub(anyString())).thenReturn(club);

        DisplayPlayer paid = new DisplayPlayer();
        paid.setFide("123456");
        paid.setFideCode("123456");
        paid.setRefffe("999001");
        paid.setNrffe("ABCDEF");
        paid.setName("DOE, John");
        paid.setFirstname(null);
        paid.setCategory("SenM");
        paid.setBirthDate("2012");
        paid.setStandardRating("1800F");
        paid.setRating("1700N");
        paid.setRapidRating("1700N");
        paid.setBlitzRating("1600");
        paid.setFederation("");
        paid.setFideTitre("MI");
        paid.setClubRef("123");
        paid.setStatus(PlayerSubscriptionStatus.PAID);
        paid.setAffType(null);
        paid.setAttendanceAt(java.time.LocalDateTime.of(2026, 7, 26, 10, 0));

        DisplayPlayer unpaid = new DisplayPlayer();
        unpaid.setFide("0");
        unpaid.setNrffe("123456");
        unpaid.setName("Smith");
        unpaid.setFirstname("Alice");
        unpaid.setCategory("U10F");
        unpaid.setBirthDate("2020-01-02T10:15:30");
        unpaid.setStandardRating("1500");
        unpaid.setRating("1400");
        unpaid.setRapidRating("1400");
        unpaid.setBlitzRating("1300");
        unpaid.setFederation("USA");
        unpaid.setClubRef("");
        unpaid.setStatus(PlayerSubscriptionStatus.NOT_PAID);
        unpaid.setAffType("A");
        unpaid.setAttendanceAt(null);

        Event event = new Event();
        event.setPriceCents(1000L);
        event.setYoungPriceCents(500L);
        event.setEventType(com.github.gcolin.event.EventType.RAPID);

        Database db = mock(Database.class);
        Table joueurs = mock(Table.class);
        when(db.getTable("JOUEUR")).thenReturn(joueurs);

        List<Map<String, Object>> rows = new ArrayList<>();
        when(joueurs.addRowFromMap(org.mockito.ArgumentMatchers.anyMap())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> row = new HashMap<>((Map<String, Object>) invocation.getArgument(0));
            rows.add(row);
            return null;
        });

        method.invoke(service, db, List.of(paid, unpaid), event);

        Assertions.assertEquals(2, rows.size());

        Map<String, Object> paidRow = rows.get(0);
        Assertions.assertEquals(999001, paidRow.get("RefFFE"));
        Assertions.assertEquals("ABCDEF", paidRow.get("NrFFE"));
        Assertions.assertEquals("DOE", paidRow.get("Nom"));
        Assertions.assertEquals("John", paidRow.get("Prenom"));
        Assertions.assertEquals(1700, paidRow.get("Elo"));
        Assertions.assertEquals(1700, paidRow.get("Rapide"));
        Assertions.assertEquals("FRA", paidRow.get("Federation"));
        Assertions.assertEquals("123456", paidRow.get("FideCode"));
        Assertions.assertEquals("MI", paidRow.get("FideTitre"));
        Assertions.assertEquals("Club Rennes", paidRow.get("Club"));
        Assertions.assertEquals("BRE", paidRow.get("Ligue"));
        Assertions.assertEquals(10d, paidRow.get("InscriptionRegle"));
        Assertions.assertEquals(0, paidRow.get("InscriptionDu"));
        Assertions.assertEquals("N", paidRow.get("AffType"));
        Assertions.assertEquals(true, paidRow.get("Pointe"));
        Assertions.assertFalse(paidRow.containsKey("Actif"));
        Assertions.assertEquals("R", paidRow.get("Rd01Cl"));
        Assertions.assertEquals(0, paidRow.get("Rd01Res"));

        Map<String, Object> unpaidRow = rows.get(1);
        Assertions.assertEquals("Smith", unpaidRow.get("Nom"));
        Assertions.assertEquals("Alice", unpaidRow.get("Prenom"));
        Assertions.assertEquals(1400, unpaidRow.get("Elo"));
        Assertions.assertEquals(1400, unpaidRow.get("Rapide"));
        Assertions.assertEquals("USA", unpaidRow.get("Federation"));
        Assertions.assertEquals(0, unpaidRow.get("InscriptionRegle"));
        Assertions.assertEquals(10d, unpaidRow.get("InscriptionDu"));
        Assertions.assertEquals("A", unpaidRow.get("AffType"));
        Assertions.assertEquals(false, unpaidRow.get("Pointe"));
        Assertions.assertNull(unpaidRow.get("RefFFE"));

        verify(luceneDb, times(1)).searchClub("123");
    }
}

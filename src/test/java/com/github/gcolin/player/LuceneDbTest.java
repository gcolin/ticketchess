package com.github.gcolin.player;

import com.github.gcolin.player.ManualPlayerEntry;
import com.github.gcolin.player.Player;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LuceneDbTest {

    @TempDir
    Path tempDir;

    private String previousTestProperty;
    private String previousLoadtestProperty;
    private LuceneDb db;

    @AfterEach
    void tearDown() throws IOException {
        if (db != null) {
            db.close();
        }
        restoreProperty("test", previousTestProperty);
        restoreProperty("loadtest", previousLoadtestProperty);
    }

    @Test
    void loadUsesInMemoryTestIndexAndFindsDefaultPlayers() throws Exception {
        db = newDb();

        db.load(true);

        Player byNrffe = db.searchJoueur("X82897");
        Assertions.assertNotNull(byNrffe);
        Assertions.assertEquals("COLIN", byNrffe.getName());
        Assertions.assertEquals("Gael", byNrffe.getFirstname());
        Assertions.assertEquals("1400F", byNrffe.getRating());
        Assertions.assertEquals("424242", byNrffe.getRefffe());

        Player byFide = db.searchJoueur("343428916");
        Assertions.assertNotNull(byFide);
        Assertions.assertEquals("Breton, Martin", byFide.getName());
        Assertions.assertEquals("", byFide.getFirstname());
        Assertions.assertEquals("ENG", byFide.getFederation());
    }

    @Test
    void searchJoueurUsesAccentInsensitiveFullNameSearch() throws Exception {
        db = newDb();
        ManualPlayerEntry manualPlayer = manualPlayer("M123", null, "Durand", "Elodie");
        manualPlayer.setFederation("FRA");
        manualPlayer.setClub("Club test");
        db.addManualPlayer(manualPlayer);

        db.load(true);

        List<Player> players = db.searchJoueur("elodie durand", 10, null);

        Assertions.assertEquals(1, players.size());
        Assertions.assertEquals("M123", players.get(0).getNrffe());
        Assertions.assertEquals("Durand", players.get(0).getName());
        Assertions.assertEquals("Elodie", players.get(0).getFirstname());
        Assertions.assertEquals("FRA", players.get(0).getFederation());
        Assertions.assertEquals("Club test", players.get(0).getClub());
    }

    @Test
    void addManualPlayerNormalizesDefaultsAndReplacesSameKey() throws Exception {
        db = newDb();
        ManualPlayerEntry first = manualPlayer(" M123 ", null, " Durand ", " Alice ");
        ManualPlayerEntry replacement = manualPlayer("M123", null, " Martin ", " Bob ");

        db.addManualPlayer(first);
        db.addManualPlayer(replacement);

        List<ManualPlayerEntry> players = db.getManualPlayers();
        Assertions.assertEquals(1, players.size());
        Assertions.assertEquals("nrffe:M123", players.get(0).getKey());
        Assertions.assertEquals("Martin", players.get(0).getName());
        Assertions.assertEquals("Bob", players.get(0).getFirstname());
    }

    @Test
    void addManualPlayerRequiresNameAndAtLeastOneIdentifier() throws IOException {
        db = newDb();

        ManualPlayerEntry withoutName = manualPlayer("M123", null, " ", "Alice");
        IllegalArgumentException nameError =
                Assertions.assertThrows(IllegalArgumentException.class, () -> db.addManualPlayer(withoutName));
        Assertions.assertEquals("name is required", nameError.getMessage());

        ManualPlayerEntry withoutIdentifier = manualPlayer(" ", " ", "Durand", "Alice");
        IllegalArgumentException idError =
                Assertions.assertThrows(IllegalArgumentException.class, () -> db.addManualPlayer(withoutIdentifier));
        Assertions.assertEquals("nrffe or fide is required", idError.getMessage());
    }

    @Test
    void removeManualPlayerReturnsWhetherEntryWasRemoved() throws IOException {
        db = newDb();
        db.addManualPlayer(manualPlayer(null, "987654", "Durand", "Alice"));

        Assertions.assertTrue(db.removeManualPlayer("fide:987654"));
        Assertions.assertFalse(db.removeManualPlayer("fide:987654"));
        Assertions.assertFalse(db.removeManualPlayer(" "));
        Assertions.assertTrue(db.getManualPlayers().isEmpty());
    }

    private LuceneDb newDb() {
        previousTestProperty = System.getProperty("test");
        previousLoadtestProperty = System.getProperty("loadtest");
        System.setProperty("test", "true");
        System.setProperty("loadtest", "true");

        LuceneDb luceneDb = new LuceneDb();
        luceneDb.mdbFile = tempDir.resolve("missing-Data.mdb").toString();
        luceneDb.fideFile = tempDir.resolve("missing-players_list_xml.zip").toString();
        luceneDb.manualPlayersFile = tempDir.resolve("lucene").resolve("manual-players.json").toString();
        return luceneDb;
    }

    private ManualPlayerEntry manualPlayer(String nrffe, String fide, String name, String firstname) {
        ManualPlayerEntry player = new ManualPlayerEntry();
        player.setNrffe(nrffe);
        player.setFide(fide);
        player.setName(name);
        player.setFirstname(firstname);
        return player;
    }

    private void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}

package com.github.gcolin.player;

import com.github.gcolin.platform.Config;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

class LuceneDbLazyInitTest {

    @TempDir
    Path tempDir;

    private String previousTestProperty;
    private String previousLoadtestProperty;

    @AfterEach
    void tearDown() {
        if (previousTestProperty == null) {
            System.clearProperty("test");
        } else {
            System.setProperty("test", previousTestProperty);
        }
        if (previousLoadtestProperty == null) {
            System.clearProperty("loadtest");
        } else {
            System.setProperty("loadtest", previousLoadtestProperty);
        }
    }

    @Test
    void initDoesNotLoadIndexUntilFirstSearch() throws Exception {
        previousTestProperty = System.getProperty("test");
        previousLoadtestProperty = System.getProperty("loadtest");
        System.setProperty("test", "true");
        System.setProperty("loadtest", "true");

        LuceneDb db = new LuceneDb();
        Config config = Mockito.mock(Config.class);
        Mockito.when(config.getConfigDir()).thenReturn(tempDir.toString());
        db.setConfig(config);
        db.init();

        Player player = db.searchJoueurByNrffe("X82897");
        Assertions.assertNotNull(player);
        Assertions.assertEquals("COLIN", player.getName());
    }
}

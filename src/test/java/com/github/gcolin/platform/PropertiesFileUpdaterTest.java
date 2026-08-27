package com.github.gcolin.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PropertiesFileUpdaterTest {

    @TempDir
    Path tempDir;

    @Test
    void updateShouldReplaceExistingKeyAndKeepComments() throws Exception {
        Path file = tempDir.resolve("params.properties");
        Files.writeString(
                file,
                "# comment\n" + "title=Old\n" + "#stripe.secret=sk_old\n",
                StandardCharsets.UTF_8);

        Map<String, String> updates = new LinkedHashMap<>();
        updates.put("title", "New");
        updates.put("org.name", "Club");
        PropertiesFileUpdater.update(file, updates, Set.of());

        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(content.contains("# comment"));
        assertTrue(content.contains("title=New"));
        assertTrue(content.contains("org.name=Club"));
        assertTrue(content.contains("#stripe.secret=sk_old") || content.contains("stripe.secret=sk_old"));
    }

    @Test
    void updateShouldSkipBlankSecrets() throws Exception {
        Path file = tempDir.resolve("params.properties");
        Files.writeString(file, "stripe.secret=sk_live\n", StandardCharsets.UTF_8);

        Map<String, String> updates = new LinkedHashMap<>();
        updates.put("stripe.secret", "");
        PropertiesFileUpdater.update(file, updates, Set.of("stripe.secret"));

        assertEquals("sk_live", Files.readString(file, StandardCharsets.UTF_8).trim().substring("stripe.secret=".length()));
    }

    @Test
    void updateShouldUncommentExistingKey() throws Exception {
        Path file = tempDir.resolve("params.properties");
        Files.writeString(file, "#oauth.clientId=old\n", StandardCharsets.UTF_8);

        Map<String, String> updates = Map.of("oauth.clientId", "new-id");
        PropertiesFileUpdater.update(file, updates, Set.of());

        assertEquals("oauth.clientId=new-id", Files.readString(file, StandardCharsets.UTF_8).trim());
    }
}

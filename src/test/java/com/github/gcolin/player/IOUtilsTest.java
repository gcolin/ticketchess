package com.github.gcolin.player;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Disabled
class IOUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void unzipExtractsNestedFiles() throws IOException {
        Path zip = tempDir.resolve("archive.zip");
        zip(zip, new Entry("club/info.txt", "hello"));
        Path destination = tempDir.resolve("out");

        IOUtils.unzip(zip.toString(), destination.toString());

        Assertions.assertEquals(
                "hello", Files.readString(destination.resolve("club").resolve("info.txt"), StandardCharsets.UTF_8));
    }

    @Test
    void unzipRejectsZipSlipEntries() throws IOException {
        Path zip = tempDir.resolve("archive.zip");
        zip(zip, new Entry("../escape.txt", "bad"));
        Path destination = tempDir.resolve("out");

        IOException error = Assertions.assertThrows(IOException.class, () -> IOUtils.unzip(zip.toString(), destination.toString()));

        Assertions.assertTrue(error.getMessage().contains("Entry en dehors du dossier cible"));
        Assertions.assertFalse(Files.exists(tempDir.resolve("escape.txt")));
    }

    @Test
    void deleteDirectoryDeletesRecursivelyAndIgnoresMissingPaths() throws IOException {
        Path directory = tempDir.resolve("to-delete");
        Files.createDirectories(directory.resolve("nested"));
        Files.writeString(directory.resolve("nested").resolve("file.txt"), "data", StandardCharsets.UTF_8);

        IOUtils.deleteDirectory(directory);
        IOUtils.deleteDirectory(directory);

        Assertions.assertFalse(Files.exists(directory));
    }

    private void zip(Path zip, Entry... entries) throws IOException {
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (Entry entry : entries) {
                output.putNextEntry(new ZipEntry(entry.name()));
                output.write(entry.content().getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
    }

    private record Entry(String name, String content) {}
}

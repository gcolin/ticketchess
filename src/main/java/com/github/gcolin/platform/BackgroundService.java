package com.github.gcolin.platform;

import jakarta.ws.rs.WebApplicationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public class BackgroundService {

    static final int MAX_SIZE_BYTES = 5 * 1024 * 1024;
    private static final String[] NAMES = {"background.png", "background.jpg", "background.jpeg", "background.webp"};

    private Config config;

    public void setConfig(Config config) {
        this.config = config;
    }

    public Path getBackgroundFile() {
        Path dir = Path.of(config.getConfigDir());
        for (String name : NAMES) {
            Path candidate = dir.resolve(name);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return dir.resolve("background.jpg");
    }

    public boolean exists() {
        return Files.isRegularFile(getBackgroundFile());
    }

    public String getContentType() {
        Path file = getBackgroundFile();
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (name.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/png";
    }

    public void save(InputStream input) throws IOException {
        if (input == null) {
            throw new WebApplicationException("Background file is required", 400);
        }
        byte[] header = input.readNBytes(12);
        String extension = extensionForHeader(header);
        if (extension == null) {
            throw new WebApplicationException("Background file must be a PNG, JPEG or WebP image", 400);
        }

        Path dir = Path.of(config.getConfigDir());
        Files.createDirectories(dir);
        String fileName = "background." + extension;
        Path target = dir.resolve(fileName);
        Path tmp = target.resolveSibling(fileName + ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(tmp)) {
                out.write(header);
                copyLimited(input, out, MAX_SIZE_BYTES - header.length);
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }
        for (String name : NAMES) {
            if (!name.equals(fileName)) {
                Files.deleteIfExists(dir.resolve(name));
            }
        }
    }

    public void delete() throws IOException {
        Path dir = Path.of(config.getConfigDir());
        for (String name : NAMES) {
            Files.deleteIfExists(dir.resolve(name));
        }
    }

    private static String extensionForHeader(byte[] header) {
        if (header == null || header.length < 12) {
            return null;
        }
        if (header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
            return "png";
        }
        if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
            return "jpg";
        }
        if (header[0] == 'R'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == 'F'
                && header[8] == 'W'
                && header[9] == 'E'
                && header[10] == 'B'
                && header[11] == 'P') {
            return "webp";
        }
        return null;
    }

    private static void copyLimited(InputStream input, OutputStream out, int maxRemaining) throws IOException {
        byte[] buffer = new byte[8192];
        long copied = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            copied += read;
            if (copied > maxRemaining) {
                throw new WebApplicationException("Background file is too large", 400);
            }
            out.write(buffer, 0, read);
        }
    }
}

package com.github.gcolin.payment;

import com.github.gcolin.platform.Config;
import jakarta.ws.rs.WebApplicationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class RibService {

    static final String FILE_NAME = "rib.pdf";
    static final int MAX_SIZE_BYTES = 5 * 1024 * 1024;
    private static final int PDF_HEADER_SCAN_BYTES = 8192;

    private Config config;

    public void setConfig(Config config) {
        this.config = config;
    }

    public Path getRibFile() {
        return Path.of(config.getConfigDir(), FILE_NAME);
    }

    public boolean exists() {
        return Files.isRegularFile(getRibFile());
    }

    public void save(InputStream input) throws IOException {
        if (input == null) {
            throw new WebApplicationException("RIB file is required", 400);
        }
        byte[] prefix = input.readNBytes(PDF_HEADER_SCAN_BYTES);
        int pdfStart = indexOfPdfMagic(prefix);
        if (pdfStart < 0) {
            throw new WebApplicationException("RIB file must be a PDF", 400);
        }

        Path target = getRibFile();
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tmp = target.resolveSibling(FILE_NAME + ".tmp");
        int initialLength = prefix.length - pdfStart;
        try {
            try (OutputStream out = Files.newOutputStream(tmp)) {
                out.write(prefix, pdfStart, initialLength);
                copyLimited(input, out, MAX_SIZE_BYTES - initialLength);
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    public void delete() throws IOException {
        Files.deleteIfExists(getRibFile());
    }

    private static int indexOfPdfMagic(byte[] data) {
        if (data == null || data.length < 4) {
            return -1;
        }
        for (int i = 0; i <= data.length - 4; i++) {
            if (data[i] == '%' && data[i + 1] == 'P' && data[i + 2] == 'D' && data[i + 3] == 'F') {
                return i;
            }
        }
        return -1;
    }

    private static void copyLimited(InputStream input, OutputStream out, int maxRemaining) throws IOException {
        byte[] buffer = new byte[8192];
        long copied = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            copied += read;
            if (copied > maxRemaining) {
                throw new WebApplicationException("RIB file is too large", 400);
            }
            out.write(buffer, 0, read);
        }
    }
}

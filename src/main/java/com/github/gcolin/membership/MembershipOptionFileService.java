package com.github.gcolin.membership;

import com.github.gcolin.platform.Config;
import jakarta.ws.rs.WebApplicationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MembershipOptionFileService {

    static final int MAX_SIZE_BYTES = 100 * 1024 * 1024;
    private static final String ROOT_DIR = "membership-files";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "webp", "gif", "doc", "docx", "xls", "xlsx", "odt", "ods", "txt", "csv");

    private Config config;

    public void setConfig(Config config) {
        this.config = config;
    }

    public Path resolveStoredFile(MembershipOptionFile file) {
        if (file == null || file.getMembershipOption() == null || file.getMembershipOption().getId() == null) {
            throw new WebApplicationException("File metadata is incomplete", 500);
        }
        if (file.getStoredName() == null || file.getStoredName().isBlank()) {
            throw new WebApplicationException("Stored file name is missing", 500);
        }
        return optionDir(file.getMembershipOption().getId()).resolve(file.getStoredName());
    }

    public StoredFile save(Integer optionId, String originalName, String contentType, InputStream input)
            throws IOException {
        if (optionId == null) {
            throw new WebApplicationException("Option is required", 400);
        }
        if (input == null) {
            throw new WebApplicationException("File is required", 400);
        }
        String safeOriginal = sanitizeOriginalName(originalName);
        String extension = extensionOf(safeOriginal);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new WebApplicationException("File type is not allowed", 400);
        }

        String storedName = UUID.randomUUID().toString().replace("-", "") + "_" + safeOriginal;
        Path dir = optionDir(optionId);
        Files.createDirectories(dir);
        Path target = dir.resolve(storedName);
        Path tmp = target.resolveSibling(storedName + ".tmp");
        long size;
        try {
            try (OutputStream out = Files.newOutputStream(tmp)) {
                size = copyLimited(input, out, MAX_SIZE_BYTES);
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }

        String resolvedContentType = contentType;
        if (resolvedContentType == null || resolvedContentType.isBlank() || "*/*".equals(resolvedContentType)) {
            resolvedContentType = guessContentType(extension);
        }
        return new StoredFile(safeOriginal, storedName, resolvedContentType, size);
    }

    public void delete(MembershipOptionFile file) throws IOException {
        if (file == null) {
            return;
        }
        Path path = resolveStoredFile(file);
        Files.deleteIfExists(path);
    }

    private Path optionDir(Integer optionId) {
        return Path.of(config.getConfigDir(), ROOT_DIR, String.valueOf(optionId));
    }

    private static String sanitizeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            throw new WebApplicationException("File name is required", 400);
        }
        String name = originalName.trim().replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[^a-zA-Z0-9._\\- ]", "_").trim();
        if (name.isBlank() || name.startsWith(".")) {
            throw new WebApplicationException("File name is invalid", 400);
        }
        if (name.length() > 180) {
            String extension = extensionOf(name);
            String base = name.substring(0, 180);
            name = extension == null ? base : base + "." + extension;
        }
        return name;
    }

    private static String extensionOf(String fileName) {
        if (fileName == null) {
            return null;
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String guessContentType(String extension) {
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            case "txt" -> "text/plain";
            case "csv" -> "text/csv";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "odt" -> "application/vnd.oasis.opendocument.text";
            case "ods" -> "application/vnd.oasis.opendocument.spreadsheet";
            default -> "application/octet-stream";
        };
    }

    private static long copyLimited(InputStream input, OutputStream out, int maxBytes) throws IOException {
        byte[] buffer = new byte[8192];
        long copied = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            copied += read;
            if (copied > maxBytes) {
                throw new WebApplicationException("File is too large", 400);
            }
            out.write(buffer, 0, read);
        }
        if (copied == 0) {
            throw new WebApplicationException("File is empty", 400);
        }
        return copied;
    }

    public record StoredFile(String originalName, String storedName, String contentType, long sizeBytes) {}
}

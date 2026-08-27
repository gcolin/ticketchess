package com.github.gcolin.platform;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import jakarta.ws.rs.core.StreamingOutput;

/** Detects a client that closed the connection while a response was still being written. */
public final class ClientDisconnect {

    private ClientDisconnect() {}

    public static boolean isGone(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof EOFException) {
                return true;
            }
            String name = current.getClass().getSimpleName();
            if ("EofException".equalsIgnoreCase(name) || name.contains("ClientAbort")) {
                return true;
            }
            if (current instanceof IOException message && message.getMessage() != null) {
                String text = message.getMessage().toLowerCase();
                if (text.contains("broken pipe")
                        || text.contains("connection reset")
                        || text.contains("abandonn")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    public static StreamingOutput copyFile(Path file) {
        return output -> {
            try {
                Files.copy(file, output);
            } catch (IOException e) {
                if (!isGone(e)) {
                    throw e;
                }
            }
        };
    }
}

package com.github.gcolin.event;

import com.github.gcolin.platform.Config;
import jakarta.inject.Inject;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Validates absolute Sharly Chess callback URLs. Localhost is always allowed;
 * additional origins can be listed in {@code sharly.callback.origins}.
 */
public class SharlyCallbackAllowlist {

    private final Config config;

    @Inject
    public SharlyCallbackAllowlist(Config config) {
        this.config = config;
    }

    public boolean isAllowed(String callback) {
        if (callback == null || callback.isBlank()) {
            return false;
        }
        URI uri;
        try {
            uri = URI.create(callback.trim());
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (uri.getScheme() == null || uri.getHost() == null) {
            return false;
        }
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            return false;
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if ("localhost".equals(host) || "127.0.0.1".equals(host)) {
            return true;
        }
        String origin = scheme + "://" + host + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
        for (String allowed : configuredOrigins()) {
            if (origin.equalsIgnoreCase(allowed) || callback.trim().regionMatches(true, 0, allowed, 0, allowed.length())) {
                return true;
            }
        }
        return false;
    }

    private List<String> configuredOrigins() {
        List<String> origins = new ArrayList<>();
        String raw = config.getProperty("sharly.callback.origins", "");
        if (raw == null || raw.isBlank()) {
            return origins;
        }
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                origins.add(trimmed.replaceAll("/$", ""));
            }
        }
        return origins;
    }
}

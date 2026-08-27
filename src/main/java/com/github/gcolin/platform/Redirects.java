package com.github.gcolin.platform;

import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;

public final class Redirects {

    private Redirects() {}

    /** Accepts app-relative paths only (e.g. {@code /event/my}). Rejects absolute and protocol-relative URLs. */
    public static boolean isSafeRelativeRedirect(String redirectUri) {
        if (redirectUri == null || redirectUri.isBlank()) {
            return false;
        }
        String trimmed = redirectUri.trim();
        if (!trimmed.startsWith("/") || trimmed.startsWith("//")) {
            return false;
        }
        return !trimmed.contains("://") && !trimmed.contains("\\") && !trimmed.contains("@");
    }

    public static URI safeRedirect(String redirectUri, URI defaultUri, UriInfo uriInfo) {
        if (!isSafeRelativeRedirect(redirectUri)) {
            return defaultUri;
        }
        String trimmed = redirectUri.trim();
        UriBuilder builder = uriInfo.getBaseUriBuilder();
        int queryIndex = trimmed.indexOf('?');
        String pathPart = queryIndex >= 0 ? trimmed.substring(0, queryIndex) : trimmed;
        builder.path(pathPart.startsWith("/") ? pathPart.substring(1) : pathPart);
        if (queryIndex >= 0) {
            builder.replaceQuery(trimmed.substring(queryIndex + 1));
        }
        return builder.build();
    }
}

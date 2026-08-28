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

    /** Strips scheme/host so redirects resolve against the browser's current origin (HTTPS behind a proxy). */
    public static URI toSameOriginRelative(URI uri) {
        String path = uri.getRawPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        StringBuilder relative = new StringBuilder(path);
        String query = uri.getRawQuery();
        if (query != null && !query.isEmpty()) {
            relative.append('?').append(query);
        }
        String fragment = uri.getRawFragment();
        if (fragment != null && !fragment.isEmpty()) {
            relative.append('#').append(fragment);
        }
        return URI.create(relative.toString());
    }
}

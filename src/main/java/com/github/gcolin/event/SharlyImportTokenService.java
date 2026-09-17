package com.github.gcolin.event;

import com.github.gcolin.platform.Config;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.inject.Inject;
import java.util.Date;

/**
 * Mints and validates short-lived JWTs used by Sharly Chess to import events
 * after the user picks an open competition on Ticket Chess.
 */
public class SharlyImportTokenService {

    public static final String SCOPE = "sharly";
    public static final long TTL_MS = 15L * 60L * 1000L;

    private final Config config;

    @Inject
    public SharlyImportTokenService(Config config) {
        this.config = config;
    }

    public String mint(
            String email,
            String eventId,
            String name,
            Integer collectionId,
            Integer eventNumericId) {
        var builder = Jwts.builder()
                .subject(email)
                .claim("scope", SCOPE)
                .claim("event_id", eventId)
                .claim("name", name == null ? "" : name)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + TTL_MS))
                .signWith(config.getKeys(), Config.JWT_ALGORITHM);
        if (collectionId != null) {
            builder.claim("collectionId", collectionId);
        }
        if (eventNumericId != null) {
            builder.claim("eventId", eventNumericId);
        }
        return builder.compact();
    }

    public Claims parseSharlyToken(String jwt) throws ChessEventException {
        if (jwt == null || jwt.isBlank()) {
            throw new ChessEventException(401, "Unauthorized");
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(config.getKeys())
                    .build()
                    .parseSignedClaims(jwt.trim())
                    .getPayload();
            Object scope = claims.get("scope");
            if (!SCOPE.equals(scope)) {
                throw new ChessEventException(401, "Unauthorized");
            }
            String eventId = claims.get("event_id", String.class);
            if (eventId == null || eventId.isBlank()) {
                throw new ChessEventException(401, "Unauthorized");
            }
            return claims;
        } catch (JwtException | IllegalArgumentException e) {
            throw new ChessEventException(401, "Unauthorized");
        }
    }

    public static String extractBearer(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        String value = authorizationHeader.trim();
        if (value.length() > 7 && value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return value.substring(7).trim();
        }
        return null;
    }
}

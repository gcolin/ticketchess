package com.github.gcolin.desk;

import com.github.gcolin.platform.AppContext;
import com.github.gcolin.platform.Config;
import com.github.gcolin.desk.EventDeskService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventDeskEndpointConfigurator extends ServerEndpointConfig.Configurator {

    private static final Logger logger = LoggerFactory.getLogger(EventDeskEndpointConfigurator.class);

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        String email = null;
        boolean authorized = false;
        Integer eventId = extractEventId(request);
        try {
            Config config = AppContext.get().config();
            Claims ticketClaims = parseTicket(request, config);
            if (ticketClaims != null && "desk".equals(ticketClaims.get("scope", String.class))) {
                if (eventId != null && ticketAllowsEvent(ticketClaims, eventId)) {
                    email = ticketClaims.getSubject();
                    sec.getUserProperties().put("allowedEventIds", extractAllowedEventIds(ticketClaims, eventId));
                }
            }

            if (email == null) {
                Object httpSessionObj = request.getHttpSession();
                if (httpSessionObj instanceof HttpSession httpSession) {
                    Object sessionEmail = httpSession.getAttribute("auth.email");
                    if (sessionEmail instanceof String s && !s.isBlank()) {
                        email = s;
                    }
                }
            }

            if (email == null) {
                Claims claims = parseCookieClaims(request, config);
                if (claims != null) {
                    email = claims.getSubject();
                }
            }

            EventDeskService deskService = AppContext.get().eventDeskService();
            authorized = email != null && deskService.hasDeskAccess(email);
        } catch (RuntimeException e) {
            logger.warn("desk websocket handshake auth failed: {}", e.toString());
            authorized = false;
        }
        sec.getUserProperties().put("email", email);
        sec.getUserProperties().put("authorized", authorized);
        if (!authorized) {
            logger.warn(
                    "desk websocket unauthorized eventId={} email={} path={}",
                    eventId,
                    email,
                    request.getRequestURI());
        }
    }

    private boolean ticketAllowsEvent(Claims ticketClaims, Integer eventId) {
        Object rawIds = ticketClaims.get("eventIds");
        if (rawIds instanceof List<?> ids) {
            for (Object id : ids) {
                if (id instanceof Number n && eventId.equals(n.intValue())) {
                    return true;
                }
                if (id != null && eventId.toString().equals(id.toString())) {
                    return true;
                }
            }
        }
        Integer ticketEventId = ticketClaims.get("eventId", Integer.class);
        if (ticketEventId == null) {
            Number n = ticketClaims.get("eventId", Number.class);
            ticketEventId = n == null ? null : n.intValue();
        }
        return eventId.equals(ticketEventId);
    }

    private List<Integer> extractAllowedEventIds(Claims ticketClaims, Integer fallbackEventId) {
        List<Integer> allowed = new java.util.ArrayList<>();
        Object rawIds = ticketClaims.get("eventIds");
        if (rawIds instanceof List<?> ids) {
            for (Object id : ids) {
                if (id instanceof Number n) {
                    allowed.add(n.intValue());
                } else if (id != null) {
                    try {
                        allowed.add(Integer.valueOf(id.toString()));
                    } catch (NumberFormatException ignored) {
                        // skip
                    }
                }
            }
        }
        if (allowed.isEmpty() && fallbackEventId != null) {
            allowed.add(fallbackEventId);
        }
        return allowed;
    }

    private Integer extractEventId(HandshakeRequest request) {
        URI uri = request.getRequestURI();
        if (uri == null || uri.getPath() == null) {
            return null;
        }
        String[] parts = uri.getPath().split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("event".equals(parts[i]) && i + 1 < parts.length) {
                try {
                    return Integer.valueOf(parts[i + 1]);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private Claims parseTicket(HandshakeRequest request, Config config) {
        String ticket = firstQueryParam(request, "ticket");
        if (ticket == null || ticket.isBlank()) {
            return null;
        }
        try {
            return Jwts.parser()
                    .verifyWith(config.getKeys())
                    .build()
                    .parseSignedClaims(ticket)
                    .getPayload();
        } catch (JwtException e) {
            logger.warn("invalid desk ticket: {}", e.toString());
            return null;
        }
    }

    private String firstQueryParam(HandshakeRequest request, String name) {
        Map<String, List<String>> params = request.getParameterMap();
        if (params != null && params.get(name) != null && !params.get(name).isEmpty()) {
            return params.get(name).get(0);
        }
        URI uri = request.getRequestURI();
        if (uri == null || uri.getRawQuery() == null) {
            return null;
        }
        for (String part : uri.getRawQuery().split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && name.equals(kv[0])) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private Claims parseCookieClaims(HandshakeRequest request, Config config) {
        Map<String, List<String>> headers = request.getHeaders();
        if (headers == null) {
            return null;
        }
        List<String> cookies = headers.get("Cookie");
        if (cookies == null || cookies.isEmpty()) {
            cookies = headers.get("cookie");
        }
        if (cookies == null) {
            return null;
        }
        for (String header : cookies) {
            for (String part : header.split(";")) {
                String trimmed = part.trim();
                if (trimmed.startsWith("remember_me=")) {
                    String token = trimmed.substring("remember_me=".length());
                    if (token.startsWith("\"") && token.endsWith("\"") && token.length() >= 2) {
                        token = token.substring(1, token.length() - 1);
                    }
                    try {
                        token = URLDecoder.decode(token, StandardCharsets.UTF_8);
                    } catch (IllegalArgumentException ignored) {
                        // keep raw token
                    }
                    try {
                        return Jwts.parser()
                                .verifyWith(config.getKeys())
                                .build()
                                .parseSignedClaims(token)
                                .getPayload();
                    } catch (JwtException e) {
                        logger.debug("jwt cookie rejected on desk websocket: {}", e.toString());
                        return null;
                    }
                }
            }
        }
        return null;
    }
}

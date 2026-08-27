package com.github.gcolin.desk;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.websocket.Session;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventDeskHub {

    private static final Logger logger = LoggerFactory.getLogger(EventDeskHub.class);
    private static final Jsonb JSONB = JsonbBuilder.create();

    private final Map<Integer, Set<Session>> sessionsByEvent = new ConcurrentHashMap<>();

    public void register(Integer eventId, Session session) {
        sessionsByEvent
                .computeIfAbsent(eventId, id -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    public void unregister(Integer eventId, Session session) {
        Set<Session> sessions = sessionsByEvent.get(eventId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByEvent.remove(eventId, sessions);
        }
    }

    public void publishSnapshot(Integer eventId, List<EventDeskPlayerDto> players, Session except) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "snapshot");
        payload.put("eventId", eventId);
        payload.put("players", players);
        payload.put("acked", List.of());
        broadcast(eventId, JSONB.toJson(payload), except);
    }

    public void broadcast(Integer eventId, String message, Session except) {
        Set<Session> sessions = collectSessions(eventId);
        if (sessions.isEmpty()) {
            return;
        }
        for (Session session : sessions) {
            if (except != null && except.getId().equals(session.getId())) {
                continue;
            }
            if (!session.isOpen()) {
                continue;
            }
            try {
                synchronized (session) {
                    session.getBasicRemote().sendText(message);
                }
            } catch (IOException e) {
                logger.warn("cannot broadcast desk update to session {}: {}", session.getId(), e.toString());
            }
        }
    }

    private Set<Session> collectSessions(Integer eventId) {
        Set<Session> sessions = new HashSet<>();
        Set<Session> direct = sessionsByEvent.get(eventId);
        if (direct != null) {
            sessions.addAll(direct);
        }
        // Desks opened on a sibling event (collection / "all" view) also need the update.
        for (Map.Entry<Integer, Set<Session>> entry : sessionsByEvent.entrySet()) {
            if (eventId.equals(entry.getKey())) {
                continue;
            }
            for (Session session : entry.getValue()) {
                if (allowsEvent(session, eventId)) {
                    sessions.add(session);
                }
            }
        }
        return sessions;
    }

    private static boolean allowsEvent(Session session, Integer eventId) {
        Object allowed = session.getUserProperties().get("allowedEventIds");
        if (allowed instanceof List<?> ids) {
            for (Object id : ids) {
                if (id instanceof Number n && eventId.equals(n.intValue())) {
                    return true;
                }
            }
            return false;
        }
        return allowed instanceof Set<?> ids && ids.contains(eventId);
    }
}

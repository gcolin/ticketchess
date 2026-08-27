package com.github.gcolin.desk;

import com.github.gcolin.desk.EventDeskOp;
import com.github.gcolin.desk.EventDeskPlayerDto;
import com.github.gcolin.platform.AppContext;
import com.github.gcolin.platform.RequestContext;
import com.github.gcolin.desk.EventDeskHub;
import com.github.gcolin.desk.EventDeskService;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServerEndpoint(value = "/ws/event/{eventId}/desk", configurator = EventDeskEndpointConfigurator.class)
public class EventDeskWebSocket {

    private static final Logger logger = LoggerFactory.getLogger(EventDeskWebSocket.class);
    private static final Jsonb JSONB = JsonbBuilder.create();
    static final long IDLE_TIMEOUT_MS = 120_000L;

    @OnOpen
    public void onOpen(Session session, @PathParam("eventId") Integer eventId) throws IOException {
        if (!Boolean.TRUE.equals(session.getUserProperties().get("authorized"))) {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "forbidden"));
            return;
        }
        session.setMaxIdleTimeout(IDLE_TIMEOUT_MS);
        session.getUserProperties().put("eventId", eventId);
        if (session.getUserProperties().get("allowedEventIds") == null) {
            withRequestContext(() -> {
                session.getUserProperties().put("allowedEventIds", deskService().collectionEventIds(eventId));
                return null;
            });
        }
        hub().register(eventId, session);
        withRequestContext(() -> {
            try {
                sendSnapshot(session, eventId, List.of());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("eventId") Integer eventId) throws IOException {
        withRequestContext(() -> {
            try {
                handleMessage(message, session, eventId);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    private void handleMessage(String message, Session session, Integer pathEventId) throws IOException {
        EventDeskOp.SyncRequest request;
        try {
            request = JSONB.fromJson(message, EventDeskOp.SyncRequest.class);
        } catch (Exception e) {
            sendError(session, "invalid message");
            return;
        }
        if (request == null || request.getType() == null) {
            sendError(session, "missing type");
            return;
        }
        switch (request.getType()) {
            case "ping" -> sendJson(session, Map.of("type", "pong"));
            case "reload" -> {
                Integer target = resolveEventId(session, pathEventId, request.getEventId());
                if (target == null) {
                    sendError(session, "forbidden event");
                    return;
                }
                sendSnapshot(session, target, List.of());
            }
            case "reloadAll" -> reloadAll(session, pathEventId, request.getEventIds());
            case "sync" -> {
                Integer target = resolveEventId(session, pathEventId, request.getEventId());
                if (target == null) {
                    sendError(session, "forbidden event");
                    return;
                }
                List<String> acked = deskService().applyOps(target, request.getOps());
                sendSnapshot(session, target, acked);
                broadcastSnapshot(target, session);
            }
            case "syncAll" -> syncAll(session, pathEventId, request.getBatches());
            default -> sendError(session, "unknown type: " + request.getType());
        }
    }

    private void reloadAll(Session session, Integer pathEventId, List<Integer> eventIds) throws IOException {
        List<Integer> targets = resolveEventIds(session, pathEventId, eventIds);
        List<Map<String, Object>> snapshots = new ArrayList<>();
        for (Integer target : targets) {
            Map<String, Object> snap = new HashMap<>();
            snap.put("eventId", target);
            snap.put("players", deskService().snapshot(target));
            snap.put("acked", List.of());
            snapshots.add(snap);
        }
        sendJson(session, Map.of("type", "snapshots", "snapshots", snapshots));
    }

    private void syncAll(Session session, Integer pathEventId, List<EventDeskOp.SyncBatch> batches)
            throws IOException {
        if (batches == null || batches.isEmpty()) {
            reloadAll(session, pathEventId, List.of(pathEventId));
            return;
        }
        List<Map<String, Object>> snapshots = new ArrayList<>();
        Set<Integer> touched = new HashSet<>();
        for (EventDeskOp.SyncBatch batch : batches) {
            if (batch == null || batch.getEventId() == null) {
                continue;
            }
            Integer target = resolveEventId(session, pathEventId, batch.getEventId());
            if (target == null) {
                continue;
            }
            List<String> acked = deskService().applyOps(target, batch.getOps());
            Map<String, Object> snap = new HashMap<>();
            snap.put("eventId", target);
            snap.put("players", deskService().snapshot(target));
            snap.put("acked", acked);
            snapshots.add(snap);
            touched.add(target);
            broadcastSnapshot(target, session);
        }
        sendJson(session, Map.of("type", "snapshots", "snapshots", snapshots));
        // Ensure current path event is included even with empty pending.
        if (!touched.contains(pathEventId)) {
            sendSnapshot(session, pathEventId, List.of());
        }
    }

    @SuppressWarnings("unchecked")
    private Integer resolveEventId(Session session, Integer pathEventId, Integer requestedEventId) {
        Integer target = requestedEventId == null ? pathEventId : requestedEventId;
        if (target == null) {
            return null;
        }
        if (target.equals(pathEventId)) {
            return target;
        }
        Object allowed = session.getUserProperties().get("allowedEventIds");
        if (allowed instanceof List<?> ids) {
            for (Object id : ids) {
                if (id instanceof Number n && target.equals(n.intValue())) {
                    return target;
                }
            }
        }
        if (allowed instanceof Set<?> ids && ids.contains(target)) {
            return target;
        }
        return null;
    }

    private List<Integer> resolveEventIds(Session session, Integer pathEventId, List<Integer> eventIds) {
        List<Integer> requested = eventIds == null || eventIds.isEmpty() ? List.of(pathEventId) : eventIds;
        List<Integer> resolved = new ArrayList<>();
        for (Integer id : requested) {
            Integer target = resolveEventId(session, pathEventId, id);
            if (target != null && !resolved.contains(target)) {
                resolved.add(target);
            }
        }
        if (resolved.isEmpty() && pathEventId != null) {
            resolved.add(pathEventId);
        }
        return resolved;
    }

    @OnClose
    public void onClose(Session session, @PathParam("eventId") Integer eventId, CloseReason reason) {
        hub().unregister(eventId, session);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        logger.warn("desk websocket error on {}: {}", session == null ? null : session.getId(), error.toString());
    }

    private void broadcastSnapshot(Integer eventId, Session except) {
        hub().publishSnapshot(eventId, deskService().snapshot(eventId), except);
    }

    private void sendSnapshot(Session session, Integer eventId, List<String> acked) throws IOException {
        List<EventDeskPlayerDto> players = deskService().snapshot(eventId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "snapshot");
        payload.put("eventId", eventId);
        payload.put("players", players);
        payload.put("acked", acked == null ? List.of() : acked);
        sendJson(session, payload);
    }

    private void sendError(Session session, String message) throws IOException {
        sendJson(session, Map.of("type", "error", "message", message));
    }

    private void sendJson(Session session, Object payload) throws IOException {
        synchronized (session) {
            session.getBasicRemote().sendText(JSONB.toJson(payload));
        }
    }

    private <T> T withRequestContext(Supplier<T> action) {
        return RequestContext.run(action);
    }

    private EventDeskService deskService() {
        return AppContext.get().eventDeskService();
    }

    private EventDeskHub hub() {
        return AppContext.get().eventDeskHub();
    }
}

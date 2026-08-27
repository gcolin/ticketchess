package com.github.gcolin.auth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ActiveLoggedUsers {

    private static final long IDLE_MS = TimeUnit.MINUTES.toMillis(35);

    private final ConcurrentHashMap<String, ActiveSession> sessions = new ConcurrentHashMap<>();

    public void touch(String sessionId, String email, String username, boolean admin) {
        if (sessionId == null || email == null) {
            return;
        }
        long now = System.currentTimeMillis();
        sessions.compute(sessionId, (id, existing) -> {
            if (existing == null) {
                return new ActiveSession(id, email, username, admin, now);
            }
            existing.setEmail(email);
            existing.setUsername(username);
            existing.setAdmin(admin);
            existing.setLastSeenMillis(now);
            return existing;
        });
    }

    public void remove(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }

    public List<ActiveSession> listActive() {
        purgeIdle();
        List<ActiveSession> result = new ArrayList<>(sessions.values());
        result.sort(Comparator.comparing(ActiveSession::getEmail)
                .thenComparing(ActiveSession::getLastSeenMillis)
                .reversed());
        return result;
    }

    private void purgeIdle() {
        long cutoff = System.currentTimeMillis() - IDLE_MS;
        sessions.entrySet().removeIf(e -> e.getValue().getLastSeenMillis() < cutoff);
    }
}

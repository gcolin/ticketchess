package com.github.gcolin.auth;

public class ActiveSession {

    private final String sessionId;
    private String email;
    private String username;
    private boolean admin;
    private long lastSeenMillis;

    public ActiveSession(String sessionId, String email, String username, boolean admin, long lastSeenMillis) {
        this.sessionId = sessionId;
        this.email = email;
        this.username = username;
        this.admin = admin;
        this.lastSeenMillis = lastSeenMillis;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public long getLastSeenMillis() {
        return lastSeenMillis;
    }

    public void setLastSeenMillis(long lastSeenMillis) {
        this.lastSeenMillis = lastSeenMillis;
    }
}

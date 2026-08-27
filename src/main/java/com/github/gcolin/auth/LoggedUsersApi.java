package com.github.gcolin.auth;

import com.github.gcolin.auth.RequirePermission;
import com.github.gcolin.auth.PermissionCode;
import com.github.gcolin.auth.ActiveLoggedUsers;
import com.github.gcolin.auth.ActiveSession;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.github.gcolin.platform.JteHtml;

@Path("logged-users")
@RequirePermission(PermissionCode.ADMIN_PANEL)
public class LoggedUsersApi {

    private static final DateTimeFormatter LAST_SEEN_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @Inject
    private ActiveLoggedUsers activeLoggedUsers;

    @GET
    public JteHtml page() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ActiveSession session : activeLoggedUsers.listActive()) {
            Map<String, Object> row = new HashMap<>();
            row.put("email", session.getEmail());
            row.put("username", session.getUsername());
            row.put("admin", session.isAdmin());
            row.put("lastSeen", LAST_SEEN_FORMAT.format(Instant.ofEpochMilli(session.getLastSeenMillis())));
            row.put(
                    "sessionIdShort",
                    session.getSessionId().length() > 8
                            ? session.getSessionId().substring(0, 8) + "…"
                            : session.getSessionId());
            rows.add(row);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("sessions", rows);
        model.put("sessionCount", rows.size());
        return new JteHtml(model, "auth/loggedusers.jte");
    }
}

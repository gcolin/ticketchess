package com.github.gcolin.desk;

import com.github.gcolin.auth.RequirePermission;
import com.github.gcolin.platform.Config;
import com.github.gcolin.event.Event;
import com.github.gcolin.auth.PermissionCode;
import com.github.gcolin.desk.EventDeskEventDto;
import com.github.gcolin.desk.EventDeskService;
import com.github.gcolin.auth.LoggedUser;
import com.github.gcolin.event.EventDao;
import io.jsonwebtoken.Jwts;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.github.gcolin.platform.JteHtml;

@Path("event/{id:\\d+}/desk")
@RequirePermission(PermissionCode.EVENT_EDIT)
public class EventDeskApi {

    private static final Jsonb JSONB = JsonbBuilder.create();

    @Inject
    private EventDao eventDao;

    @Inject
    private EventDeskService eventDeskService;

    @Inject
    private LoggedUser loggedUser;

    @Inject
    private Config config;

    @GET
    public JteHtml show(@PathParam("id") Integer eventId) {
        Event event = eventDao.find(eventId);
        if (event == null) {
            throw new WebApplicationException("Event not found", Response.Status.NOT_FOUND);
        }
        List<EventDeskEventDto> events = eventDeskService.collectionSnapshots(eventId);
        List<Integer> eventIds = events.stream().map(EventDeskEventDto::getId).toList();
        String deskTicket = Jwts.builder()
                .subject(loggedUser.getEmail())
                .claim("eventId", eventId)
                .claim("eventIds", eventIds)
                .claim("scope", "desk")
                .claim("admin", loggedUser.isAdmin())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 12))
                .signWith(config.getKeys())
                .compact();
        Map<String, Object> model = new HashMap<>();
        model.put("event", event);
        model.put("eventsJson", JSONB.toJson(events));
        model.put("hasCollection", events.size() > 1);
        model.put("deskTicket", deskTicket);
        return new JteHtml(model, "desk/eventDesk.jte");
    }
}

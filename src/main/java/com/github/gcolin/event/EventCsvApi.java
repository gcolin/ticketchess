package com.github.gcolin.event;

import com.github.gcolin.auth.RequirePermission;
import com.github.gcolin.auth.PermissionCode;
import com.github.gcolin.event.EventDao;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("event/{id}/csv")
public class EventCsvApi {

    @Inject
    private EventDao eventService;

    @GET
    @RequirePermission(PermissionCode.EVENT_READ)
    public Response papi(@PathParam("id") Integer eventId) {

        String filename = eventId + ".csv";

        String csv = eventService.buildCsv(eventId);

        return Response.ok(csv)
                .type("text/csv")
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .build();
    }
}

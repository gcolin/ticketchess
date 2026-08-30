package com.github.gcolin.event;

import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("chessevent")
public class ChessEventApi {

    private static final Jsonb JSONB = JsonbBuilder.create();

    @Inject
    private ChessEventService chessEventService;

    @POST
    @Path("tournaments")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response tournaments(
            @FormParam("user_id") String userId,
            @FormParam("password") String password,
            @FormParam("event_id") String eventId) {
        try {
            List<String> tournaments = chessEventService.listTournaments(userId, password, eventId);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("tournaments", tournaments);
            return Response.ok(JSONB.toJson(body)).build();
        } catch (ChessEventException e) {
            return errorResponse(e);
        } catch (RuntimeException e) {
            return errorResponse(500, e.getMessage() == null ? "Internal error" : e.getMessage());
        }
    }

    @POST
    @Path("download")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response download(
            @FormParam("user_id") String userId,
            @FormParam("password") String password,
            @FormParam("event_id") String eventId,
            @FormParam("tournament_name") String tournamentName) {
        try {
            Map<String, Object> tournament =
                    chessEventService.downloadTournament(userId, password, eventId, tournamentName);
            return Response.ok(JSONB.toJson(tournament)).build();
        } catch (ChessEventException e) {
            return errorResponse(e);
        } catch (RuntimeException e) {
            return errorResponse(500, e.getMessage() == null ? "Internal error" : e.getMessage());
        }
    }

    private Response errorResponse(ChessEventException e) {
        return errorResponse(e.getStatus(), e.getError());
    }

    private Response errorResponse(int status, String message) {
        Map<String, String> body = Map.of("error", message);
        return Response.status(status).entity(JSONB.toJson(body)).build();
    }
}

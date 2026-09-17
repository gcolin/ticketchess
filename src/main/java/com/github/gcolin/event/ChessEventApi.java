package com.github.gcolin.event;

import io.jsonwebtoken.Claims;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ChessEvent-compatible API for Sharly Chess. Authenticated with a short-lived
 * Sharly import Bearer JWT ({@code scope=sharly}).
 */
@Path("chessevent")
public class ChessEventApi {

    private static final Jsonb JSONB = JsonbBuilder.create();

    @Inject
    private ChessEventService chessEventService;

    @Inject
    private SharlyImportTokenService sharlyImportTokenService;

    @POST
    @Path("tournaments")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response tournaments(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization,
            @FormParam("event_id") String eventId) {
        try {
            Claims claims = requireSharlyToken(authorization);
            List<String> tournaments = chessEventService.listTournamentsWithSharlyToken(
                    claims.get("event_id", String.class), eventId);
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
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization,
            @FormParam("event_id") String eventId,
            @FormParam("tournament_name") String tournamentName) {
        try {
            Claims claims = requireSharlyToken(authorization);
            Map<String, Object> tournament = chessEventService.downloadTournamentWithSharlyToken(
                    claims.get("event_id", String.class), eventId, tournamentName);
            return Response.ok(JSONB.toJson(tournament)).build();
        } catch (ChessEventException e) {
            return errorResponse(e);
        } catch (RuntimeException e) {
            return errorResponse(500, e.getMessage() == null ? "Internal error" : e.getMessage());
        }
    }

    private Claims requireSharlyToken(String authorization) throws ChessEventException {
        String bearer = SharlyImportTokenService.extractBearer(authorization);
        if (bearer == null) {
            throw new ChessEventException(401, "Unauthorized");
        }
        return sharlyImportTokenService.parseSharlyToken(bearer);
    }

    private Response errorResponse(ChessEventException e) {
        return errorResponse(e.getStatus(), e.getError());
    }

    private Response errorResponse(int status, String message) {
        Map<String, String> body = Map.of("error", message);
        return Response.status(status).entity(JSONB.toJson(body)).build();
    }
}

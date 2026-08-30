package com.github.gcolin.player;

import com.github.gcolin.auth.RequireRole;
import com.github.gcolin.auth.RoleCode;
import com.github.gcolin.player.LuceneDb;
import com.github.gcolin.player.ManualPlayerEntry;
import com.github.gcolin.player.Player;
import com.github.gcolin.platform.ServiceUtils;
import io.jsonwebtoken.lang.Collections;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.gcolin.platform.JteHtml;
import com.github.gcolin.platform.ModelUtils;

@Path("database")
public class DatabaseApi {

    @Inject
    private LuceneDb luceneDb;

    @Context
    private UriInfo uriInfo;

    private static Logger logger = LoggerFactory.getLogger(DatabaseApi.class);

    @GET
    @RequireRole(RoleCode.ADMIN)
    public JteHtml page() {
        return new JteHtml(Collections.emptyMap(), "player/database.jte");
    }

    @GET
    @Path("ffe")
    @RequireRole(RoleCode.ADMIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String getFfeInfo() {
        File file = luceneDb.getMdbFile();
        if (file == null || !file.exists()) {
            return "Data.mdb not found";
        }
        Instant instant = Instant.ofEpochMilli(file.lastModified());
        String iso = DateTimeFormatter.ISO_INSTANT.format(instant.atZone(ZoneId.systemDefault()));
        return file.getAbsolutePath() + " - " + iso + " - " + ServiceUtils.readable(file.length());
    }

    @GET
    @Path("ffe/download")
    @RequireRole(RoleCode.ADMIN)
    public Response downloadAndExtractFfeDb() {
        try {
            luceneDb.downloadAndExtractFFeDb();
            return Response.ok("Téléchargement et extraction OK").build();
        } catch (IOException e) {
            logger.error("cannot download FFE db", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("fide")
    @RequireRole(RoleCode.ADMIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String getFideInfo() {
        File file = luceneDb.getFideFile();
        if (file == null || !file.exists()) {
            return "Data.mdb not found";
        }
        Instant instant = Instant.ofEpochMilli(file.lastModified());
        String iso = DateTimeFormatter.ISO_INSTANT.format(instant.atZone(ZoneId.systemDefault()));
        return file.getAbsolutePath() + " - " + iso + " - " + ServiceUtils.readable(file.length());
    }

    @GET
    @Path("fide/download")
    @RequireRole(RoleCode.ADMIN)
    public Response downloadAndExtractFideDb() {
        try {
            luceneDb.downloadFideDB();
            return Response.ok("Téléchargement et extraction OK").build();
        } catch (IOException e) {
            logger.error("cannot download FFE db", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("reload")
    @RequireRole(RoleCode.ADMIN)
    public Response reload() {
        try {
            luceneDb.load(true);
            return Response.ok("reload OK").build();
        } catch (IOException e) {
            logger.error("cannot reload db", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("manual-players")
    @RequireRole(RoleCode.ADMIN)
    public JteHtml manualPlayersPage(@QueryParam("status") String status) throws IOException {
        Map<String, Object> model = new HashMap<>();
        model.put("players", luceneDb.getManualPlayers());
        model.put("manualPlayersFile", luceneDb.getManualPlayersFile().getAbsolutePath());
        model.put("status", status);
        return new JteHtml(model, "player/databasePlayers.jte");
    }

    @POST
    @Path("manual-players")
    @RequireRole(RoleCode.ADMIN)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addManualPlayer(
            @FormParam("nrffe") String nrffe,
            @FormParam("fide") String fide,
            @FormParam("name") String name,
            @FormParam("firstname") String firstname,
            @FormParam("birth") String birth,
            @FormParam("category") String category,
            @FormParam("federation") String federation,
            @FormParam("club") String club,
            @FormParam("eloStd") String eloStd,
            @FormParam("eloRapide") String eloRapide,
            @FormParam("eloBlitz") String eloBlitz,
            @FormParam("fideTitre") String fideTitre) {
        try {
            ManualPlayerEntry entry = new ManualPlayerEntry();
            entry.setNrffe(nrffe);
            entry.setFide(fide);
            entry.setName(name);
            entry.setFirstname(firstname);
            entry.setBirth(birth);
            entry.setCategory(category);
            entry.setFederation(federation);
            entry.setClub(club);
            entry.setEloStd(eloStd);
            entry.setEloRapide(eloRapide);
            entry.setEloBlitz(eloBlitz);
            entry.setFideTitre(fideTitre);
            luceneDb.addManualPlayer(entry);
            return redirectToManualPlayers("added");
        } catch (IllegalArgumentException e) {
            return redirectToManualPlayers("validation");
        } catch (IOException e) {
            logger.error("cannot add manual player", e);
            return redirectToManualPlayers("error");
        }
    }

    @POST
    @Path("manual-players/delete")
    @RequireRole(RoleCode.ADMIN)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response deleteManualPlayer(@FormParam("key") String key) {
        try {
            boolean removed = luceneDb.removeManualPlayer(key);
            if (removed) {
                luceneDb.load(true);
            }
            return redirectToManualPlayers(removed ? "deleted" : "missing");
        } catch (IOException e) {
            logger.error("cannot remove manual player {}", key, e);
            return redirectToManualPlayers("error");
        }
    }

    @GET
    @Path("manual-players/prefill")
    @RequireRole(RoleCode.ADMIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Player prefillManualPlayer(@QueryParam("nrffe") String nrffe, @QueryParam("fide") String fide) {
        try {
            Player player = null;
            String nrffeValue = com.github.gcolin.platform.ModelUtils.trimToNull(nrffe);
            String fideValue = com.github.gcolin.platform.ModelUtils.trimToNull(fide);

            if (nrffeValue != null) {
                player = luceneDb.searchJoueurByNrffe(nrffeValue);
            }
            if (player == null && fideValue != null) {
                player = luceneDb.searchJoueurByFide(fideValue);
            }
            if (player == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            return player;
        } catch (IOException e) {
            throw new WebApplicationException(e);
        }
    }

    private Response redirectToManualPlayers(String status) {
        URI uri = uriInfo.getBaseUriBuilder()
                .path("database")
                .path("manual-players")
                .queryParam("status", status)
                .build();
        return Response.seeOther(uri).build();
    }
}

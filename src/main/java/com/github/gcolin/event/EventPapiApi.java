package com.github.gcolin.event;

import com.github.gcolin.auth.RequireRole;
import com.github.gcolin.event.EventOption;
import com.github.gcolin.event.EventOptionType;
import com.github.gcolin.auth.RoleCode;
import com.github.gcolin.event.PapiUlploadService;
import com.github.gcolin.event.PapiService;
import com.github.gcolin.event.EventDao;
import com.github.gcolin.event.EventOptionDao;
import com.github.gcolin.event.EventCache;
import com.github.gcolin.player.DisplayPlayer;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("event/{id}/papi")
public class EventPapiApi {

    @Inject
    private EventDao eventService;

    @Inject
    private PapiService papiService;

    @Inject
    private PapiUlploadService papiUlploadService;

    @Inject
    private EventOptionDao eventOptionService;

    private static final Logger logger = LoggerFactory.getLogger(EventPapiApi.class);

    @GET
    @RequireRole(RoleCode.ARBITRE)
    public Response papi(@PathParam("id") Integer eventId) {
        EventCache cache = eventService.buildCache(eventId);
        List<DisplayPlayer> players = filterCancelledPlayers(cache);

        File file;
        try {
            file = papiService.generatePapiFile(cache.event, players);
        } catch (IOException e) {
            logger.error(e.toString(), e);
            throw new WebApplicationException(e);
        }

        String filename = cache.event.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".papi";

        StreamingOutput stream = output -> {
            try (InputStream in = new FileInputStream(file)) {
                in.transferTo(output);
            } finally {
                file.delete();
            }
        };

        return Response.ok(stream)
                .type("application/x-msaccess")
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .build();
    }

    @POST
    @Path("upload")
    @RequireRole(RoleCode.EVENT_ADMIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response uploadToFfe(@PathParam("id") Integer eventId) {
        EventCache cache = eventService.buildCache(eventId);
        List<DisplayPlayer> players = filterCancelledPlayers(cache);

        String login = getOption(eventId, EventOptionType.FFE_ID);
        String password = getOption(eventId, EventOptionType.FFE_PASSWORD);
        if (login == null || login.isBlank() || password == null || password.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("FFE_ID ou FFE_PASSWORD manquant dans les options de l'evenement")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        File file;
        try {
            file = papiService.generatePapiFile(cache.event, players);
        } catch (IOException e) {
            logger.error(e.toString(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Impossible de generer le fichier PAPI")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        try {
            papiUlploadService.upload(login, password, file.toPath());
            return Response.ok("Upload PAPI termine").type(MediaType.TEXT_PLAIN).build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error(e.toString(), e);
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity("Upload PAPI interrompu")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        } catch (IOException | WebApplicationException e) {
            logger.error(e.toString(), e);
            String message = e.getMessage() == null || e.getMessage().isBlank() ? "Echec upload PAPI" : e.getMessage();
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(message)
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        } finally {
            file.delete();
        }
    }

    private String getOption(Integer eventId, EventOptionType optionType) {
        EventOption option = eventOptionService.findByEventIdAndOptionType(eventId, optionType);
        return option == null ? null : option.getValue();
    }

    private List<DisplayPlayer> filterCancelledPlayers(EventCache cache) {
        if (cache.players == null) {
            return List.of();
        }
        return cache.players.stream()
                .filter(player -> player.getStatus() != PlayerSubscriptionStatus.CANCELLED)
                .toList();
    }
}

package com.github.gcolin.event;

import com.github.gcolin.auth.RequireRole;
import com.github.gcolin.event.Event;
import com.github.gcolin.auth.RoleCode;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.player.LuceneDb;
import com.github.gcolin.event.EventType;
import com.github.gcolin.player.Player;
import com.github.gcolin.player.IPlayer;
import com.github.gcolin.player.Find;
import com.github.gcolin.event.EventDao;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import com.github.gcolin.player.CustomPlayer;

/**
 * Rapport CSV des joueurs sans licence valide pour un événement donné.
 * <ul>
 *   <li>Cadence Classique ({@link EventType#STANDARD}) : licence A requise</li>
 *   <li>Autres cadences : numéro FIDE ou FFE présent dans Lucene</li>
 * </ul>
 */
@Path("event/{id}/licencecheck")
public class EventLicenceCheckApi {

    @Inject
    private EventDao eventDao;

    @Inject
    private PlayerSubscriptionDao playerSubscriptionDao;

    @Inject
    private LuceneDb luceneDb;

    @Inject
    private Find find;

    @GET
    @RequireRole(RoleCode.ARBITRE)
    public Response licenceCheck(@PathParam("id") Integer eventId) throws IOException {

        Event event = eventDao.find(eventId);
        if (event == null) {
            throw new WebApplicationException("Event not found", Response.Status.NOT_FOUND);
        }

        boolean requiresLicenceA = event.getEventType() == EventType.STANDARD;
        List<PlayerSubscription> subscriptions = playerSubscriptionDao.findByEvent(event);

        StringBuilder csv = new StringBuilder();
        csv.append("nom/prenom;eventName;eventId;subId;error\n");

        for (PlayerSubscription sub : subscriptions) {
            String nrFfe = sub.getNrFfe();
            if (nrFfe == null || nrFfe.isEmpty()) {
                continue;
            }

            String playerName = nrFfe;
            String error = null;

            if (!nrFfe.startsWith("@")) {
                try {
                    Player player = luceneDb.searchJoueur(nrFfe);
                    if (player != null) {
                        if (requiresLicenceA) {
                            if (!"A".equals(player.getAffType())) {
                                error = "licence A manquante";
                            }
                        } else {
                            String ffe = player.getNrffe();
                            String fide = player.getFide();
                            boolean hasFfe = ffe != null && !ffe.isEmpty();
                            boolean hasFide = fide != null && !fide.isEmpty() && !"0".equals(fide);
                            if (!hasFfe && !hasFide) {
                                error = "sans numéro FFE/FIDE";
                            }
                        }

                        if (player.getName() != null) {
                            playerName = player.getName();
                            if (player.getFirstname() != null && !player.getFirstname().isEmpty()) {
                                playerName = player.getName() + " " + player.getFirstname();
                            }
                        }
                    } else {
                        error = "joueur introuvable";
                        IPlayer fallback = find.player(nrFfe, null);
                        if (fallback != null && fallback.getName() != null) {
                            playerName = fallback.getName();
                            if (fallback.getFirstname() != null && !fallback.getFirstname().isEmpty()) {
                                playerName = fallback.getName() + " " + fallback.getFirstname();
                            }
                        }
                    }
                } catch (Exception ignored) {
                    error = "joueur introuvable";
                }
            } else {
                // CustomPlayer (@) : utilise Find pour récupérer le nom
                error = "joueur custom";
                IPlayer fallback = find.player(nrFfe, null);
                if (fallback != null && fallback.getName() != null) {
                    playerName = fallback.getName();
                    if (fallback.getFirstname() != null && !fallback.getFirstname().isEmpty()) {
                        playerName = fallback.getName() + " " + fallback.getFirstname();
                    }
                }
            }

            if (error != null) {
                csv.append(escapeCsv(playerName)).append(";");
                csv.append(escapeCsv(event.getName())).append(";");
                csv.append(eventId).append(";");
                csv.append(sub.getId()).append(";");
                csv.append(escapeCsv(error)).append("\n");
            }
        }

        String filename = "licencecheck-" + eventId + ".csv";
        return Response.ok(csv.toString())
                .type("text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .build();
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

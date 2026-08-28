package com.github.gcolin.registration;

import com.github.gcolin.auth.RequirePermission;
import com.github.gcolin.club.ClubSeasonFilter;
import com.github.gcolin.club.SeasonScope;
import com.github.gcolin.player.CustomPlayer;
import com.github.gcolin.auth.PermissionCode;
import com.github.gcolin.registration.PlayerPendingSubscription;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.player.IPlayer;
import com.github.gcolin.platform.Caches;
import com.github.gcolin.player.Find;
import com.github.gcolin.player.CustomPlayerDao;
import com.github.gcolin.registration.PlayerPendingSubscriptionDao;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.github.gcolin.platform.JteHtml;

@Path("playersubscription-admin")
@RequirePermission(PermissionCode.EVENT_EDIT)
public class PlayerSubscriptionAdminApi {

    @Inject
    private PlayerSubscriptionDao playerSubscriptionService;

    @Inject
    private CustomPlayerDao customPlayerService;

    @Inject
    private PlayerPendingSubscriptionDao playerPendingSubscriptionService;

    @Inject
    private Find find;

    @Inject
    private Caches caches;

    @Inject
    private ClubSeasonFilter clubSeasonFilter;

    @Context
    private UriInfo uriInfo;

    @GET
    public JteHtml page(@jakarta.ws.rs.QueryParam("seasonId") Integer seasonId) {
        return page(null, null, null, null, seasonId);
    }

    @GET
    @Path("result")
    public JteHtml pageResult(
            @jakarta.ws.rs.QueryParam("replaced") Integer replaced,
            @jakarta.ws.rs.QueryParam("failed") Integer failed,
            @jakarta.ws.rs.QueryParam("deleted") Integer deleted,
            @jakarta.ws.rs.QueryParam("deleteFailed") Integer deleteFailed,
            @jakarta.ws.rs.QueryParam("seasonId") Integer seasonId) {
        return page(replaced, failed, deleted, deleteFailed, seasonId);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response replace(
            @FormParam("subscriptionId") Integer subscriptionId,
            @FormParam("all") @DefaultValue("false") boolean all,
            @FormParam("deleteOrphans") @DefaultValue("false") boolean deleteOrphans,
            @FormParam("deletePendingId") Integer deletePendingId,
            @FormParam("seasonId") Integer seasonId) {
        int replaced = 0;
        int failed = 0;
        int deleted = 0;
        int deleteFailed = 0;
        boolean pendingRemoved = false;
        SeasonScope scope = clubSeasonFilter.resolve(seasonId);

        if (deletePendingId != null) {
            PlayerPendingSubscription pending = playerPendingSubscriptionService.find(deletePendingId);
            if (pending != null) {
                playerPendingSubscriptionService.remove(pending);
                pendingRemoved = true;
                deleted = 1;
            } else {
                deleteFailed = 1;
            }
        } else if (deleteOrphans) {
            for (CustomPlayer customPlayer : customPlayerService.findWithoutSubscription()) {
                try {
                    customPlayerService.remove(customPlayer);
                    deleted++;
                } catch (Exception ex) {
                    deleteFailed++;
                }
            }
        } else if (all) {
            for (PlayerSubscription subscription : playerSubscriptionService.findLinkedToCustomPlayers(scope)) {
                ReplacementCandidate candidate = buildCandidate(subscription);
                if (candidate != null && candidate.canReplace) {
                    subscription.setNrFfe(candidate.replacementNrFfe);
                    playerSubscriptionService.merge(subscription);
                    replaced++;
                } else {
                    failed++;
                }
            }
        } else if (subscriptionId != null) {
            PlayerSubscription subscription = playerSubscriptionService.find(subscriptionId);
            ReplacementCandidate candidate = buildCandidate(subscription);
            if (candidate != null && candidate.canReplace) {
                subscription.setNrFfe(candidate.replacementNrFfe);
                playerSubscriptionService.merge(subscription);
                replaced++;
            } else {
                failed = 1;
            }
        }

        if (replaced > 0 || pendingRemoved) {
            caches.getEvent().invalidateAll();
            caches.getAllEvents().invalidateAll();
            caches.getDebtCache().invalidateAll();
        }

        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder()
                .path("playersubscription-admin")
                .path("result")
                .queryParam("replaced", replaced)
                .queryParam("failed", failed)
                .queryParam("deleted", deleted)
                .queryParam("deleteFailed", deleteFailed);
        if (seasonId != null) {
            uriBuilder.queryParam("seasonId", seasonId);
        }
        return Response.seeOther(uriBuilder.build()).build();
    }

    private JteHtml page(Integer replaced, Integer failed, Integer deleted, Integer deleteFailed, Integer seasonId) {
        SeasonScope scope = clubSeasonFilter.resolve(seasonId);
        List<Map<String, Object>> rows = new ArrayList<>();
        List<Map<String, Object>> pendingRows = new ArrayList<>();
        int replaceableCount = 0;

        for (PlayerSubscription subscription : playerSubscriptionService.findLinkedToCustomPlayers(scope)) {
            ReplacementCandidate candidate = buildCandidate(subscription);
            if (candidate == null) {
                continue;
            }
            if (candidate.canReplace) {
                replaceableCount++;
            }

            Map<String, Object> row = new HashMap<>();
            row.put("subscriptionId", subscription.getId());
            row.put("eventId", subscription.getEvent().getId());
            row.put("eventName", subscription.getEvent().getName());
            row.put("creationUser", subscription.getCreationUser());
            row.put("currentRef", subscription.getNrFfe());
            row.put("customPlayerId", candidate.customPlayerId);
            row.put("customPlayerName", candidate.customPlayerName);
            row.put("customPlayerLicence", candidate.customPlayerLicence);
            row.put("luceneName", candidate.lucenePlayerName);
            row.put("replacementNrFfe", candidate.replacementNrFfe);
            row.put("canReplace", candidate.canReplace);
            row.put("reason", candidate.reason);
            rows.add(row);
        }

        for (PlayerPendingSubscription pendingSubscription : playerPendingSubscriptionService.findAllWithEvent(scope)) {
            IPlayer player = resolvePlayer(pendingSubscription.getNrFfe());
            Map<String, Object> row = new HashMap<>();
            row.put("id", pendingSubscription.getId());
            row.put("licence", pendingSubscription.getNrFfe());
            row.put("lastname", player == null ? null : player.getName());
            row.put("firstname", player == null ? null : player.getFirstname());
            row.put("creationUser", pendingSubscription.getCreationUser());
            row.put("eventId", pendingSubscription.getEvent().getId());
            row.put("eventName", pendingSubscription.getEvent().getName());
            pendingRows.add(row);
        }

        Map<String, Object> model = new HashMap<>();
        List<CustomPlayer> orphanCustomPlayers = customPlayerService.findWithoutSubscription();
        model.put("rows", rows);
        model.put("rowCount", rows.size());
        model.put("pendingRows", pendingRows);
        model.put("pendingRowCount", pendingRows.size());
        model.put("replaceableCount", replaceableCount);
        model.put("orphanCount", orphanCustomPlayers.size());
        model.put("orphanCustomPlayers", orphanCustomPlayers);
        model.put("replaced", replaced);
        model.put("failed", failed);
        model.put("deleted", deleted);
        model.put("deleteFailed", deleteFailed);
        clubSeasonFilter.addToModel(model, seasonId);
        return new JteHtml(model, "registration/playersubscriptionAdmin.jte");
    }

    private IPlayer resolvePlayer(String nrFfe) {
        String trimmedNrFfe = trim(nrFfe);
        if (trimmedNrFfe == null) {
            return null;
        }
        return find.player(trimmedNrFfe, null);
    }

    private ReplacementCandidate buildCandidate(PlayerSubscription subscription) {
        if (subscription == null
                || subscription.getNrFfe() == null
                || !subscription.getNrFfe().startsWith("@")) {
            return null;
        }

        Integer customPlayerId;
        try {
            customPlayerId = Integer.parseInt(subscription.getNrFfe().substring(1));
        } catch (NumberFormatException ex) {
            return new ReplacementCandidate(false, "customId.invalid", null, null, null, null);
        }

        CustomPlayer customPlayer = customPlayerService.find(customPlayerId);
        if (customPlayer == null) {
            return new ReplacementCandidate(false, "customPlayer.missing", customPlayerId, null, null, null);
        }

        String licence = trim(customPlayer.getLicence());
        if (licence == null) {
            return new ReplacementCandidate(
                    false,
                    "customPlayer.licence.missing",
                    customPlayerId,
                    customPlayer.getFirstname() + " " + customPlayer.getName(),
                    null,
                    null);
        }

        IPlayer lucenePlayer = find.player(licence, null);
        if (lucenePlayer == null || lucenePlayer.isEditable()) {
            return new ReplacementCandidate(
                    false,
                    "lucenePlayer.missing",
                    customPlayerId,
                    customPlayer.getFirstname() + " " + customPlayer.getName(),
                    licence,
                    null);
        }

        String replacementNrFfe = trim(lucenePlayer.getNrffe());
        if (replacementNrFfe == null) {
            replacementNrFfe = trim(lucenePlayer.getFide());
        }
        if (replacementNrFfe == null) {
            replacementNrFfe = trim(lucenePlayer.getLicence());
        }
        if (replacementNrFfe == null || replacementNrFfe.startsWith("@")) {
            return new ReplacementCandidate(
                    false,
                    "lucenePlayer.licence.invalid",
                    customPlayerId,
                    customPlayer.getFirstname() + " " + customPlayer.getName(),
                    licence,
                    lucenePlayer.getFirstname() + " " + lucenePlayer.getName());
        }

        return new ReplacementCandidate(
                !replacementNrFfe.equals(subscription.getNrFfe()),
                !replacementNrFfe.equals(subscription.getNrFfe()) ? "ok" : "already.replaced",
                customPlayerId,
                customPlayer.getFirstname() + " " + customPlayer.getName(),
                licence,
                lucenePlayer.getFirstname() + " " + lucenePlayer.getName(),
                replacementNrFfe);
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static class ReplacementCandidate {
        private final boolean canReplace;
        private final String reason;
        private final Integer customPlayerId;
        private final String customPlayerName;
        private final String customPlayerLicence;
        private final String lucenePlayerName;
        private final String replacementNrFfe;

        private ReplacementCandidate(
                boolean canReplace,
                String reason,
                Integer customPlayerId,
                String customPlayerName,
                String customPlayerLicence,
                String lucenePlayerName) {
            this(canReplace, reason, customPlayerId, customPlayerName, customPlayerLicence, lucenePlayerName, null);
        }

        private ReplacementCandidate(
                boolean canReplace,
                String reason,
                Integer customPlayerId,
                String customPlayerName,
                String customPlayerLicence,
                String lucenePlayerName,
                String replacementNrFfe) {
            this.canReplace = canReplace;
            this.reason = reason;
            this.customPlayerId = customPlayerId;
            this.customPlayerName = customPlayerName;
            this.customPlayerLicence = customPlayerLicence;
            this.lucenePlayerName = lucenePlayerName;
            this.replacementNrFfe = replacementNrFfe;
        }
    }
}

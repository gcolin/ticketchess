package com.github.gcolin.event;

import com.github.gcolin.auth.RequirePermission;
import com.github.gcolin.event.Event;
import com.github.gcolin.auth.PermissionCode;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.player.IPlayer;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import com.github.gcolin.player.Find;
import com.github.gcolin.event.EventDao;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import com.github.gcolin.platform.BroadcastMail;
import com.github.gcolin.platform.MailTemplate;
import com.github.gcolin.platform.SendMail;
import com.github.gcolin.platform.Config;
import io.jsonwebtoken.Jwts;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.gcolin.platform.JteHtml;

@Path("event/{id:\\d+}/mail")
@RequirePermission(PermissionCode.MAIL_SEND)
public class EventMailApi {

    @Inject
    private EventDao eventService;

    @Inject
    private PlayerSubscriptionDao playerSubscriptionService;

    @Inject
    private Config config;

    @Inject
    private SendMail sendMail;

    @Context
    UriInfo uriInfo;

    @Inject
    private Find find;

    private static final Logger logger = LoggerFactory.getLogger(EventMailApi.class);

    @GET
    public JteHtml showForm(@PathParam("id") Integer eventId, @QueryParam("sent") Integer sent) {
        Event event = eventService.find(eventId);
        Map<String, Object> model = new HashMap<>();
        model.put("event", event);
        model.put("recipients", buildRecipients(event));
        if (sent != null) {
            model.put("sent", sent);
        }
        return new JteHtml(model, "event/eventMail.jte");
    }

    @POST
    @Path("preview")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response preview(
            @PathParam("id") Integer eventId, @FormParam("body") String body, @FormParam("bcc") boolean bcc) {
        Event event = eventService.find(eventId);
        Parser parser = Parser.builder().build();
        Node document = parser.parse(body == null ? "" : body);
        HtmlRenderer renderer = HtmlRenderer.builder().escapeHtml(true).build();

        BroadcastMail mail = new BroadcastMail();
        mail.setEventName(event.getName());
        mail.setName(bcc ? "" : "NOM Prénom");
        mail.setBody(renderer.render(document));

        try {
            config.applyOrg(mail);
            MailTemplate tmpl = new MailTemplate();
            String html = tmpl.render(mail.getTemplate(), mail);
            return Response.ok(html, MediaType.TEXT_HTML).build();
        } catch (Exception e) {
            logger.error("Failed to render mail preview for event {}: {}", eventId, e.getMessage());
            return Response.serverError().build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response sendMails(
            @PathParam("id") Integer eventId,
            @FormParam("subject") String subject,
            @FormParam("body") String body,
            @FormParam("subscriptionStatus") String subscriptionStatus,
            @FormParam("sendAsBcc") String sendAsBcc,
            @FormParam("sendInBatches") String sendInBatches,
            @FormParam("recipientFilterEnabled") String recipientFilterEnabled,
            @FormParam("selectedSubscriptions") List<Integer> selectedSubscriptions) {
        Event event = eventService.find(eventId);
        List<PlayerSubscription> subscriptions = playerSubscriptionService.findByEvent(event);
        boolean useRecipientFilter = "true".equals(recipientFilterEnabled);
        Set<Integer> selectedSubscriptionIds = toIdSet(selectedSubscriptions);
        boolean batched = "true".equals(sendInBatches);
        boolean bcc = batched || "true".equals(sendAsBcc);

        int sent = bcc
            ? sendBccMails(
                event,
                subscriptions,
                body,
                subject,
                subscriptionStatus,
                useRecipientFilter,
                selectedSubscriptionIds,
                eventId,
                batched)
            : sendIndividualMails(
                event,
                subscriptions,
                body,
                subject,
                subscriptionStatus,
                useRecipientFilter,
                selectedSubscriptionIds,
                eventId);

        URI location = uriInfo.getBaseUriBuilder()
                .path("event/{id}/mail")
                .queryParam("sent", sent)
                .build(eventId);
        return Response.seeOther(location).build();
    }

    private int sendBccMails(
            Event event,
            List<PlayerSubscription> subscriptions,
            String body,
            String subject,
            String subscriptionStatus,
            boolean useRecipientFilter,
            Set<Integer> selectedSubscriptionIds,
            Integer eventId,
            boolean batched) {
        List<String> bccAddresses = new ArrayList<>();
        for (PlayerSubscription sub : subscriptions) {
            if (statusMatches(sub, subscriptionStatus)
                    && selectedMatches(sub, useRecipientFilter, selectedSubscriptionIds)
                    && isValidCreationUser(sub)) {
                bccAddresses.add(sub.getCreationUser());
            }
        }
        if (bccAddresses.isEmpty()) {
            return 0;
        }
        try {
            BroadcastMail mail = buildBroadcastMail(event, body, "", null);
            if (batched) {
                return sendMail.sendBccBatched(mail, bccAddresses, subject);
            }
            sendMail.sendBcc(mail, bccAddresses, subject);
            return bccAddresses.size();
        } catch (Exception e) {
            logger.error("Failed to send BCC broadcast mail for event {}: {}", eventId, e.getMessage());
            return 0;
        }
    }

    private int sendIndividualMails(
            Event event,
            List<PlayerSubscription> subscriptions,
            String body,
            String subject,
            String subscriptionStatus,
            boolean useRecipientFilter,
            Set<Integer> selectedSubscriptionIds,
            Integer eventId) {
        int sent = 0;
        for (PlayerSubscription sub : subscriptions) {
            if (statusMatches(sub, subscriptionStatus)
                    && selectedMatches(sub, useRecipientFilter, selectedSubscriptionIds)
                    && isValidCreationUser(sub)) {
                try {
                    IPlayer player = find.player(sub.getNrFfe(), null);
                    String fullName = player != null ? player.getFullname() : "";
                    BroadcastMail mail = buildBroadcastMail(event, body, fullName, sub.getCreationUser());
                    sendMail.send(mail, sub.getCreationUser(), subject);
                    sent++;
                } catch (Exception e) {
                    logger.error("Failed to send broadcast mail for event {}: {}", eventId, e.getMessage());
                }
            }
        }
        return sent;
    }

    private boolean statusMatches(PlayerSubscription sub, String subscriptionStatus) {
        if (subscriptionStatus == null || subscriptionStatus.isEmpty()) {
            return sub.getStatus() != PlayerSubscriptionStatus.CANCELLED;
        }
        try {
            return sub.getStatus() == PlayerSubscriptionStatus.valueOf(subscriptionStatus);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isValidCreationUser(PlayerSubscription sub) {
        return sub.getCreationUser() != null && !sub.getCreationUser().isEmpty();
    }

    private boolean selectedMatches(
            PlayerSubscription sub, boolean useRecipientFilter, Set<Integer> selectedSubscriptionIds) {
        if (!useRecipientFilter) {
            return true;
        }
        return selectedSubscriptionIds.contains(sub.getId());
    }

    private Set<Integer> toIdSet(List<Integer> selectedSubscriptions) {
        if (selectedSubscriptions == null || selectedSubscriptions.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(selectedSubscriptions);
    }

    private List<MailRecipient> buildRecipients(Event event) {
        if (playerSubscriptionService == null) {
            return Collections.emptyList();
        }
        List<PlayerSubscription> subscriptions = playerSubscriptionService.findByEvent(event);
        List<MailRecipient> recipients = new ArrayList<>();
        for (PlayerSubscription sub : subscriptions) {
            if (!isValidCreationUser(sub) || sub.getStatus() == PlayerSubscriptionStatus.CANCELLED) {
                continue;
            }
            IPlayer player = null;
            try {
                if (find == null) {
                    recipients.add(new MailRecipient(sub.getId(), "", "", sub.getCreationUser(), sub.getStatus()));
                    continue;
                }
                player = find.player(sub.getNrFfe(), null);
            } catch (Exception e) {
                logger.debug("Could not load player {} for mail recipients: {}", sub.getNrFfe(), e.getMessage());
            }
            String lastname = player != null && player.getName() != null ? player.getName() : "";
            String firstname = player != null && player.getFirstname() != null ? player.getFirstname() : "";
            recipients.add(new MailRecipient(sub.getId(), lastname, firstname, sub.getCreationUser(), sub.getStatus()));
        }
        recipients.sort((a, b) -> {
            int nameCompare = a.getLastname().compareToIgnoreCase(b.getLastname());
            if (nameCompare != 0) {
                return nameCompare;
            }
            return a.getFirstname().compareToIgnoreCase(b.getFirstname());
        });
        return recipients;
    }

    private BroadcastMail buildBroadcastMail(Event event, String body, String playerName, String emailAddress) {
        BroadcastMail mail = new BroadcastMail();
        mail.setEventName(event.getName());
        mail.setName(playerName);
        Parser parser = Parser.builder().build();
        Node document = parser.parse(body);
        HtmlRenderer renderer = HtmlRenderer.builder().escapeHtml(true).build();
        mail.setBody(renderer.render(document));
        
        if (emailAddress != null && !emailAddress.isBlank()) {
            String jwt = Jwts.builder()
                .subject(emailAddress.trim())
                .issuer(event.getName())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30))
                .signWith(config.getKeys())
                .compact();
            String loginUrl = uriInfo.getBaseUri() + "login?jwt=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8);
            mail.setLoginUrl(loginUrl);
        }
        return mail;
    }

    public static class MailRecipient {

        private final Integer id;
        private final String lastname;
        private final String firstname;
        private final String email;
        private final PlayerSubscriptionStatus status;

        public MailRecipient(
                Integer id, String lastname, String firstname, String email, PlayerSubscriptionStatus status) {
            this.id = id;
            this.lastname = lastname;
            this.firstname = firstname;
            this.email = email;
            this.status = status;
        }

        public Integer getId() {
            return id;
        }

        public String getLastname() {
            return lastname;
        }

        public String getFirstname() {
            return firstname;
        }

        public String getEmail() {
            return email;
        }

        public PlayerSubscriptionStatus getStatus() {
            return status;
        }
    }
}

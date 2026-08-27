package com.github.gcolin.membership;

import com.github.gcolin.auth.RequirePermission;
import com.github.gcolin.membership.Membership;
import com.github.gcolin.membership.MembershipOption;
import com.github.gcolin.membership.MembershipOptionSubscription;
import com.github.gcolin.membership.MembershipStatus;
import com.github.gcolin.auth.PermissionCode;
import com.github.gcolin.membership.MembershipDao;
import com.github.gcolin.platform.Config;
import com.github.gcolin.membership.MembershipOptionDao;
import com.github.gcolin.membership.MembershipOptionSubscriptionDao;
import com.github.gcolin.platform.BroadcastMail;
import com.github.gcolin.platform.MailTemplate;
import com.github.gcolin.platform.SendMail;
import io.jsonwebtoken.Jwts;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.io.StringWriter;
import java.net.URI;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.gcolin.platform.JteHtml;

@RequirePermission(PermissionCode.EVENT_EDIT)
@Path("membership")
public class MembershipApi {

    @Inject
    private MembershipDao membershipDao;

    @Inject
    private MembershipOptionDao membershipOptionDao;

    @Inject
    private MembershipOptionSubscriptionDao membershipOptionSubscriptionDao;

    @Inject
    private SendMail sendMail;

    @Inject
    private Config config;

    private static final Logger logger = LoggerFactory.getLogger(MembershipApi.class);

    @Context
    UriInfo uriInfo;

    @GET
    public JteHtml list() {
        List<Membership> memberships = membershipDao.all();
        memberships.sort(Comparator.comparing(Membership::getId, Comparator.nullsLast(Integer::compareTo)).reversed());

        List<MembershipOptionSubscription> allSubscriptions = membershipOptionSubscriptionDao.all();
        Map<String, List<MembershipOptionSubscription>> subscriptionsByMembership = new HashMap<>();
        for (MembershipOptionSubscription sub : allSubscriptions) {
            String membershipId = sub.getMembership().getId().toString();
            subscriptionsByMembership.computeIfAbsent(membershipId, k -> new java.util.ArrayList<>()).add(sub);
        }

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("memberships", memberships);
        model.put("membershipSubscriptions", subscriptionsByMembership);
        return new JteHtml(model, "membership/membership.jte");
    }

    @GET
    @Path("export/csv")
    @Produces("text/csv; charset=UTF-8")
    public Response exportCsv() {
        List<Membership> memberships = new java.util.ArrayList<>(membershipDao.all());
        memberships.sort(Comparator.comparing(Membership::getId, Comparator.nullsLast(Integer::compareTo)).reversed());

        Map<Integer, List<String>> optionsByMembership = new HashMap<>();
        for (MembershipOptionSubscription sub : membershipOptionSubscriptionDao.all()) {
            if (sub.getMembership() == null || sub.getMembership().getId() == null) {
                continue;
            }
            String optionValue = sub.getMembershipOption() != null ? sub.getMembershipOption().getOptionValue() : null;
            if (optionValue == null || optionValue.isBlank()) {
                continue;
            }
            optionsByMembership
                    .computeIfAbsent(sub.getMembership().getId(), k -> new java.util.ArrayList<>())
                    .add(optionValue);
        }

        StringWriter writer = new StringWriter();
        writer.append("id;User;Nr FFE;Nom;Prénom;Date de naissance;Status;Options;Created;Updated\n");

        for (Membership membership : memberships) {
            List<String> options = optionsByMembership.getOrDefault(membership.getId(), List.of());
            writer.append(String.valueOf(membership.getId())).append(";");
            writer.append(safe(membership.getUser())).append(";");
            writer.append(safe(membership.getNrFfe())).append(";");
            writer.append(safe(membership.getLastname())).append(";");
            writer.append(safe(membership.getFirstname())).append(";");
            writer.append(safe(membership.getBirthDate())).append(";");
            writer.append(safe(membership.getStatus() != null ? membership.getStatus().name() : "")).append(";");
            writer.append(safe(String.join(", ", options))).append(";");
            writer.append(safe(String.valueOf(membership.getCreatedAt()))).append(";");
            writer.append(safe(String.valueOf(membership.getUpdatedAt()))).append("\n");
        }

        return Response.ok(writer.toString())
                .header("Content-Disposition", "attachment; filename=memberships.csv")
                .build();
    }

    @GET
    @Path("new")
    public JteHtml createPage() {
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("membership", new Membership());
        model.put("statuses", MembershipStatus.values());
        return new JteHtml(model, "membership/membershipNew.jte");
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response create(
            @FormParam("user") String user,
            @FormParam("nrFfe") String nrFfe,
            @FormParam("lastname") String lastname,
            @FormParam("firstname") String firstname,
            @FormParam("birthDate") String birthDate,
            @FormParam("clubRef") Integer clubRef,
            @FormParam("status") String status,
            @FormParam("amountCents") Integer amountCents) {
        Membership membership = new Membership();
        membership.setUser(user);
        membership.setNrFfe(nrFfe);
        membership.setLastname(lastname);
        membership.setFirstname(firstname);
        membership.setBirthDate(birthDate);
        membership.setClubRef(clubRef == null ? 0 : clubRef);
        membership.setAmountCents(amountCents == null ? 0 : amountCents);

        MembershipStatus parsedStatus = MembershipStatus.PENDING_APPROVAL;
        if (status != null && !status.isBlank()) {
            parsedStatus = MembershipStatus.valueOf(status);
        }
        membership.setStatus(parsedStatus);

        membershipDao.persist(membership);

        URI redirect = uriInfo.getBaseUriBuilder().path("membership").build();
        return Response.seeOther(redirect).build();
    }

    @GET
    @Path("{id}/edit")
    public JteHtml editPage(@PathParam("id") Integer id) {
        Membership membership = membershipDao.find(id);
        if (membership == null) {
            throw new jakarta.ws.rs.NotFoundException("Membership not found");
        }

        List<MembershipOptionSubscription> subscriptions = membershipOptionSubscriptionDao.all()
            .stream()
            .filter(s -> s.getMembership().getId().equals(id))
            .toList();

        List<MembershipOption> availableOptions = membershipOptionDao.all();

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("membership", membership);
        model.put("statuses", MembershipStatus.values());
        model.put("membershipOptions", subscriptions);
        model.put("availableOptions", availableOptions);
        return new JteHtml(model, "membership/membershipEdit.jte");
    }

    @POST
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response update(
            @PathParam("id") Integer id,
            @FormParam("user") String user,
            @FormParam("nrFfe") String nrFfe,
            @FormParam("lastname") String lastname,
            @FormParam("firstname") String firstname,
            @FormParam("birthDate") String birthDate,
            @FormParam("clubRef") Integer clubRef,
            @FormParam("status") String status,
            @FormParam("amountCents") Integer amountCents) {
        Membership membership = membershipDao.find(id);
        if (membership == null) {
            throw new jakarta.ws.rs.NotFoundException("Membership not found");
        }

        membership.setUser(user);
        membership.setNrFfe(nrFfe);
        membership.setLastname(lastname);
        membership.setFirstname(firstname);
        membership.setBirthDate(birthDate);
        membership.setClubRef(clubRef == null ? 0 : clubRef);
        membership.setAmountCents(amountCents == null ? 0 : amountCents);

        MembershipStatus parsedStatus = MembershipStatus.PENDING_APPROVAL;
        if (status != null && !status.isBlank()) {
            parsedStatus = MembershipStatus.valueOf(status);
        }
        membership.setStatus(parsedStatus);

        membershipDao.merge(membership);

        URI redirect = uriInfo.getBaseUriBuilder().path("membership").path(id.toString()).path("edit").build();
        return Response.seeOther(redirect).build();
    }

    @GET
    @Path("{id}/mail-preview")
    @Produces(MediaType.TEXT_HTML)
    public Response previewMail(@PathParam("id") Integer id) {
        Membership membership = membershipDao.find(id);
        if (membership == null) {
            throw new jakarta.ws.rs.NotFoundException("Membership not found");
        }

        try {
            BroadcastMail mail = buildConfirmationMail(membership);
            config.applyOrg(mail);
            String html = new MailTemplate().render(mail.getTemplate(), mail);
            return Response.ok(html, MediaType.TEXT_HTML).build();
        } catch (Exception e) {
            logger.error("Failed to render membership mail preview for {}: {}", id, e.getMessage());
            return Response.serverError()
                    .entity("Impossible de générer l'aperçu de l'email")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
    }

    @POST
    @Path("{id}/send-mail")
    public Response sendMailToUser(@PathParam("id") Integer id) {
        Membership membership = membershipDao.find(id);
        if (membership == null) {
            throw new jakarta.ws.rs.NotFoundException("Membership not found");
        }

        String recipient = membership.getUser();
        if (recipient == null || recipient.isBlank()) {
            throw new jakarta.ws.rs.BadRequestException("Membership user email is required");
        }

        BroadcastMail mail = buildConfirmationMail(membership);

        try {
            sendMail.send(mail, recipient, "Votre inscription a bien été prise en compte");
        } catch (Exception e) {
            logger.error("Unable to send membership confirmation email to {}: {}", recipient, e.getMessage());
        }

        URI redirect = uriInfo.getBaseUriBuilder().path("membership").path(id.toString()).path("edit").build();
        return Response.seeOther(redirect).build();
    }

    private BroadcastMail buildConfirmationMail(Membership membership) {
        String recipient = membership.getUser() != null ? membership.getUser().trim() : "";
        String firstname = membership.getFirstname() != null ? membership.getFirstname() : "";
        String lastname = membership.getLastname() != null ? membership.getLastname() : "";
        String fullName = (firstname + " " + lastname).trim();

        String jwt = Jwts.builder()
                .subject(recipient)
                .issuer(fullName.isEmpty() ? recipient : fullName)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30))
                .signWith(config.getKeys())
                .compact();

        String loginUrl = config.getProperties().getProperty("baseurl", "http://localhost:8080")
                + "/login?jwt=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode("/club-register?success=mail", StandardCharsets.UTF_8);

        List<String> optionValues = membershipOptionSubscriptionDao.all().stream()
                .filter(sub -> sub.getMembership() != null
                        && sub.getMembership().getId() != null
                        && sub.getMembership().getId().equals(membership.getId()))
                .map(sub -> sub.getMembershipOption() != null ? sub.getMembershipOption().getOptionValue() : null)
                .filter(optionValue -> optionValue != null && !optionValue.isBlank())
                .distinct()
                .toList();

        String optionsText = optionValues.isEmpty()
                ? "<i>Aucune option sélectionnée</i>"
                : "<i><b>" + String.join("<br>", optionValues) + "</b></i>";

        String orgName = config.getPage().getOrgName() != null ? config.getPage().getOrgName() : "TicketChess";
        String greetingName = firstname.isBlank() ? "" : firstname;
        boolean needsConfirmation = membership.getStatus() == MembershipStatus.PENDING_CONFIRMATION;

        StringBuilder body = new StringBuilder();
        body.append("Bonjour").append(greetingName.isBlank() ? "" : " " + greetingName)
                .append(",<br><br>Votre inscription à la saison 2026/2027 a bien été prise en compte :")
                .append("<br><br>").append(optionsText);
        if (needsConfirmation) {
            body.append("<br><br>Merci de confirmer votre adhésion en cliquant sur le bouton ci-dessous.");
        }
        body.append("<br><br>Cordialement,<br>").append(orgName);

        BroadcastMail mail = new BroadcastMail();
        mail.setEventName("Inscription 2026/2027");
        mail.setName(fullName);
        mail.setBody(body.toString());
        mail.setLoginUrl(loginUrl);
        mail.setHeaderBackgroundColor("#0f766e");
        mail.setHeaderTextColor("#ffffff");
        mail.setHeaderIcon("✨");
        mail.setButtonText(needsConfirmation ? "Confirmer mon inscription" : "Accéder au site");
        return mail;
    }

    @POST
    @Path("{id}/option")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addOption(
            @PathParam("id") Integer membershipId,
            @FormParam("optionId") Integer optionId) {
        Membership membership = membershipDao.find(membershipId);
        if (membership == null) {
            throw new jakarta.ws.rs.NotFoundException("Membership not found");
        }

        MembershipOption option = membershipOptionDao.find(optionId);
        if (option == null) {
            throw new jakarta.ws.rs.NotFoundException("Option not found");
        }

        MembershipOptionSubscription subscription = new MembershipOptionSubscription();
        subscription.setMembership(membership);
        subscription.setMembershipOption(option);
        subscription.setNrFfe(membership.getNrFfe());

        membershipOptionSubscriptionDao.persist(subscription);

        URI redirect = uriInfo.getBaseUriBuilder().path("membership").path(membershipId.toString()).path("edit").build();
        return Response.seeOther(redirect).build();
    }

    @GET
    @Path("{id}/option/{optionId}/remove")
    public Response removeOption(
            @PathParam("id") Integer membershipId,
            @PathParam("optionId") Integer optionId) {
        MembershipOptionSubscription subscription = membershipOptionSubscriptionDao.find(optionId);
        if (subscription == null) {
            throw new jakarta.ws.rs.NotFoundException("Subscription not found");
        }

        if (!subscription.getMembership().getId().equals(membershipId)) {
            throw new jakarta.ws.rs.BadRequestException("Subscription does not belong to this membership");
        }

        membershipOptionSubscriptionDao.remove(optionId);

        URI redirect = uriInfo.getBaseUriBuilder().path("membership").path(membershipId.toString()).path("edit").build();
        return Response.seeOther(redirect).build();
    }

    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}



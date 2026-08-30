package com.github.gcolin.membership;

import com.github.gcolin.auth.RequireRole;
import com.github.gcolin.club.ClubSeason;
import com.github.gcolin.club.ClubSeasonDao;
import com.github.gcolin.club.ClubSeasonFilter;
import com.github.gcolin.club.SeasonScope;
import com.github.gcolin.membership.Membership;
import com.github.gcolin.membership.MembershipOption;
import com.github.gcolin.membership.MembershipOptionSubscription;
import com.github.gcolin.membership.MembershipStatus;
import com.github.gcolin.auth.RoleCode;
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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.io.StringWriter;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import com.github.gcolin.platform.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.gcolin.platform.JteHtml;

@RequireRole(RoleCode.TRESORIER)
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

    @Inject
    private ClubSeasonFilter clubSeasonFilter;

    @Inject
    private ClubSeasonDao clubSeasonDao;

    @Inject
    private MembershipReportService membershipReportService;

    @Inject
    private LicenseDao licenseDao;

    @Inject
    private LicensePriceService licensePriceService;

    private static final LocalDate LICENSE_REFERENCE_DATE = LocalDate.of(2026, 9, 30);

    private static final Logger logger = LoggerFactory.getLogger(MembershipApi.class);

    @Context
    UriInfo uriInfo;

    @GET
    public JteHtml list(@QueryParam("seasonId") Integer seasonId) {
        SeasonScope scope = clubSeasonFilter.resolve(seasonId);
        List<Membership> memberships = membershipDao.all(scope);
        memberships.sort(Comparator.comparing(Membership::getId, Comparator.nullsLast(Integer::compareTo)).reversed());

        List<MembershipOptionSubscription> allSubscriptions = findSubscriptionsForMemberships(memberships);
        Map<String, List<MembershipOptionSubscription>> subscriptionsByMembership = new HashMap<>();
        for (MembershipOptionSubscription sub : allSubscriptions) {
            String membershipId = sub.getMembership().getId().toString();
            subscriptionsByMembership.computeIfAbsent(membershipId, k -> new java.util.ArrayList<>()).add(sub);
        }

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("memberships", memberships);
        model.put("membershipSubscriptions", subscriptionsByMembership);
        clubSeasonFilter.addToModel(model, seasonId);
        return new JteHtml(model, "membership/membership.jte");
    }

    @GET
    @Path("export/csv")
    @Produces("text/csv; charset=UTF-8")
    public Response exportCsv(@QueryParam("seasonId") Integer seasonId) {
        SeasonScope scope = clubSeasonFilter.resolve(seasonId);
        List<Membership> memberships = new java.util.ArrayList<>(membershipDao.all(scope));
        memberships.sort(Comparator.comparing(Membership::getId, Comparator.nullsLast(Integer::compareTo)).reversed());
        Map<Integer, List<String>> optionsByMembership = buildOptionsByMembership(memberships);

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
    @Path("export/pdf")
    @Produces("application/pdf")
    public Response exportPdf(@QueryParam("seasonId") Integer seasonId) {
        SeasonScope scope = clubSeasonFilter.resolve(seasonId);
        List<Membership> memberships = membershipDao.all(scope);
        Map<Integer, List<String>> optionsByMembership = buildOptionsByMembership(memberships);
        Map<String, MembershipSummaryLine> summaryByName = buildSummaryByName(memberships, optionsByMembership);
        byte[] pdf = membershipReportService.generate(memberships, optionsByMembership, summaryByName, scope);
        return Response.ok(pdf)
                .header(
                        "Content-Disposition",
                        "attachment; filename=adhesions-"
                                + java.time.LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                                + ".pdf")
                .build();
    }

    private List<MembershipOptionSubscription> findSubscriptionsForMemberships(List<Membership> memberships) {
        List<Integer> membershipIds = memberships.stream()
                .map(Membership::getId)
                .filter(id -> id != null)
                .toList();
        if (membershipIds.isEmpty()) {
            return List.of();
        }
        return membershipOptionSubscriptionDao.findByMembershipIds(membershipIds);
    }

    private List<MembershipOptionSubscription> findSubscriptionsForMembershipId(Integer membershipId) {
        if (membershipId == null) {
            return List.of();
        }
        return membershipOptionSubscriptionDao.findByMembershipIds(List.of(membershipId));
    }

    private Map<Integer, List<String>> buildOptionsByMembership(List<Membership> memberships) {
        Map<Integer, List<String>> optionsByMembership = new HashMap<>();
        for (MembershipOptionSubscription sub : findSubscriptionsForMemberships(memberships)) {
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
        return optionsByMembership;
    }

    private Map<String, MembershipSummaryLine> buildSummaryByName(
            List<Membership> memberships, Map<Integer, List<String>> optionsByMembership) {
        Map<String, int[]> totals = new HashMap<>();
        Map<Integer, Integer> optionSumByMembership = buildOptionSumByMembershipId(memberships);
        Map<String, Integer> optionAmountCentsByName = buildOptionAmountCentsByName(memberships, false);
        Map<String, Integer> optionApprovedAmountCentsByName = buildOptionAmountCentsByName(memberships, true);

        Map<String, List<Membership>> membersByOption = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Membership membership : memberships) {
            List<String> options = membership.getId() == null
                    ? List.of()
                    : optionsByMembership.getOrDefault(membership.getId(), List.of());
            for (String option : options) {
                if (option == null || option.isBlank()) {
                    continue;
                }
                membersByOption.computeIfAbsent(option.trim(), key -> new ArrayList<>()).add(membership);
            }
        }

        for (Map.Entry<String, List<Membership>> entry : membersByOption.entrySet()) {
            String optionName = entry.getKey();
            List<Membership> members = entry.getValue();
            int approvedCount = (int) members.stream()
                    .filter(m -> m.getStatus() == MembershipStatus.APPROVED)
                    .count();
            mergeSummaryLine(
                    totals,
                    optionName,
                    members.size(),
                    approvedCount,
                    optionAmountCentsByName.getOrDefault(optionName, 0),
                    optionApprovedAmountCentsByName.getOrDefault(optionName, 0));
        }

        for (Membership membership : memberships) {
            int optionSum = membership.getId() == null
                    ? 0
                    : optionSumByMembership.getOrDefault(membership.getId(), 0);
            addLicenseToSummary(totals, membership, optionSum);
        }

        LinkedHashMap<String, MembershipSummaryLine> ordered = new LinkedHashMap<>();
        for (License license : licenseDao.all()) {
            String label = formatLicenseLabel(license.getName());
            addSummaryLineIfPresent(ordered, totals, label);
        }
        membersByOption.keySet().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(name -> addSummaryLineIfPresent(ordered, totals, name));
        return ordered;
    }

    private static void mergeSummaryLine(
            Map<String, int[]> totals,
            String label,
            int count,
            int approvedCount,
            int amountCents,
            int approvedAmountCents) {
        totals.compute(
                label,
                (key, existing) -> existing == null
                        ? new int[] {count, approvedCount, amountCents, approvedAmountCents}
                        : new int[] {
                            existing[0] + count,
                            existing[1] + approvedCount,
                            existing[2] + amountCents,
                            existing[3] + approvedAmountCents
                        });
    }

    private static void addSummaryLineIfPresent(
            Map<String, MembershipSummaryLine> ordered, Map<String, int[]> totals, String label) {
        int[] values = totals.get(label);
        if (values != null) {
            ordered.put(label, new MembershipSummaryLine(values[0], values[1], values[2], values[3]));
        }
    }

    private Map<Integer, Integer> buildOptionSumByMembershipId(List<Membership> memberships) {
        Map<Integer, Integer> sums = new HashMap<>();
        for (MembershipOptionSubscription sub : findSubscriptionsForMemberships(memberships)) {
            if (sub.getMembership() == null || sub.getMembership().getId() == null || sub.getMembershipOption() == null) {
                continue;
            }
            int amountCents = sub.getMembershipOption().getAmountCents() == null
                    ? 0
                    : sub.getMembershipOption().getAmountCents();
            sums.merge(sub.getMembership().getId(), amountCents, Integer::sum);
        }
        return sums;
    }

    private void addLicenseToSummary(Map<String, int[]> totals, Membership membership, int optionSum) {
        int licenseCents = membership.getAmountCents() - optionSum;
        String licenseType = resolveLicenseType(membership, licenseCents);
        if (licenseType == null) {
            return;
        }
        String label = formatLicenseLabel(licenseType);
        int amount = licenseCents > 0 ? licenseCents : licenseAmountFromGrid(membership, licenseType);
        int approvedAmount = membership.getStatus() == MembershipStatus.APPROVED ? amount : 0;
        int approvedCount = membership.getStatus() == MembershipStatus.APPROVED ? 1 : 0;
        mergeSummaryLine(totals, label, 1, approvedCount, amount, approvedAmount);
    }

    private String resolveLicenseType(Membership membership, int licenseCents) {
        String storedType = membership.getLicenseType();
        if (storedType != null && !storedType.isBlank()) {
            return storedType.trim().toUpperCase(Locale.ROOT);
        }
        if (licenseCents <= 0) {
            return null;
        }
        String inferredType = inferLicenseTypeFromAmount(membership, licenseCents);
        return inferredType != null ? inferredType : Membership.DEFAULT_LICENSE_TYPE;
    }

    private String inferLicenseTypeFromAmount(Membership membership, int licenseCents) {
        String category = categoryFromBirthDate(membership.getBirthDate());
        if (category == null) {
            return null;
        }
        for (License license : licenseDao.all()) {
            String name = license.getName();
            if (name == null || name.isBlank()) {
                continue;
            }
            Integer price = licensePriceService.getLicensePrice(category, name.charAt(0));
            if (price != null && price == licenseCents) {
                return name.trim().toUpperCase(Locale.ROOT);
            }
        }
        return null;
    }

    private int licenseAmountFromGrid(Membership membership, String licenseType) {
        String category = categoryFromBirthDate(membership.getBirthDate());
        if (category == null || licenseType == null || licenseType.isBlank()) {
            return 0;
        }
        Integer price = licensePriceService.getLicensePrice(category, licenseType.charAt(0));
        return price == null ? 0 : price;
    }

    private static String formatLicenseLabel(String licenseName) {
        return "Licence " + licenseName.trim();
    }

    private static String categoryFromBirthDate(String birthDate) {
        Integer birthYear = extractBirthYear(birthDate);
        if (birthYear == null) {
            return null;
        }
        return ModelUtils.getCategory(LICENSE_REFERENCE_DATE, birthYear, true);
    }

    private static Integer extractBirthYear(String birthDate) {
        if (birthDate == null || birthDate.length() < 4) {
            return null;
        }
        String first4 = birthDate.substring(0, 4);
        for (int i = 0; i < first4.length(); i++) {
            if (!Character.isDigit(first4.charAt(i))) {
                return null;
            }
        }
        return Integer.valueOf(first4);
    }

    private Map<String, Integer> buildOptionAmountCentsByName(List<Membership> memberships, boolean approvedOnly) {
        Set<Integer> approvedIds = null;
        if (approvedOnly) {
            approvedIds = memberships.stream()
                    .filter(m -> m.getStatus() == MembershipStatus.APPROVED)
                    .map(Membership::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }
        Map<String, Integer> amountsByOption = new HashMap<>();
        for (MembershipOptionSubscription sub : findSubscriptionsForMemberships(memberships)) {
            if (sub.getMembership() == null || sub.getMembership().getId() == null || sub.getMembershipOption() == null) {
                continue;
            }
            if (approvedOnly && !approvedIds.contains(sub.getMembership().getId())) {
                continue;
            }
            String optionValue = sub.getMembershipOption().getOptionValue();
            if (optionValue == null || optionValue.isBlank()) {
                continue;
            }
            int amountCents = sub.getMembershipOption().getAmountCents() == null
                    ? 0
                    : sub.getMembershipOption().getAmountCents();
            amountsByOption.merge(optionValue.trim(), amountCents, Integer::sum);
        }
        return amountsByOption;
    }

    @GET
    @Path("new")
    public JteHtml createPage() {
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("membership", new Membership());
        model.put("statuses", MembershipStatus.values());
        model.put("licenses", licenseDao.all());
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
            @FormParam("status") String status,
            @FormParam("amountCents") Integer amountCents,
            @FormParam("licenseType") String licenseType) {
        Membership membership = new Membership();
        membership.setUser(user);
        membership.setNrFfe(nrFfe);
        membership.setLastname(lastname);
        membership.setFirstname(firstname);
        membership.setBirthDate(birthDate);
        membership.setAmountCents(amountCents == null ? 0 : amountCents);
        membership.setLicenseType(Membership.normalizeLicenseType(licenseType));

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

        List<MembershipOptionSubscription> subscriptions = findSubscriptionsForMembershipId(id);

        List<MembershipOption> availableOptions = membershipOptionDao.all();

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("membership", membership);
        model.put("statuses", MembershipStatus.values());
        model.put("membershipOptions", subscriptions);
        model.put("availableOptions", availableOptions);
        model.put("licenses", licenseDao.all());
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
            @FormParam("status") String status,
            @FormParam("amountCents") Integer amountCents,
            @FormParam("licenseType") String licenseType) {
        Membership membership = membershipDao.find(id);
        if (membership == null) {
            throw new jakarta.ws.rs.NotFoundException("Membership not found");
        }

        membership.setUser(user);
        membership.setNrFfe(nrFfe);
        membership.setLastname(lastname);
        membership.setFirstname(firstname);
        membership.setBirthDate(birthDate);
        membership.setAmountCents(amountCents == null ? 0 : amountCents);
        membership.setLicenseType(Membership.normalizeLicenseType(licenseType));

        MembershipStatus parsedStatus = MembershipStatus.PENDING_APPROVAL;
        if (status != null && !status.isBlank()) {
            parsedStatus = MembershipStatus.valueOf(status);
        }
        membership.setStatus(parsedStatus);

        membershipDao.merge(membership);

        URI redirect = uriInfo.getBaseUriBuilder().path("membership").path(id.toString()).path("edit").build();
        return Response.seeOther(redirect).build();
    }

    @POST
    @Path("{id}/delete")
    public Response delete(@PathParam("id") Integer id, @FormParam("seasonId") Integer seasonId) {
        Membership membership = membershipDao.find(id);
        if (membership == null) {
            throw new jakarta.ws.rs.NotFoundException("Membership not found");
        }

        for (MembershipOptionSubscription subscription : findSubscriptionsForMembershipId(id)) {
            membershipOptionSubscriptionDao.remove(subscription.getId());
        }
        membershipDao.remove(id);

        jakarta.ws.rs.core.UriBuilder builder = uriInfo.getBaseUriBuilder().path("membership");
        if (seasonId != null) {
            builder.queryParam("seasonId", seasonId);
        }
        return Response.seeOther(builder.build()).build();
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
                .signWith(config.getKeys(), Config.JWT_ALGORITHM)
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
        String seasonName = currentSeasonName();
        body.append("Bonjour").append(greetingName.isBlank() ? "" : " " + greetingName)
                .append(",<br><br>Votre inscription à la saison ")
                .append(seasonName)
                .append(" a bien été prise en compte :")
                .append("<br><br>").append(optionsText);
        if (needsConfirmation) {
            body.append("<br><br>Merci de confirmer votre adhésion en cliquant sur le bouton ci-dessous.");
        }
        body.append("<br><br>Cordialement,<br>").append(orgName);

        BroadcastMail mail = new BroadcastMail();
        mail.setEventName("Inscription " + seasonName);
        mail.setName(fullName);
        mail.setBody(body.toString());
        mail.setLoginUrl(loginUrl);
        mail.setHeaderBackgroundColor("#0f766e");
        mail.setHeaderTextColor("#ffffff");
        mail.setHeaderIcon("✨");
        mail.setButtonText(needsConfirmation ? "Confirmer mon inscription" : "Accéder au site");
        return mail;
    }

    private void recalculateMembershipAmount(Membership membership) {
        if (membership == null || membership.getId() == null) {
            return;
        }
        int optionSum = findSubscriptionsForMembershipId(membership.getId()).stream()
                .map(MembershipOptionSubscription::getMembershipOption)
                .filter(Objects::nonNull)
                .mapToInt(option -> option.getAmountCents() == null ? 0 : option.getAmountCents())
                .sum();
        String licenseType = membership.getLicenseType();
        if (licenseType == null || licenseType.isBlank()) {
            licenseType = Membership.DEFAULT_LICENSE_TYPE;
        }
        int licenseCents = licenseAmountFromGrid(membership, licenseType.trim().toUpperCase(Locale.ROOT));
        membership.setAmountCents(Math.max(0, licenseCents + optionSum));
        membershipDao.merge(membership);
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

        recalculateMembershipAmount(membership);

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

        recalculateMembershipAmount(subscription.getMembership());

        URI redirect = uriInfo.getBaseUriBuilder().path("membership").path(membershipId.toString()).path("edit").build();
        return Response.seeOther(redirect).build();
    }

    private String currentSeasonName() {
        ClubSeason current = clubSeasonDao.findCurrent();
        return current != null && current.getName() != null ? current.getName() : "";
    }

    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}



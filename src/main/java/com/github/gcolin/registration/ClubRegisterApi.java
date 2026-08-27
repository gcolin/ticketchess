package com.github.gcolin.registration;

import com.github.gcolin.auth.LoggedOnly;
import com.github.gcolin.membership.Membership;
import com.github.gcolin.membership.MembershipOption;
import com.github.gcolin.membership.MembershipOptionAccessRule;
import com.github.gcolin.membership.MembershipOptionSubscription;
import com.github.gcolin.membership.MembershipStatus;
import com.github.gcolin.platform.Transactional;
import com.github.gcolin.membership.License;
import com.github.gcolin.player.LuceneDb;
import com.github.gcolin.player.IPlayer;
import com.github.gcolin.player.Player;
import com.github.gcolin.platform.ModelUtils;
import com.github.gcolin.player.Find;
import com.github.gcolin.auth.LoggedUser;
import com.github.gcolin.membership.LicensePriceService;
import com.github.gcolin.membership.LicenseDao;
import com.github.gcolin.membership.MembershipDao;
import com.github.gcolin.membership.MembershipOptionDao;
import com.github.gcolin.membership.MembershipOptionSubscriptionDao;
import com.github.gcolin.platform.BroadcastMail;
import com.github.gcolin.platform.SendMail;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.lucene.queryparser.classic.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.gcolin.platform.JteHtml;

@Path("club-register")
public class ClubRegisterApi {

    private static final LocalDate LICENSE_REFERENCE_DATE = LocalDate.of(2026, 9, 30);
    private static final Logger logger = LoggerFactory.getLogger(ClubRegisterApi.class);

    @Inject
    private LuceneDb luceneDb;

    @Inject
    private LoggedUser loggedUser;

    @Inject
    private Find find;

    @Inject
    private MembershipOptionDao membershipOptionDao;

    @Inject
    private MembershipDao membershipDao;

    @Inject
    private MembershipOptionSubscriptionDao membershipOptionSubscriptionDao;

    @Inject
    private LicenseDao licenseDao;

    @Inject
    private LicensePriceService licensePriceService;

    @Inject
    private SendMail sendMail;

    @Inject
    private Properties properties;

    @Context
    UriInfo uriInfo;

    @GET
    public JteHtml page(@QueryParam("query") String query, @QueryParam("success") String success) {
        Map<String, Object> model = new HashMap<>();
        model.put("query", query);
        model.put("success", success);

        boolean isLogged = loggedUser != null && loggedUser.getEmail() != null;
        if (isLogged) {
            List<Membership> memberships = membershipDao.findByUser(loggedUser.getEmail());
            model.put("memberships", memberships);
            List<Integer> membershipIds = memberships.stream().map(Membership::getId).collect(Collectors.toList());
            Map<String, List<MembershipOptionSubscription>> subscriptionsByMembership =
                membershipOptionSubscriptionDao.findByMembershipIds(membershipIds).stream()
                    .collect(Collectors.groupingBy(s -> s.getMembership().getId().toString()));
            model.put("membershipSubscriptions", subscriptionsByMembership);
        }
        if (isLogged && query != null) {
            if (query.isBlank()) {
                model.put("players", Collections.emptyList());
            } else {
                List<Player> players;
                try {
                    players = luceneDb.searchJoueur(query, 20, null);
                } catch (ParseException | IOException e) {
                    throw new WebApplicationException(e);
                }
                model.put("players", players);
            }
        }

        return new JteHtml(model, "registration/clubRegister.jte");
    }

    @POST
    @Path("select-player")
    @LoggedOnly
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response selectPlayer(@FormParam("nrffe") String nrffe) {
        return redirectToMembershipOptions(nrffe, null, null, null, null, null);
    }

    @GET
    @Path("membership-options")
    @LoggedOnly
    public JteHtml membershipOptionsPage(
            @QueryParam("nrffe") String nrffe,
            @QueryParam("error") String error,
            @QueryParam("licenseType") String selectedLicenseType,
            @QueryParam("lastname") String lastname,
            @QueryParam("firstname") String firstname,
            @QueryParam("birthdate") String birthdate) {
        IPlayer player = null;
        boolean manualPlayer = nrffe == null || nrffe.isBlank();
        if (!manualPlayer) {
            player = find.player(nrffe, null);
            if (player == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
        } else if (isBlank(lastname) || isBlank(firstname) || isBlank(birthdate)) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        boolean young = manualPlayer ? ModelUtils.isYoung(calculateCategory(birthdate)) : player.isYoung();

        List<MembershipOption> options = membershipOptionDao.all();
        options.sort(Comparator.comparing(MembershipOption::getOptionType).thenComparing(MembershipOption::getOptionValue));

        // Build licenses list with prices (filtered by access rule)
        String category = manualPlayer ? calculateCategory(birthdate) : player.getCategory();
        List<License> allLicenses = licenseDao.all();
        allLicenses.sort(Comparator.comparing(License::getName));
        
        List<Map<String, Object>> licensesWithPrices = new ArrayList<>();
        for (License license : allLicenses) {
            // Filter licenses by access rule
            if (!isLicenseAccessible(license.getAccessRule(), young)) {
                continue;
            }
            Integer price = licensePriceService.getLicensePrice(category, license.getName().charAt(0));
            Map<String, Object> licenseData = new HashMap<>();
            licenseData.put("name", license.getName());
            licenseData.put("id", license.getId());
            licenseData.put("priceCents", price != null ? price : 0);
            licensesWithPrices.add(licenseData);
        }

        // Build options with license ID for filtering
        List<Map<String, Object>> optionsWithLicenses = new ArrayList<>();
        for (MembershipOption option : options) {
            if (!isOptionAccessible(option, young)) {
                continue;
            }
            Map<String, Object> optionData = new HashMap<>();
            optionData.put("id", option.getId());
            optionData.put("optionType", option.getOptionType());
            optionData.put("optionValue", option.getOptionValue());
            optionData.put("amountCents", option.getAmountCents());
            optionData.put("accessRule", option.getAccessRule());
            // If option.license is null, it applies to all licenses (use 0 as marker)
            optionData.put("licenseId", option.getLicense() != null ? option.getLicense().getId() : 0);
            optionsWithLicenses.add(optionData);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("nrffe", nrffe == null ? "" : nrffe);
        model.put("player", player);
        model.put("manualPlayer", manualPlayer);
        model.put("lastname", lastname);
        model.put("firstname", firstname);
        model.put("birthdate", birthdate);
        model.put("options", optionsWithLicenses);
        model.put("playerYoung", young);
        model.put("error", error);
        model.put("selectedLicenseType", selectedLicenseType);
        model.put("licenses", licensesWithPrices);
        if (manualPlayer) {
            model.put("category", calculateCategory(birthdate));
        }
        return new JteHtml(model, "registration/clubMembershipOptions.jte");
    }

    @POST
    @Path("membership-options")
    @LoggedOnly
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response saveMembershipOptions(
            @FormParam("nrffe") String nrffe,
            @FormParam("licenseType") String licenseType,
            @FormParam("lastname") String lastname,
            @FormParam("firstname") String firstname,
            @FormParam("birthdate") String birthdate,
            @FormParam("optionIds") List<String> optionIds) {
        boolean manualPlayer = nrffe == null || nrffe.isBlank();
        if (manualPlayer && (isBlank(lastname) || isBlank(firstname) || isBlank(birthdate))) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        List<Integer> ids = new ArrayList<>();
        if (optionIds != null) {
            for (String optionId : optionIds) {
                if (optionId != null && !optionId.isBlank()) {
                    ids.add(Integer.valueOf(optionId));
                }
            }
        }

        String membershipRef = manualPlayer ? "" : nrffe;

        Membership membership = new Membership();
        membership.setUser(loggedUser.getEmail());
        membership.setNrFfe(membershipRef);
        membership.setLastname(lastname);
        membership.setFirstname(firstname);
        membership.setBirthDate(birthdate);
        membership.setClubRef(0);
        membership.setStatus(MembershipStatus.PENDING_APPROVAL);

        int totalAmountCents = 0;
        if (manualPlayer) {
            Integer licensePrice = calculateLicensePriceCents(birthdate, licenseType.charAt(0));
            totalAmountCents += licensePrice == null ? 0 : licensePrice;
        } else {
            IPlayer player = find.player(nrffe, null);
            if (player != null) {
                membership.setLastname(player.getName());
                membership.setFirstname(player.getFirstname());
                membership.setBirthDate(player.getBirthDate());
                Integer licensePrice = calculateLicensePriceCents(player, licenseType.charAt(0));
                totalAmountCents += licensePrice == null ? 0 : licensePrice;
            }
        }

        boolean young = manualPlayer
                ? ModelUtils.isYoung(calculateCategory(birthdate))
                : find.player(nrffe, null) != null && find.player(nrffe, null).isYoung();

        List<MembershipOption> selectedOptions = new ArrayList<>();
        for (Integer id : ids) {
            MembershipOption option = membershipOptionDao.find(id);
            if (option != null && isOptionAccessible(option, young)) {
                selectedOptions.add(option);
                totalAmountCents += option.getAmountCents() == null ? 0 : option.getAmountCents();
            }
        }
        // Persist an explicit computed amount (license + selected options), never negative.
        membership.setAmountCents(Math.max(0, totalAmountCents));
        membershipDao.persist(membership);

        for (MembershipOption option : selectedOptions) {
            MembershipOptionSubscription sub = new MembershipOptionSubscription();
            sub.setMembership(membership);
            sub.setMembershipOption(option);
            sub.setNrFfe(membershipRef);
            membershipOptionSubscriptionDao.persist(sub);
        }

        notifyMembershipSubmission(membership, licenseType, selectedOptions);

        URI redirect = uriInfo.getBaseUriBuilder()
                .path("club-register")
                .queryParam("success", "membership")
                .build();
        return Response.seeOther(redirect).build();
    }

    @POST
    @Path("{id}/confirm")
    @LoggedOnly
    @Transactional
    public Response confirmMembership(@PathParam("id") Integer id) {
        Membership membership = membershipDao.find(id);
        if (membership == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        if (!loggedUser.getEmail().equals(membership.getUser())) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        if (membership.getStatus() == MembershipStatus.PENDING_CONFIRMATION) {
            membership.setStatus(MembershipStatus.APPROVED);
            membershipDao.merge(membership);
        }

        URI redirect = uriInfo.getBaseUriBuilder()
                .path("club-register")
                .queryParam("success", "confirmation")
                .build();
        return Response.seeOther(redirect).build();
    }

    private Response redirectToMembershipOptions(
            String nrffe,
            String error,
            String licenseType,
            String lastname,
            String firstname,
            String birthdate) {
        if ((nrffe == null || nrffe.isBlank()) && (isBlank(lastname) || isBlank(firstname) || isBlank(birthdate))) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        var uriBuilder = uriInfo.getBaseUriBuilder()
                .path("club-register")
                .path("membership-options");
        if (!isBlank(nrffe)) {
            uriBuilder.queryParam("nrffe", nrffe);
        } else {
            uriBuilder.queryParam("lastname", lastname)
                    .queryParam("firstname", firstname)
                    .queryParam("birthdate", birthdate);
        }
        if (error != null && !error.isBlank()) {
            uriBuilder.queryParam("error", error);
        }
        if (licenseType != null && !licenseType.isBlank()) {
            uriBuilder.queryParam("licenseType", licenseType);
        }
        return Response.seeOther(uriBuilder.build()).build();
    }

    private Integer calculateLicensePriceCents(String birthDate, char licenseType) {
        Integer birthYear = extractBirthYear(birthDate);
        if (birthYear == null) {
            return null;
        }
        String category = calculateCategory(birthYear);
        Integer licensePrice = licensePriceService.getLicensePrice(category, licenseType);
        if (licensePrice == null) {
            return null;
        }
        return licensePrice;
    }

    private Integer calculateLicensePriceCents(IPlayer player, char licenseType) {
        Integer birthYear = extractBirthYear(player.getBirthDate());
        if (birthYear == null) {
            return null;
        }

        String category = calculateCategory(birthYear);
        Integer licensePrice = licensePriceService.getLicensePrice(category, licenseType);
        if (licensePrice == null) {
            return null;
        }
        return licensePrice;
    }

    private String calculateCategory(String birthDate) {
        Integer birthYear = extractBirthYear(birthDate);
        if (birthYear == null) {
            return null;
        }
        return calculateCategory(birthYear);
    }

    private String calculateCategory(Integer birthYear) {
        return ModelUtils.getCategory(LICENSE_REFERENCE_DATE, birthYear, true);
    }

    private Integer extractBirthYear(String birthDate) {
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

    private boolean isOptionAccessible(MembershipOption option, boolean young) {
        MembershipOptionAccessRule accessRule = option.getAccessRule();
        if (accessRule == null || accessRule == MembershipOptionAccessRule.ALL) {
            return true;
        }
        if (accessRule == MembershipOptionAccessRule.YOUNG_ONLY) {
            return young;
        }
        if (accessRule == MembershipOptionAccessRule.NON_YOUNG_ONLY) {
            return !young;
        }
        return false;
    }

    private boolean isLicenseAccessible(MembershipOptionAccessRule accessRule, boolean young) {
        if (accessRule == null || accessRule == MembershipOptionAccessRule.ALL) {
            return true;
        }
        if (accessRule == MembershipOptionAccessRule.YOUNG_ONLY) {
            return young;
        }
        if (accessRule == MembershipOptionAccessRule.NON_YOUNG_ONLY) {
            return !young;
        }
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void notifyMembershipSubmission(Membership membership, String licenseType, List<MembershipOption> selectedOptions) {
        List<String> recipients = getMembershipNotificationEmails();
        if (recipients.isEmpty()) {
            return;
        }

        BroadcastMail mail = new BroadcastMail();
        mail.setEventName("Nouvelle demande adhesion club");
        mail.setName(buildMembershipName(membership));
        mail.setBody(buildMembershipNotificationBody(membership, licenseType, selectedOptions));

        try {
            sendMail.sendBcc(mail, recipients, "Nouvelle demande adhesion club");
        } catch (Exception e) {
            logger.error(
                    "Failed to send membership notification mail for membership {}: {}",
                    membership.getId(),
                    e.getMessage());
        }
    }

    private List<String> getMembershipNotificationEmails() {
        String raw = properties.getProperty("membership.notif.emails", "");
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }

        Set<String> recipients = new LinkedHashSet<>();
        for (String value : raw.split(",")) {
            String email = value.trim();
            if (!email.isEmpty()) {
                recipients.add(email);
            }
        }
        return new ArrayList<>(recipients);
    }

    private String buildMembershipNotificationBody(
            Membership membership, String licenseType, List<MembershipOption> selectedOptions) {
        StringBuilder body = new StringBuilder();
        body.append("<p>Une nouvelle demande d'adhesion club a ete enregistree.</p>");
        body.append("<p>");
        body.append("Utilisateur: ").append(escapeHtml(membership.getUser())).append("<br/>");
        body.append("Joueur: ").append(escapeHtml(buildMembershipName(membership))).append("<br/>");
        body.append("Date de naissance: ").append(escapeHtml(membership.getBirthDate())).append("<br/>");
        body.append("Nr FFE: ").append(escapeHtml(membership.getNrFfe())).append("<br/>");
        body.append("Type licence: ").append(escapeHtml(licenseType)).append("<br/>");
        body.append("Montant total: ").append(formatEuros(membership.getAmountCents())).append(" EUR");
        body.append("</p>");

        body.append("<p>Options choisies:</p>");
        if (selectedOptions == null || selectedOptions.isEmpty()) {
            body.append("<p>Aucune option supplementaire.</p>");
            return body.toString();
        }

        body.append("<ul>");
        for (MembershipOption option : selectedOptions) {
            body.append("<li>")
                    .append(escapeHtml(option.getOptionType() + " - " + option.getOptionValue()))
                    .append(" (")
                    .append(formatEuros(option.getAmountCents()))
                    .append(" EUR)")
                    .append("</li>");
        }
        body.append("</ul>");
        return body.toString();
    }

    private String buildMembershipName(Membership membership) {
        String firstname = membership.getFirstname() == null ? "" : membership.getFirstname().trim();
        String lastname = membership.getLastname() == null ? "" : membership.getLastname().trim();
        String combined = (firstname + " " + lastname).trim();
        return combined.isEmpty() ? "(inconnu)" : combined;
    }

    private String formatEuros(Integer amountCents) {
        int cents = amountCents == null ? 0 : amountCents;
        return String.format(java.util.Locale.ROOT, "%.2f", cents / 100d);
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

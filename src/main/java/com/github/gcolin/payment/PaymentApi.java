package com.github.gcolin.payment;

import com.github.gcolin.auth.LoggedOnly;
import com.github.gcolin.auth.RequireRole;
import com.github.gcolin.club.ClubSeasonDao;
import com.github.gcolin.club.ClubSeasonFilter;
import com.github.gcolin.club.SeasonScope;
import com.github.gcolin.event.EventPaymentsReportService;
import com.github.gcolin.membership.Membership;
import com.github.gcolin.membership.MembershipDao;
import com.github.gcolin.membership.MembershipDisplay;
import com.github.gcolin.membership.MembershipOptionSubscription;
import com.github.gcolin.membership.MembershipOptionSubscriptionDao;
import com.github.gcolin.membership.MembershipStatus;
import com.github.gcolin.payment.Payment;
import com.github.gcolin.payment.PaymentStatus;
import com.github.gcolin.payment.PaymentType;
import com.github.gcolin.auth.RoleCode;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.registration.PlayerSubscriptionOption;
import com.github.gcolin.platform.Transactional;
import com.github.gcolin.player.IPlayer;
import com.github.gcolin.platform.PagedList;
import com.github.gcolin.registration.SubscriptionDisplay;
import com.github.gcolin.registration.SubscriptionOptionDisplay;
import com.github.gcolin.payment.DebtService;
import com.github.gcolin.player.Find;
import com.github.gcolin.auth.LoggedUser;
import com.github.gcolin.platform.Redirects;
import com.github.gcolin.platform.SendMail;
import com.github.gcolin.platform.ServiceUtils;
import com.github.gcolin.platform.Config;
import com.github.gcolin.platform.MailAttachment;
import com.github.gcolin.payment.PaymentDao;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import com.github.gcolin.registration.PlayerSubscriptionOptionDao;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.io.StringWriter;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.gcolin.platform.JteHtml;
import com.github.gcolin.player.Player;

@Path("payment")
public class PaymentApi {

    @Inject
    private LoggedUser loggerUser;

    @Inject
    private DebtService debtService;

    @Inject
    private Find find;

    private static final Logger logger = LoggerFactory.getLogger(PaymentApi.class);

    @Context
    UriInfo uriInfo;

    @Inject
    private PaymentDao paymentService;

    @Inject
    private PlayerSubscriptionDao playerSubscriptionService;

    @Inject
    private PlayerSubscriptionOptionDao playerSubscriptionOptionService;

    @Inject
    private Properties properties;

    @Inject
    private ClubSeasonFilter clubSeasonFilter;

    @Inject
    private ClubSeasonDao clubSeasonDao;

    @Inject
    private MembershipDao membershipDao;

    @Inject
    private MembershipOptionSubscriptionDao membershipOptionSubscriptionDao;

    @Inject
    private EventPaymentsReportService eventPaymentsReportService;

    @Inject
    private PaymentReceiptPdfService paymentReceiptPdfService;

    @Inject
    private SendMail sendMail;

    @GET
    @RequireRole(RoleCode.TRESORIER)
    public JteHtml page(
            @QueryParam("page") @DefaultValue("0") @Min(0) Integer page,
            @QueryParam("size") @DefaultValue("25") @Max(100) @Min(1) Integer size,
            @QueryParam("search") @DefaultValue("") String search,
            @QueryParam("seasonId") Integer seasonId) {
        int start = page * size;
        SeasonScope scope = clubSeasonFilter.resolve(seasonId);

        PagedList<Payment> paged = search.isBlank()
                ? paymentService.page(start, size, scope)
                : paymentService.pageSearch(search, start, size, scope);
        long totalItems = paged.getTotal();
        int totalPages = (int) Math.max(1, (totalItems + size - 1) / size);
        page++;
        if (page > totalPages) {
            page = totalPages;
        }

        Map<String, String> paymentSearchLicences = new HashMap<>();
        Map<String, String> paymentSearchNames = new HashMap<>();
        for (Payment payment : paged.getElements()) {
            List<PlayerSubscription> subs =
                    playerSubscriptionService.findByPaymentId(payment.getId().intValue());
            LinkedHashSet<String> licences = new LinkedHashSet<>();
            LinkedHashSet<String> names = new LinkedHashSet<>();
            for (PlayerSubscription sub : subs) {
                if (sub.getNrFfe() != null && !sub.getNrFfe().isBlank()) {
                    licences.add(sub.getNrFfe().trim());
                }
                IPlayer player = find.player(sub.getNrFfe(), null);
                String fullName = buildFullName(player);
                if (!fullName.isBlank()) {
                    names.add(fullName);
                }
            }
            List<Membership> memberships = membershipDao.findByPaymentId(payment.getId().intValue());
            for (Membership membership : memberships) {
                if (membership.getNrFfe() != null && !membership.getNrFfe().isBlank()) {
                    licences.add(membership.getNrFfe().trim());
                }
                String fullName = buildMembershipFullName(membership);
                if (!fullName.isBlank()) {
                    names.add(fullName);
                }
            }
            if (payment.isDonation() && payment.getPayerName() != null && !payment.getPayerName().isBlank()) {
                names.add(payment.getPayerName().trim());
            } else if (payment.getPayerName() != null && !payment.getPayerName().isBlank() && names.isEmpty()) {
                names.add(payment.getPayerName().trim());
            }
            paymentSearchLicences.put(String.valueOf(payment.getId()), String.join(" ", new ArrayList<>(licences)));
            paymentSearchNames.put(String.valueOf(payment.getId()), String.join(" ", new ArrayList<>(names)));
        }

        Map<String, Object> model = new HashMap<>();
        model.put("payments", paged.getElements());
        model.put("paymentSearchLicences", paymentSearchLicences);
        model.put("paymentSearchNames", paymentSearchNames);
        model.put("search", search);
        model.put("currentPage", page);
        model.put("pageSize", size);
        model.put("totalItems", totalItems);
        model.put("totalPages", totalPages);
        model.put("hasPrev", page > 1);
        model.put("hasNext", page < totalPages);
        model.put("prevPage", Math.max(0, page - 2));
        model.put("nextPage", Math.min(totalPages, page));
        clubSeasonFilter.addToModel(model, seasonId);
        return new JteHtml(model, "payment/payments.jte");
    }

    @GET
    @Path("new")
    @RequireRole(RoleCode.TRESORIER)
    public JteHtml newPayment(
            @QueryParam("donation") @DefaultValue("false") boolean donation,
            @QueryParam("membershipId") Integer membershipId) {
        if (membershipId != null && !donation) {
            Membership membership = membershipDao.find(membershipId);
            if (membership == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            if (membership.getPayment() != null && membership.getPayment().getId() != null) {
                return editPayment(membership.getPayment().getId().intValue(), false, "");
            }
        }
        return editPayment(null, donation, "", membershipId);
    }

    @GET
    @Path("{id:\\d+}/edit")
    @RequireRole(RoleCode.TRESORIER)
    public JteHtml editPayment(
            @PathParam("id") Integer id, @QueryParam("success") @DefaultValue("") String success) {
        return editPayment(id, false, success);
    }

    private JteHtml editPayment(Integer id, boolean donationMode) {
        return editPayment(id, donationMode, "");
    }

    private JteHtml editPayment(Integer id, boolean donationMode, String success) {
        return editPayment(id, donationMode, success, null);
    }

    private JteHtml editPayment(Integer id, boolean donationMode, String success, Integer membershipId) {
        Payment payment;
        List<PlayerSubscription> currentSubs = List.of();
        List<PlayerSubscriptionOption> currentOptions = List.of();
        List<Membership> currentMemberships = List.of();
        if (id != null) {
            payment = paymentService.find(id);
            if (payment == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            currentSubs = playerSubscriptionService.findByPaymentId(id);
            currentOptions = playerSubscriptionOptionService.findByPaymentId(id);
            currentMemberships = membershipDao.findByPaymentId(id);
            donationMode = payment.isDonation();
        } else {
            payment = new Payment();
            payment.setStatus(donationMode ? PaymentStatus.PAID : PaymentStatus.PENDING);
            payment.setType(donationMode ? PaymentType.CHEQUE : PaymentType.CARD);
            payment.setAmount(0d);
            payment.setDonation(donationMode);

            if (membershipId != null && !donationMode) {
                Membership membership = membershipDao.find(membershipId);
                if (membership == null) {
                    throw new WebApplicationException(Response.Status.NOT_FOUND);
                }
                payment.setUserEmail(membership.getUser());
                payment.setAmount(ServiceUtils.toEuros(membership.getAmountCents()));
                if (membership.getStatus() == MembershipStatus.PAID) {
                    payment.setStatus(PaymentStatus.PAID);
                    payment.setType(PaymentType.CHEQUE);
                }
                currentMemberships = List.of(membership);
            }
        }

        List<Integer> currentSubscriptionIds =
                currentSubs.stream().map(PlayerSubscription::getId).collect(Collectors.toList());
        List<SubscriptionDisplay> subscriptionDisplays = new ArrayList<>();
        for (PlayerSubscription sub : currentSubs) {
            if (sub.getId() != null && sub.getEvent() != null && sub.getEvent().getId() != null) {
                SubscriptionDisplay display = new SubscriptionDisplay();
                display.setSubscriptionId(sub.getId());
                display.setEditLink("/event/" + sub.getEvent().getId() + "/register/" + sub.getId());
                display.setEventLink("/event/" + sub.getEvent().getId());
                display.setEventName(sub.getEvent().getName());

                String playerName = buildFullName(find.player(sub.getNrFfe(), null));
                if (!playerName.isBlank()) {
                    display.setPlayerName(playerName);
                }

                subscriptionDisplays.add(display);
            }
        }

        List<SubscriptionOptionDisplay> optionDisplays = new ArrayList<>();
        for (PlayerSubscriptionOption option : currentOptions) {
            if (option.getId() == null || option.getPlayerSubscription() == null) {
                continue;
            }
            PlayerSubscription parent = option.getPlayerSubscription();
            SubscriptionOptionDisplay display = new SubscriptionOptionDisplay();
            display.setOptionId(option.getId());
            display.setDescription(option.getDescription());
            display.setAmountCents(option.getAmountCents());
            display.setStatus(option.getStatus() == null ? null : option.getStatus().name());
            if (parent.getEvent() != null && parent.getEvent().getId() != null) {
                display.setEditLink("/event/" + parent.getEvent().getId() + "/register/" + parent.getId() + "/option/"
                        + option.getId());
                display.setEventLink("/event/" + parent.getEvent().getId());
                display.setEventName(parent.getEvent().getName());
            }
            String playerName = buildFullName(find.player(parent.getNrFfe(), null));
            if (!playerName.isBlank()) {
                display.setPlayerName(playerName);
            }
            optionDisplays.add(display);
        }

        List<MembershipDisplay> membershipDisplays = new ArrayList<>();
        for (Membership membership : currentMemberships) {
            if (membership.getId() == null) {
                continue;
            }
            MembershipDisplay display = new MembershipDisplay();
            display.setMembershipId(membership.getId());
            display.setEditLink("/membership/" + membership.getId() + "/edit");
            display.setMemberName(buildMembershipFullName(membership));
            display.setStatus(membership.getStatus() == null ? null : membership.getStatus().name());
            display.setAmountCents(membership.getAmountCents());
            membershipDisplays.add(display);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("payment", payment);
        model.put("donationMode", donationMode);
        model.put("paymentStatuses", PaymentStatus.values());
        model.put(
                "paymentTypes",
                donationMode
                        ? new PaymentType[] {
                            PaymentType.CHEQUE, PaymentType.CASH, PaymentType.BANK_TRANSFER, PaymentType.CARD
                        }
                        : PaymentType.values());
        model.put("selectedSubscriptionIds", currentSubscriptionIds);
        model.put("subscriptionDisplays", subscriptionDisplays);
        model.put("optionDisplays", optionDisplays);
        model.put("membershipDisplays", membershipDisplays);
        model.put("success", success == null ? "" : success);
        return new JteHtml(model, "payment/paymentEdit.jte");
    }

    private String buildFullName(IPlayer player) {
        if (player == null) {
            return "";
        }
        String firstname =
                player.getFirstname() == null ? "" : player.getFirstname().trim();
        String name = player.getName() == null ? "" : player.getName().trim();
        return (firstname + " " + name).trim();
    }

    private String buildMembershipFullName(Membership membership) {
        if (membership == null) {
            return "";
        }
        String firstname = membership.getFirstname() == null ? "" : membership.getFirstname().trim();
        String lastname = membership.getLastname() == null ? "" : membership.getLastname().trim();
        return (firstname + " " + lastname).trim();
    }

    private Long resolveAmountCents(PlayerSubscription subscription) {
        if (subscription.getAmountCents() != null) {
            return subscription.getAmountCents();
        }
        IPlayer player = find.player(subscription.getNrFfe(), null);
        if (player == null || subscription.getEvent() == null) {
            return null;
        }
        return ServiceUtils.calculatePrice(player, subscription.getEvent());
    }

    @POST
    @Path("save")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RequireRole(RoleCode.TRESORIER)
    @Transactional
    public Response save(
            @FormParam("id") Integer id,
            @FormParam("toRemove") @DefaultValue("false") String toRemove,
            @FormParam("userEmail") String userEmail,
            @FormParam("payerName") String payerName,
            @FormParam("payerAddress") String payerAddress,
            @FormParam("donation") @DefaultValue("false") String donationRaw,
            @FormParam("status") String status,
            @FormParam("type") String type,
            @FormParam("amount") Double amount,
            @FormParam("stripeSessionId") String stripeSessionId,
            @FormParam("stripeIntent") String stripeIntent,
            @FormParam("subscriptionIds") List<String> subscriptionIdsRaw,
            @FormParam("optionIds") List<String> optionIdsRaw,
            @FormParam("membershipIds") List<String> membershipIdsRaw) {
        if ("true".equals(toRemove)) {
            if (id != null) {
                List<PlayerSubscription> attached = playerSubscriptionService.findByPaymentId(id);
                for (PlayerSubscription sub : attached) {
                    sub.setPayment(null);
                    playerSubscriptionService.persist(sub);
                }
                List<PlayerSubscriptionOption> attachedOptions = playerSubscriptionOptionService.findByPaymentId(id);
                for (PlayerSubscriptionOption option : attachedOptions) {
                    option.setPayment(null);
                    playerSubscriptionOptionService.merge(option);
                }
                List<Membership> attachedMemberships = membershipDao.findByPaymentId(id);
                for (Membership membership : attachedMemberships) {
                    membership.setPayment(null);
                    membershipDao.merge(membership);
                }
                paymentService.remove(id);
            }
            return Response.seeOther(uriInfo.getBaseUriBuilder().path("payment").build())
                    .build();
        }

        boolean donation = "true".equalsIgnoreCase(donationRaw) || "on".equalsIgnoreCase(donationRaw);
        if (donation) {
            if (payerName == null || payerName.isBlank()) {
                throw new WebApplicationException("payerName is required for donations", Response.Status.BAD_REQUEST);
            }
            if (payerAddress == null || payerAddress.isBlank()) {
                throw new WebApplicationException(
                        "payerAddress is required for donations", Response.Status.BAD_REQUEST);
            }
            if (amount == null || amount <= 0) {
                throw new WebApplicationException(
                        "amount must be greater than 0 for donations", Response.Status.BAD_REQUEST);
            }
            if (hasAnyId(subscriptionIdsRaw) || hasAnyId(optionIdsRaw) || hasAnyId(membershipIdsRaw)) {
                throw new WebApplicationException(
                        "donations cannot be linked to memberships or subscriptions", Response.Status.BAD_REQUEST);
            }
        }

        Payment payment;
        if (id != null) {
            payment = paymentService.find(id);
            if (payment == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            if (payment.isDonation()) {
                donation = true;
            }
        } else {
            payment = new Payment();
        }

        payment.setUserEmail(userEmail);
        payment.setPayerName(payerName == null || payerName.isBlank() ? null : payerName.trim());
        payment.setPayerAddress(payerAddress == null || payerAddress.isBlank() ? null : payerAddress.trim());
        payment.setDonation(donation);
        payment.setStatus(PaymentStatus.valueOf(status));
        payment.setType(PaymentType.valueOf(type));
        payment.setAmount(amount);
        if (stripeSessionId == null || stripeSessionId.isEmpty()) {
            stripeSessionId = null;
        }
        payment.setStripeSessionId(stripeSessionId);
        if (stripeIntent == null || stripeIntent.isEmpty()) {
            stripeIntent = null;
        }
        payment.setStripeIntent(stripeIntent);

        if (id == null) {
            paymentService.persist(payment);
        } else {
            payment = paymentService.merge(payment);
        }

        List<PlayerSubscription> previouslyAttached =
                playerSubscriptionService.findByPaymentId(payment.getId().intValue());
        for (PlayerSubscription sub : previouslyAttached) {
            sub.setPayment(null);
            playerSubscriptionService.persist(sub);
        }

        if (!donation && subscriptionIdsRaw != null) {
            for (String subscriptionIdRaw : subscriptionIdsRaw) {
                if (subscriptionIdRaw == null || subscriptionIdRaw.isBlank()) {
                    continue;
                }
                Integer subscriptionId;
                try {
                    subscriptionId = Integer.valueOf(subscriptionIdRaw.trim());
                } catch (NumberFormatException ex) {
                    throw new WebApplicationException(
                            "subscriptionIds contains an invalid value: " + subscriptionIdRaw,
                            Response.Status.BAD_REQUEST);
                }
                PlayerSubscription selectedSub = playerSubscriptionService.find(subscriptionId);
                if (selectedSub != null) {
                    selectedSub.setPayment(payment);
                    if (selectedSub.getAmountCents() == null) {
                        selectedSub.setAmountCents(resolveAmountCents(selectedSub));
                    }
                    playerSubscriptionService.persist(selectedSub);
                }
            }
        }

        List<PlayerSubscriptionOption> previouslyAttachedOptions =
                playerSubscriptionOptionService.findByPaymentId(payment.getId().intValue());
        for (PlayerSubscriptionOption option : previouslyAttachedOptions) {
            option.setPayment(null);
            playerSubscriptionOptionService.merge(option);
        }

        if (!donation && optionIdsRaw != null) {
            for (String optionIdRaw : optionIdsRaw) {
                if (optionIdRaw == null || optionIdRaw.isBlank()) {
                    continue;
                }
                Integer optionId;
                try {
                    optionId = Integer.valueOf(optionIdRaw.trim());
                } catch (NumberFormatException ex) {
                    throw new WebApplicationException(
                            "optionIds contains an invalid value: " + optionIdRaw, Response.Status.BAD_REQUEST);
                }
                PlayerSubscriptionOption selectedOption = playerSubscriptionOptionService.find(optionId);
                if (selectedOption != null) {
                    selectedOption.setPayment(payment);
                    playerSubscriptionOptionService.merge(selectedOption);
                }
            }
        }

        List<Membership> previouslyAttachedMemberships =
                membershipDao.findByPaymentId(payment.getId().intValue());
        for (Membership membership : previouslyAttachedMemberships) {
            membership.setPayment(null);
            membershipDao.merge(membership);
        }

        if (!donation && membershipIdsRaw != null) {
            for (String membershipIdRaw : membershipIdsRaw) {
                if (membershipIdRaw == null || membershipIdRaw.isBlank()) {
                    continue;
                }
                Integer membershipId;
                try {
                    membershipId = Integer.valueOf(membershipIdRaw.trim());
                } catch (NumberFormatException ex) {
                    throw new WebApplicationException(
                            "membershipIds contains an invalid value: " + membershipIdRaw,
                            Response.Status.BAD_REQUEST);
                }
                Membership selectedMembership = membershipDao.find(membershipId);
                if (selectedMembership != null) {
                    selectedMembership.setPayment(payment);
                    membershipDao.merge(selectedMembership);
                }
            }
        }

        URI redirect = uriInfo.getBaseUriBuilder()
                .path("payment")
                .path(String.valueOf(payment.getId()))
                .path("edit")
                .build();

        return Response.seeOther(redirect).build();
    }

    private static boolean hasAnyId(List<String> ids) {
        if (ids == null) {
            return false;
        }
        for (String id : ids) {
            if (id != null && !id.isBlank()) {
                return true;
            }
        }
        return false;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Path("initiate")
    @LoggedOnly
    @Transactional
    public String initiate() {
        double amount = debtService.calculateEventDebt(loggerUser.getEmail());

        if (amount == 0) {
            throw new WebApplicationException("you have nothing to pay", Response.Status.BAD_GATEWAY);
        }

        if (amount < 5) {
            throw new WebApplicationException("amount too small for card payment", Response.Status.BAD_GATEWAY);
        }

        if (!Config.isStripeCardEnabledForEvents(properties)) {
            throw new WebApplicationException("card payment disabled for events", Response.Status.BAD_GATEWAY);
        }

        if (Config.isStripeSimulated(properties)) {
            throw new WebApplicationException("card payment simulation enabled", Response.Status.BAD_GATEWAY);
        }

        Payment existing = paymentService.findPendingEventPayment(loggerUser.getEmail());
        if (existing != null) {
            try {
                Session session = Session.retrieve(existing.getStripeSessionId(), stripeRequestOptions());
                String status = session.getStatus();
                String paymentStatus = session.getPaymentStatus();

                if ("open".equals(status)) {
                    logger.info("session exists payment {} for {} euros", loggerUser.getEmail(), amount);
                    return ("{\"sessionId\":\"" + existing.getStripeSessionId() + "\"}");
                }

                if ("complete".equals(status) && "paid".equals(paymentStatus)) {
                    debtService.payment(
                            amount, loggerUser.getEmail(), existing.getStripeSessionId(), session.getPaymentIntent());

                    return "{}";
                }

                if ("expired".equals(status) || "complete".equals(status)) {
                    existing.setStatus(PaymentStatus.EXPIRED);
                    paymentService.persist(existing);
                }
            } catch (StripeException ex) {
                logger.info("error while getting previous session " + existing.getStripeSessionId(), ex);
            }
        }

        logger.info("start payment {} for {} euros", loggerUser.getEmail(), amount);

        long amountCents = (long) (amount * 100);

        // 2️⃣ Construire la session Checkout
        SessionCreateParams params = SessionCreateParams.builder()
                // 2.1 Paiement (paiement en ligne)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                // 2.2 Lier la session au produit, vous pouvez changer le prix/description
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("eur")
                                .setUnitAmount(amountCents)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Commande " + loggerUser.getUsername())
                                        .build())
                                .build())
                        .setQuantity(1L)
                        .build())
                // 2.3 URL de succès et d’annulation
                .setSuccessUrl(uriInfo.getBaseUriBuilder()
                                .path("payment")
                                .path("success")
                                .build()
                                .toString() + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(uriInfo.getBaseUriBuilder()
                        .path("event")
                        .path("my")
                        .build()
                        .toString()) // page de retour
                .putMetadata("email", loggerUser.getEmail())
                .putMetadata("scope", "event")
                .setCustomerEmail(loggerUser.getEmail())
                .build();

        Payment payment = debtService.createPayment(loggerUser.getEmail(), amount);

        try {
            String paymentPrefix = properties.getProperty("stripe.keyprefix", "p");
            RequestOptions options = RequestOptions.builder()
                    .setApiKey(properties.getProperty("stripe.secret"))
                    .setIdempotencyKey(paymentPrefix + payment.getId())
                    .build();

            Session session = Session.create(params, options);

            payment.setStripeSessionId(session.getId());
            payment.setStripeIntent(session.getPaymentIntent());
            paymentService.persist(payment);

            return "{\"sessionId\":\"" + session.getId() + "\"}";
        } catch (StripeException e) {
            logger.error(e.getMessage(), e);
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("sim")
    @LoggedOnly
    @Transactional
    public Response sim(@QueryParam("status") String status) {
        if (!Config.isStripeCardEnabledForEvents(properties)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        if (!Config.isStripeSimulated(properties)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        String email = loggerUser.getEmail();
        String simSession = "sim-session-" + email;
        String simIntent = "sim-intent-" + email;
        double amount = debtService.calculateEventDebt(email);
        if (status != null) {
            UriBuilder redirectBuilder =
                    uriInfo.getBaseUriBuilder().path("event").path("my");
            if (status.equals("paid")) {
                Payment existing = paymentService.findPendingEventPayment(email);
                if (existing == null) {
                    existing = debtService.createPayment(email, amount);
                    existing.setStripeIntent(simIntent);
                    existing.setStripeSessionId(simSession);
                    paymentService.persist(existing);
                } else if (existing.getStripeSessionId() != null) {
                    simSession = existing.getStripeSessionId();
                }
                debtService.payment(amount, email, simSession, simIntent);
                redirectBuilder.queryParam("success", "payment");
            }
            return Response.seeOther(redirectBuilder.build()).build();
        }

        Payment existing = paymentService.findPendingEventPayment(email);
        if (existing == null) {
            existing = debtService.createPayment(email, amount);
            existing.setStripeIntent(simIntent);
            existing.setStripeSessionId(simSession);
            paymentService.persist(existing);
        }
        return Response.ok(new JteHtml(
                        Map.of("simulationPath", "/payment/sim"),
                        "payment/paymentStatus.jte"))
                .build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Path("membership/initiate")
    @LoggedOnly
    @Transactional
    public String membershipInitiate() {
        SeasonScope scope = clubSeasonDao.findCurrent() != null
                ? SeasonScope.of(clubSeasonDao.findCurrent())
                : SeasonScope.all();
        double amount = debtService.calculateMembershipDebt(loggerUser.getEmail(), scope);

        if (amount == 0) {
            throw new WebApplicationException("you have nothing to pay", Response.Status.BAD_GATEWAY);
        }

        if (amount < 5) {
            throw new WebApplicationException("amount too small for card payment", Response.Status.BAD_GATEWAY);
        }

        if (!Config.isStripeCardEnabledForMemberships(properties)) {
            throw new WebApplicationException("card payment disabled for memberships", Response.Status.BAD_GATEWAY);
        }

        if (Config.isStripeSimulated(properties)) {
            throw new WebApplicationException("card payment simulation enabled", Response.Status.BAD_GATEWAY);
        }

        Payment existing = paymentService.findPendingMembershipPayment(loggerUser.getEmail());
        if (existing != null) {
            try {
                Session session = Session.retrieve(existing.getStripeSessionId(), stripeRequestOptions());
                String status = session.getStatus();
                String paymentStatus = session.getPaymentStatus();

                if ("open".equals(status)) {
                    logger.info("membership session exists payment {} for {} euros", loggerUser.getEmail(), amount);
                    return ("{\"sessionId\":\"" + existing.getStripeSessionId() + "\"}");
                }

                if ("complete".equals(status) && "paid".equals(paymentStatus)) {
                    debtService.payment(
                            amount, loggerUser.getEmail(), existing.getStripeSessionId(), session.getPaymentIntent());
                    return "{}";
                }

                if ("expired".equals(status) || "complete".equals(status)) {
                    existing.setStatus(PaymentStatus.EXPIRED);
                    paymentService.persist(existing);
                }
            } catch (StripeException ex) {
                logger.info("error while getting previous membership session " + existing.getStripeSessionId(), ex);
            }
        }

        logger.info("start membership payment {} for {} euros", loggerUser.getEmail(), amount);

        long amountCents = (long) (amount * 100);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("eur")
                                .setUnitAmount(amountCents)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Adhesion " + loggerUser.getUsername())
                                        .build())
                                .build())
                        .setQuantity(1L)
                        .build())
                .setSuccessUrl(uriInfo.getBaseUriBuilder()
                                .path("payment")
                                .path("success")
                                .build()
                                .toString() + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(uriInfo.getBaseUriBuilder()
                        .path("club-register")
                        .build()
                        .toString())
                .putMetadata("email", loggerUser.getEmail())
                .putMetadata("scope", "membership")
                .setCustomerEmail(loggerUser.getEmail())
                .build();

        Payment payment = debtService.createMembershipPayment(loggerUser.getEmail(), amount, scope);

        try {
            String paymentPrefix = properties.getProperty("stripe.keyprefix", "p");
            RequestOptions options = RequestOptions.builder()
                    .setApiKey(properties.getProperty("stripe.secret"))
                    .setIdempotencyKey(paymentPrefix + payment.getId())
                    .build();

            Session session = Session.create(params, options);

            payment.setStripeSessionId(session.getId());
            payment.setStripeIntent(session.getPaymentIntent());
            paymentService.persist(payment);

            return "{\"sessionId\":\"" + session.getId() + "\"}";
        } catch (StripeException e) {
            logger.error(e.getMessage(), e);
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("membership/sim")
    @LoggedOnly
    @Transactional
    public Response membershipSim(@QueryParam("status") String status) {
        if (!Config.isStripeCardEnabledForMemberships(properties)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        if (!Config.isStripeSimulated(properties)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        SeasonScope scope = clubSeasonDao.findCurrent() != null
                ? SeasonScope.of(clubSeasonDao.findCurrent())
                : SeasonScope.all();
        String email = loggerUser.getEmail();
        String simSession = "sim-membership-session-" + email;
        String simIntent = "sim-membership-intent-" + email;
        double amount = debtService.calculateMembershipDebt(email, scope);
        if (status != null) {
            UriBuilder redirectBuilder = uriInfo.getBaseUriBuilder().path("club-register");
            if (status.equals("paid")) {
                Payment existing = paymentService.findPendingMembershipPayment(email);
                if (existing == null) {
                    existing = debtService.createMembershipPayment(email, amount, scope);
                    existing.setStripeIntent(simIntent);
                    existing.setStripeSessionId(simSession);
                    paymentService.persist(existing);
                } else if (existing.getStripeSessionId() == null) {
                    existing.setStripeIntent(simIntent);
                    existing.setStripeSessionId(simSession);
                    paymentService.persist(existing);
                } else {
                    simSession = existing.getStripeSessionId();
                }
                debtService.payment(amount, email, simSession, simIntent);
                redirectBuilder.queryParam("success", "payment");
            }
            return Response.seeOther(redirectBuilder.build()).build();
        }

        Payment existing = paymentService.findPendingMembershipPayment(email);
        if (existing == null) {
            existing = debtService.createMembershipPayment(email, amount, scope);
            existing.setStripeIntent(simIntent);
            existing.setStripeSessionId(simSession);
            paymentService.persist(existing);
        }
        return Response.ok(new JteHtml(
                        Map.of("simulationPath", "/payment/membership/sim"),
                        "payment/paymentStatus.jte"))
                .build();
    }

    @GET
    @Path("success")
    @Transactional
    public Response success(@QueryParam("session_id") String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            logger.error("session_id requis");
            throw new WebApplicationException("session_id requis", Response.Status.BAD_REQUEST);
        }

        Session session = null;
        try {
            session = Session.retrieve(sessionId, stripeRequestOptions());
        } catch (StripeException e) {
            logger.error("Erreur Stripe : {}", e.getMessage());
            throw new WebApplicationException("Erreur Stripe : " + e.getMessage());
        }
        String email = session.getMetadata().get("email");
        String scope = session.getMetadata().get("scope");
        if (scope == null || scope.isBlank()) {
            scope = "event";
        }

        logger.info(
                "[email={}] Stripe payment {} euros – status: {} scope: {}",
                email,
                session.getAmountTotal() / 100,
                session.getPaymentStatus(),
                scope);

        UriBuilder redirectBuilder = "membership".equals(scope)
                ? uriInfo.getBaseUriBuilder().path("club-register")
                : uriInfo.getBaseUriBuilder().path("event").path("my");
        if ("paid".equals(session.getPaymentStatus())) {
            double amount = session.getAmountTotal() / 100;
            debtService.payment(amount, email, sessionId, session.getPaymentIntent());
            redirectBuilder.queryParam("success", "payment");
        }

        return Response.seeOther(redirectBuilder.build()).build();
    }

    @GET
    @Path("export/csv")
    @Produces("text/csv")
    @RequireRole(RoleCode.TRESORIER)
    public Response exportCsv(@QueryParam("seasonId") Integer seasonId) {
        SeasonScope scope = clubSeasonFilter.resolve(seasonId);
        List<Payment> payments = paymentService.all(scope);

        StringWriter writer = new StringWriter();

        // Header CSV
        writer.append("id;email;status;type;amount;createdAt;updatedAt;stripeSessionId\n");

        for (Payment p : payments) {
            writer.append(String.valueOf(p.getId())).append(";");
            writer.append(safe(p.getUserEmail())).append(";");
            writer.append(String.valueOf(p.getStatus())).append(";");
            writer.append(String.valueOf(p.getType())).append(";");
            writer.append(p.getAmountCents() == null ? "null" : String.valueOf(ServiceUtils.toEuros(p.getAmountCents())))
                    .append(";");
            writer.append(String.valueOf(p.getCreatedAt())).append(";");
            writer.append(String.valueOf(p.getUpdatedAt())).append(";");
            writer.append(safe(p.getStripeSessionId())).append("\n");
        }

        return Response.ok(writer.toString())
                .header("Content-Disposition", "attachment; filename=payments.csv")
                .build();
    }

    @GET
    @Path("export/csv/details")
    @Produces("text/csv")
    @RequireRole(RoleCode.TRESORIER)
    public Response exportCsvDetails(@QueryParam("seasonId") Integer seasonId) {
        SeasonScope scope = clubSeasonFilter.resolve(seasonId);
        List<Payment> payments = paymentService.all(scope);
        StringWriter writer = new StringWriter();
        writer.append(
                "id;type;status;amount;line_type;event_name;membership_name;license_type;option_labels;player_name;player_license\n");
        for (Payment p : payments) {
            List<PlayerSubscription> subs =
                    playerSubscriptionService.findByPaymentId(p.getId().intValue());
            List<Membership> memberships = membershipDao.findByPaymentId(p.getId().intValue());
            if (subs.isEmpty() && memberships.isEmpty()) {
                writer.append(String.valueOf(p.getId())).append(";");
                writer.append(String.valueOf(p.getType())).append(";");
                writer.append(String.valueOf(p.getStatus())).append(";");
                writer.append(p.getAmountCents() == null ? "null" : String.valueOf(ServiceUtils.toEuros(p.getAmountCents())))
                        .append(";");
                writer.append(p.isDonation() ? "donation" : "").append(";");
                writer.append(";");
                writer.append(p.isDonation() && p.getPayerName() != null ? safe(p.getPayerName()) : "").append(";");
                writer.append(";;;").append("\n");
            } else {
                for (PlayerSubscription sub : subs) {
                    appendCsvDetailLine(writer, p, "event", sub.getEvent() != null ? sub.getEvent().getName() : "",
                            "", "", "", sub);
                }
                for (Membership membership : memberships) {
                    appendCsvMembershipDetailLine(writer, p, membership);
                }
            }
        }

        return Response.ok(writer.toString())
                .header("Content-Disposition", "attachment; filename=payments_details.csv")
                .build();
    }

    @GET
    @Path("export/pdf")
    @Produces("application/pdf")
    @RequireRole(RoleCode.TRESORIER)
    public Response exportAccountingPdf(@QueryParam("seasonId") Integer seasonId) {
        SeasonScope scope = clubSeasonFilter.resolve(seasonId);
        List<Payment> payments = paymentService.findPaid(scope);
        List<EventPaymentsReportService.AccountingDetailRow> rows = buildAccountingDetailRows(payments);
        byte[] pdf = eventPaymentsReportService.generateForAccountingDetails(rows, scope);
        return Response.ok(pdf)
                .header(
                        "Content-Disposition",
                        "attachment; filename=journal-recettes-"
                                + java.time.LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                                + ".pdf")
                .build();
    }

    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @RequireRole(RoleCode.TRESORIER)
    @Path("export")
    public PagedList<Payment> export(
            @QueryParam("page") @DefaultValue("0") @Min(0) Integer page,
            @QueryParam("size") @DefaultValue("50") @Max(100) @Min(1) Integer size,
            @QueryParam("seasonId") Integer seasonId) {
        int start = page * size;
        SeasonScope scope = clubSeasonFilter.resolve(seasonId);
        PagedList<Payment> paged = paymentService.page(start, size, scope);
        paged.setElements(paymentService.detachAll(paged.getElements()));
        return paged;
    }

    @GET
    @Path("{payment_id:\\d+}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @RequireRole(RoleCode.TRESORIER)
    public Payment exportById(@PathParam("payment_id") Integer payment_id) {
        Payment payment = paymentService.find(payment_id);
        return payment;
    }

    @GET
    @Path("{payment_id:\\d+}/sub")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @RequireRole(RoleCode.TRESORIER)
    public List<PlayerSubscription> subById(@PathParam("payment_id") Integer payment_id) {
        return playerSubscriptionService.findByPaymentId(payment_id);
    }

    @GET
    @Path("audit")
    @RequireRole(RoleCode.TRESORIER)
    @Transactional
    public JteHtml audit(@QueryParam("seasonId") Integer seasonId) {
        SeasonScope scope = clubSeasonFilter.resolve(seasonId);
        List<Payment> payments = paymentService.all(scope);
        List<Map<String, Object>> mismatches = new ArrayList<>();
        for (Payment payment : payments) {
            if (payment.getStatus() != com.github.gcolin.payment.PaymentStatus.PAID) {
                continue;
            }
            if (payment.getAmountCents() == null) {
                if (payment.getAmount() != null) {
                    payment.setAmountCents((long) (payment.getAmount() * 100L));
                    paymentService.persist(payment);
                } else {
                    continue;
                }
            }
            List<PlayerSubscription> subs =
                    playerSubscriptionService.findByPaymentId(payment.getId().intValue());
            long sumCents = subs.stream()
                    .filter(s -> s.getAmountCents() != null)
                    .mapToLong(PlayerSubscription::getAmountCents)
                    .sum();
            long expectedCents = 0L;
            for (PlayerSubscription sub : subs) {
                IPlayer player = find.player(sub.getNrFfe(), null);
                if (player != null && sub.getEvent() != null) {
                    expectedCents += ServiceUtils.calculatePrice(player, sub.getEvent());
                } else if (sub.getAmountCents() != null) {
                    expectedCents += sub.getAmountCents();
                }
            }
            long paymentCents = payment.getAmountCents();
            if (expectedCents != paymentCents || sumCents != paymentCents) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("payment", payment);
                entry.put("subs", subs);
                entry.put("sumCents", sumCents);
                entry.put("expectedCents", expectedCents);
                entry.put("paymentCents", paymentCents);
                entry.put("hasMissingAmount", sumCents != paymentCents);
                mismatches.add(entry);
            }
        }
        Map<String, Object> model = new HashMap<>();
        model.put("mismatches", mismatches);
        clubSeasonFilter.addToModel(model, seasonId);
        return new JteHtml(model, "payment/paymentAudit.jte");
    }

    @POST
    @Path("audit/fix/{payment_id:\\d+}")
    @RequireRole(RoleCode.TRESORIER)
    @Transactional
    public Response auditFix(
            @PathParam("payment_id") Integer paymentId, @FormParam("seasonId") Integer seasonId) {
        Payment payment = paymentService.find(paymentId);
        if (payment == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        List<PlayerSubscription> subs = playerSubscriptionService.findByPaymentId(paymentId);
        if (subs.isEmpty()) {
            return Response.seeOther(buildAuditUri(seasonId)).build();
        }
        for (PlayerSubscription sub : subs) {
            IPlayer player = find.player(sub.getNrFfe(), null);
            if (player != null && sub.getEvent() != null) {
                sub.setAmountCents(ServiceUtils.calculatePrice(player, sub.getEvent()));
                playerSubscriptionService.persist(sub);
            }
        }
        return Response.seeOther(buildAuditUri(seasonId)).build();
    }

    private URI buildAuditUri(Integer seasonId) {
        UriBuilder builder = uriInfo.getBaseUriBuilder().path("payment").path("audit");
        if (seasonId != null) {
            builder.queryParam("seasonId", seasonId);
        }
        return builder.build();
    }

    private RequestOptions stripeRequestOptions() {
        return RequestOptions.builder()
                .setApiKey(properties.getProperty("stripe.secret"))
                .build();
    }

    private String safe(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    @GET
    @Path("{id:\\d+}/payer")
    @LoggedOnly
    public JteHtml editPayer(
            @PathParam("id") Integer id,
            @QueryParam("returnTo") String returnTo,
            @QueryParam("success") String success) {
        Payment payment = requireOwnedPaidPayment(id);
        List<PlayerSubscription> subs = playerSubscriptionService.findByPaymentId(id);
        List<Membership> memberships = membershipDao.findByPaymentId(id);

        Map<String, Object> model = new HashMap<>();
        model.put("payment", payment);
        model.put(
                "suggestedPayerName",
                paymentReceiptPdfService.suggestPayerName(
                        payment, subs, memberships, find, loggerUser.getUsername()));
        model.put("returnTo", defaultPayerReturnTo(returnTo, memberships));
        model.put("success", success == null ? "" : success);
        return new JteHtml(model, "payment/paymentPayer.jte");
    }

    @POST
    @Path("{id:\\d+}/payer")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @LoggedOnly
    @Transactional
    public Response savePayer(
            @PathParam("id") Integer id,
            @FormParam("payerName") String payerName,
            @FormParam("returnTo") String returnTo) {
        Payment payment = requireOwnedPaidPayment(id);
        payment.setPayerName(payerName == null || payerName.isBlank() ? null : payerName.trim());
        paymentService.merge(payment);

        List<Membership> memberships = membershipDao.findByPaymentId(id);
        String target = defaultPayerReturnTo(returnTo, memberships);
        URI redirect = Redirects.safeRedirect(
                withSuccessQuery(target, "payerUpdated"),
                uriInfo.getBaseUriBuilder()
                        .path(target.startsWith("/") ? target.substring(1) : target)
                        .queryParam("success", "payerUpdated")
                        .build(),
                uriInfo);
        return Response.seeOther(Redirects.toSameOriginRelative(redirect)).build();
    }

    private static String withSuccessQuery(String path, String success) {
        if (!Redirects.isSafeRelativeRedirect(path)) {
            return path;
        }
        String trimmed = path.trim();
        int hash = trimmed.indexOf('#');
        String beforeHash = hash >= 0 ? trimmed.substring(0, hash) : trimmed;
        String fragment = hash >= 0 ? trimmed.substring(hash) : "";
        if (beforeHash.contains("?")) {
            return beforeHash + "&success=" + success + fragment;
        }
        return beforeHash + "?success=" + success + fragment;
    }

    private Payment requireOwnedPaidPayment(Integer id) {
        Payment payment = paymentService.find(id);
        if (payment == null || payment.getStatus() != PaymentStatus.PAID) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        boolean isOwner = loggerUser.getEmail() != null
                && payment.getUserEmail() != null
                && loggerUser.getEmail().equalsIgnoreCase(payment.getUserEmail());
        if (!isOwner) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        return payment;
    }

    private static String defaultPayerReturnTo(String returnTo, List<Membership> memberships) {
        if (Redirects.isSafeRelativeRedirect(returnTo)) {
            return returnTo.trim();
        }
        if (memberships != null && !memberships.isEmpty()) {
            return "/club-register";
        }
        return "/event/my";
    }

    @GET
    @Path("{id:\\d+}/invoice")
    @Produces("application/pdf")
    @LoggedOnly
    public Response invoice(@PathParam("id") Integer id) {
        Payment payment = paymentService.find(id);
        if (payment == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        boolean isOwner = loggerUser.getEmail() != null
                && payment.getUserEmail() != null
                && loggerUser.getEmail().equalsIgnoreCase(payment.getUserEmail());
        boolean isTreasurer = loggerUser.hasRole(RoleCode.TRESORIER);
        if (!isOwner && !isTreasurer) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        if (payment.getStatus() != com.github.gcolin.payment.PaymentStatus.PAID) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        try {
            GeneratedReceipt receipt = generateReceipt(payment, id);
            return Response.ok(receipt.pdf())
                    .header("Content-Disposition", "attachment; filename=" + receipt.filename())
                    .build();
        } catch (Exception e) {
            logger.error("Error generating invoice PDF for payment " + id, e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("{id:\\d+}/send-receipt")
    @RequireRole(RoleCode.TRESORIER)
    public Response sendReceipt(@PathParam("id") Integer id) {
        Payment payment = paymentService.find(id);
        if (payment == null || payment.getStatus() != PaymentStatus.PAID) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        if (payment.getUserEmail() == null || payment.getUserEmail().isBlank()) {
            throw new WebApplicationException("payment email is required", Response.Status.BAD_REQUEST);
        }

        try {
            GeneratedReceipt receipt = generateReceipt(payment, id);
            PaymentReceiptMail mail = new PaymentReceiptMail();
            mail.setDonation(payment.isDonation());
            mail.setName(resolveReceiptMailName(payment));
            mail.setDocumentLabel(
                    payment.isDonation() ? "Attestation de don" : "Reçu de paiement");
            mail.setReference(String.valueOf(payment.getId()));
            if (payment.getAmountCents() != null) {
                mail.setAmount(String.format(java.util.Locale.FRANCE, "%.2f €", ServiceUtils.toEuros(payment.getAmountCents())));
            } else if (payment.getAmount() != null) {
                mail.setAmount(String.format(java.util.Locale.FRANCE, "%.2f €", payment.getAmount()));
            }
            String subject = payment.isDonation()
                    ? "Votre attestation fiscale"
                    : "Votre reçu de paiement";
            sendMail.send(
                    mail,
                    payment.getUserEmail().trim(),
                    subject,
                    new MailAttachment(receipt.filename(), "application/pdf", receipt.pdf()));
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error sending receipt email for payment " + id, e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }

        URI redirect = uriInfo.getBaseUriBuilder()
                .path("payment")
                .path(String.valueOf(id))
                .path("edit")
                .queryParam("success", "receiptSent")
                .build();
        return Response.seeOther(Redirects.toSameOriginRelative(redirect)).build();
    }

    private GeneratedReceipt generateReceipt(Payment payment, Integer id) throws Exception {
        if (payment.isDonation()) {
            byte[] pdf = paymentReceiptPdfService.generateDonationAttestation(payment);
            return new GeneratedReceipt(pdf, "attestation-fiscale-" + id + ".pdf", true);
        }
        List<PlayerSubscription> subs = playerSubscriptionService.findByPaymentId(id);
        List<Membership> memberships = membershipDao.findByPaymentId(id);
        Map<Integer, List<MembershipOptionSubscription>> optionSubscriptions = memberships.isEmpty()
                ? Map.of()
                : membershipOptionSubscriptionDao
                        .findByMembershipIds(memberships.stream().map(Membership::getId).toList())
                        .stream()
                        .collect(Collectors.groupingBy(s -> s.getMembership().getId()));
        String fallbackName = payment.getPayerName() != null && !payment.getPayerName().isBlank()
                ? payment.getPayerName()
                : (loggerUser.getUsername() != null ? loggerUser.getUsername() : "");
        byte[] pdf = paymentReceiptPdfService.generatePaymentReceipt(
                payment, subs, memberships, optionSubscriptions, find, fallbackName);
        return new GeneratedReceipt(pdf, "facture-" + id + ".pdf", false);
    }

    private String resolveReceiptMailName(Payment payment) {
        if (payment.getPayerName() != null && !payment.getPayerName().isBlank()) {
            return payment.getPayerName().trim();
        }
        return payment.getUserEmail();
    }

    private record GeneratedReceipt(byte[] pdf, String filename, boolean donation) {}

    private void appendCsvDetailLine(
            StringWriter writer,
            Payment p,
            String lineType,
            String eventName,
            String membershipName,
            String licenseType,
            String optionLabels,
            PlayerSubscription sub) {
        writer.append(String.valueOf(p.getId())).append(";");
        writer.append(String.valueOf(p.getType())).append(";");
        writer.append(String.valueOf(p.getStatus())).append(";");
        writer.append(
                        p.getAmountCents() == null
                                ? "null"
                                : String.valueOf(ServiceUtils.toEuros(p.getAmountCents())))
                .append(";");
        writer.append(lineType).append(";");
        writer.append(eventName).append(";");
        writer.append(membershipName).append(";");
        writer.append(licenseType).append(";");
        writer.append(optionLabels).append(";");
        IPlayer player = find.player(sub.getNrFfe(), null);
        if (player != null) {
            writer.append(player.getFullname());
        }
        writer.append(";");
        writer.append(sub.getNrFfe() != null ? sub.getNrFfe() : "").append("\n");
    }

    private void appendCsvMembershipDetailLine(StringWriter writer, Payment p, Membership membership) {
        String membershipName = buildMembershipFullName(membership);
        String licenseType = membership.getLicenseType() != null ? membership.getLicenseType() : "";
        List<MembershipOptionSubscription> subscriptions =
                membershipOptionSubscriptionDao.findByMembershipIds(List.of(membership.getId()));
        String optionLabels = subscriptions.stream()
                .map(MembershipOptionSubscription::getMembershipOption)
                .filter(java.util.Objects::nonNull)
                .map(option -> option.getOptionValue())
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(", "));
        writer.append(String.valueOf(p.getId())).append(";");
        writer.append(String.valueOf(p.getType())).append(";");
        writer.append(String.valueOf(p.getStatus())).append(";");
        writer.append(
                        p.getAmountCents() == null
                                ? "null"
                                : String.valueOf(ServiceUtils.toEuros(p.getAmountCents())))
                .append(";");
        writer.append("membership").append(";;;");
        writer.append(membershipName).append(";");
        writer.append(licenseType).append(";");
        writer.append(optionLabels).append(";");
        writer.append(membershipName).append(";");
        writer.append(membership.getNrFfe() != null ? membership.getNrFfe() : "").append("\n");
    }

    private List<EventPaymentsReportService.AccountingDetailRow> buildAccountingDetailRows(List<Payment> payments) {
        List<EventPaymentsReportService.AccountingDetailRow> rows = new ArrayList<>();
        for (Payment payment : payments) {
            if (payment.getStatus() != PaymentStatus.PAID || payment.getAmountCents() == null) {
                continue;
            }
            LocalDateTime paymentAt =
                    payment.getUpdatedAt() != null ? payment.getUpdatedAt() : payment.getCreatedAt();
            String paymentType = EventPaymentsReportService.resolvePaymentType(payment);
            List<PlayerSubscription> subs =
                    playerSubscriptionService.findByPaymentId(payment.getId().intValue());
            List<Membership> memberships = membershipDao.findByPaymentId(payment.getId().intValue());
            if (payment.isDonation() || (subs.isEmpty() && memberships.isEmpty())) {
                String nature = payment.isDonation() ? "donation" : "";
                String label = payment.isDonation()
                        ? (payment.getPayerName() != null ? payment.getPayerName() : "")
                        : "";
                rows.add(new EventPaymentsReportService.AccountingDetailRow(
                        paymentAt,
                        payment.getId(),
                        paymentType,
                        payment.getUserEmail(),
                        ServiceUtils.toEuros(payment.getAmountCents()),
                        nature,
                        label));
                continue;
            }
            for (PlayerSubscription sub : subs) {
                double lineAmount = sub.getAmountCents() != null
                        ? ServiceUtils.toEuros(sub.getAmountCents())
                        : 0d;
                String label = sub.getEvent() != null ? sub.getEvent().getName() : "";
                rows.add(new EventPaymentsReportService.AccountingDetailRow(
                        paymentAt, payment.getId(), paymentType, payment.getUserEmail(), lineAmount, "event", label));
            }
            for (Membership membership : memberships) {
                double lineAmount = membership.getAmountCents() / 100d;
                rows.add(new EventPaymentsReportService.AccountingDetailRow(
                        paymentAt,
                        payment.getId(),
                        paymentType,
                        payment.getUserEmail(),
                        lineAmount,
                        "membership",
                        buildMembershipFullName(membership)));
            }
        }
        rows.sort(java.util.Comparator.comparing(
                        EventPaymentsReportService.AccountingDetailRow::paymentAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                .thenComparing(
                        EventPaymentsReportService.AccountingDetailRow::id,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));
        return rows;
    }
}

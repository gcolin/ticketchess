package com.github.gcolin.payment;

import com.github.gcolin.auth.LoggedOnly;
import com.github.gcolin.auth.RequireRole;
import com.github.gcolin.club.ClubSeasonFilter;
import com.github.gcolin.club.SeasonScope;
import com.github.gcolin.event.EventPaymentsReportService;
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
import com.github.gcolin.platform.ServiceUtils;
import com.github.gcolin.platform.Config;
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
    private EventPaymentsReportService eventPaymentsReportService;

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
    public JteHtml newPayment() {
        return editPayment(null);
    }

    @GET
    @Path("{id:\\d+}/edit")
    @RequireRole(RoleCode.TRESORIER)
    public JteHtml editPayment(@PathParam("id") Integer id) {
        Payment payment;
        List<PlayerSubscription> currentSubs = List.of();
        List<PlayerSubscriptionOption> currentOptions = List.of();
        if (id != null) {
            payment = paymentService.find(id);
            if (payment == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            currentSubs = playerSubscriptionService.findByPaymentId(id);
            currentOptions = playerSubscriptionOptionService.findByPaymentId(id);
        } else {
            payment = new Payment();
            payment.setStatus(PaymentStatus.PENDING);
            payment.setType(PaymentType.CARD);
            payment.setAmount(0d);
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

        Map<String, Object> model = new HashMap<>();
        model.put("payment", payment);
        model.put("paymentStatuses", PaymentStatus.values());
        model.put("paymentTypes", PaymentType.values());
        model.put("selectedSubscriptionIds", currentSubscriptionIds);
        model.put("subscriptionDisplays", subscriptionDisplays);
        model.put("optionDisplays", optionDisplays);
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
            @FormParam("status") String status,
            @FormParam("type") String type,
            @FormParam("amount") Double amount,
            @FormParam("stripeSessionId") String stripeSessionId,
            @FormParam("stripeIntent") String stripeIntent,
            @FormParam("subscriptionIds") List<String> subscriptionIdsRaw,
            @FormParam("optionIds") List<String> optionIdsRaw) {
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
                paymentService.remove(id);
            }
            return Response.seeOther(uriInfo.getBaseUriBuilder().path("payment").build())
                    .build();
        }

        Payment payment;
        if (id != null) {
            payment = paymentService.find(id);
            if (payment == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
        } else {
            payment = new Payment();
        }

        payment.setUserEmail(userEmail);
        payment.setStatus(PaymentStatus.valueOf(status));
        payment.setType(PaymentType.valueOf(type));
        payment.setAmount(amount);
        if (stripeSessionId.isEmpty()) {
            stripeSessionId = null;
        }
        payment.setStripeSessionId(stripeSessionId);
        if (stripeIntent.isEmpty()) {
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

        if (subscriptionIdsRaw != null) {
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

        if (optionIdsRaw != null) {
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

        URI redirect = uriInfo.getBaseUriBuilder()
                .path("payment")
                .path(String.valueOf(payment.getId()))
                .path("edit")
                .build();

        return Response.seeOther(redirect).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Path("initiate")
    @LoggedOnly
    @Transactional
    public String initiate() {
        double amount = debtService.calculateDebt(loggerUser.getEmail());

        if (amount == 0) {
            throw new WebApplicationException("you have nothing to pay", Response.Status.BAD_GATEWAY);
        }

        if (amount < 5) {
            throw new WebApplicationException("amount too small for card payment", Response.Status.BAD_GATEWAY);
        }

        if (Config.isStripeSimulated(properties)) {
            throw new WebApplicationException("card payment simulation enabled", Response.Status.BAD_GATEWAY);
        }

        Payment existing = paymentService.findPendingByUser(loggerUser.getEmail());
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
        if (!Config.isStripeSimulated(properties)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        String email = loggerUser.getEmail();
        String simSession = "sim-session-" + email;
        String simIntent = "sim-intent-" + email;
        double amount = debtService.calculateDebt(email);
        if (status != null) {
            UriBuilder redirectBuilder =
                    uriInfo.getBaseUriBuilder().path("event").path("my");
            if (status.equals("paid")) {
                debtService.payment(amount, email, simSession, simIntent);
                redirectBuilder.queryParam("success", "payment");
            }
            return Response.seeOther(redirectBuilder.build()).build();
        }

        Payment existing = paymentService.findPendingByUser(email);
        if (existing == null) {
            existing = debtService.createPayment(email, amount);
            existing.setStripeIntent(simIntent);
            existing.setStripeSessionId(simSession);
            paymentService.persist(existing);
        }
        return Response.ok(new JteHtml(Collections.emptyMap(), "payment/paymentStatus.jte"))
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

        logger.info(
                "[email={}] Stripe payment {} euros – status: {}",
                email,
                session.getAmountTotal() / 100,
                session.getPaymentStatus());

        UriBuilder redirectBuilder = uriInfo.getBaseUriBuilder().path("event").path("my");
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
        // Header CSV with detailed subscription info
        writer.append("id;type;status;amount;event_name;player_name;player_license\n");
        for (Payment p : payments) {
            List<PlayerSubscription> subs =
                    playerSubscriptionService.findByPaymentId(p.getId().intValue());
            if (subs.isEmpty()) {
                // No subscription, leave fields empty
                writer.append(String.valueOf(p.getId())).append(";");
                writer.append(String.valueOf(p.getType())).append(";");
                writer.append(String.valueOf(p.getStatus())).append(";");
                writer.append(p.getAmountCents() == null ? "null" : String.valueOf(ServiceUtils.toEuros(p.getAmountCents())))
                        .append(";");
                writer.append(";;;").append("\n");
            } else {
                for (PlayerSubscription sub : subs) {
                    writer.append(String.valueOf(p.getId())).append(";");
                    writer.append(String.valueOf(p.getType())).append(";");
                    writer.append(String.valueOf(p.getStatus())).append(";");
                    writer.append(
                                    p.getAmountCents() == null
                                            ? "null"
                                            : String.valueOf(ServiceUtils.toEuros(p.getAmountCents())))
                            .append(";");
                    writer.append(sub.getEvent() != null ? sub.getEvent().getName() : "")
                            .append(";");
                    // Player first/last names are not stored; placeholders
                    IPlayer player = find.player(sub.getNrFfe(), null);
                    if (player != null) {
                        writer.append(player.getFullname());
                    }
                    writer.append(";");
                    writer.append(sub.getNrFfe() != null ? sub.getNrFfe() : "").append("\n");
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
        byte[] pdf = eventPaymentsReportService.generateForAccounting(paymentService.findPaid(scope), scope);
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

        List<PlayerSubscription> subs = playerSubscriptionService.findByPaymentId(id);

        try {
            byte[] pdf = generateInvoicePdf(payment, subs);
            return Response.ok(pdf)
                    .header("Content-Disposition", "attachment; filename=facture-" + id + ".pdf")
                    .build();
        } catch (Exception e) {
            logger.error("Error generating invoice PDF for payment " + id, e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private byte[] generateInvoicePdf(Payment payment, List<PlayerSubscription> subs) throws Exception {
        org.openpdf.text.Document document =
                new org.openpdf.text.Document(org.openpdf.text.PageSize.A4, 50, 50, 60, 60);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        org.openpdf.text.pdf.PdfWriter.getInstance(document, baos);
        document.open();

        org.openpdf.text.Font titleFont =
                new org.openpdf.text.Font(org.openpdf.text.Font.HELVETICA, 18, org.openpdf.text.Font.BOLD);
        org.openpdf.text.Font headerFont =
                new org.openpdf.text.Font(org.openpdf.text.Font.HELVETICA, 11, org.openpdf.text.Font.BOLD);
        org.openpdf.text.Font normalFont = new org.openpdf.text.Font(org.openpdf.text.Font.HELVETICA, 10);
        org.openpdf.text.Font smallFont = new org.openpdf.text.Font(org.openpdf.text.Font.HELVETICA, 9);

        String sellerName = Config.configured(properties, "invoice.seller.name", "org.name");
        String sellerAddress1 = Config.configured(properties, "invoice.seller.address1", "org.address");
        String sellerAddress2 = Config.configured(properties, "invoice.seller.address2", null);
        String sellerZip = Config.configured(properties, "invoice.seller.zip", null);
        String sellerCity = Config.configured(properties, "invoice.seller.city", null);
        String sellerCountry = Config.configured(properties, "invoice.seller.country", null);
        String sellerEmail = Config.configured(properties, "invoice.seller.email", "org.email");
        String sellerPhone = Config.configured(properties, "invoice.seller.phone", null);
        String sellerWebsite = Config.configured(properties, "invoice.seller.website", "contact.url");
        String sellerSiret = Config.configured(properties, "invoice.seller.siret", null);
        String sellerRna = Config.configured(properties, "invoice.seller.rna", null);
        String sellerPrefecture = Config.configured(properties, "invoice.seller.prefecture", null);

        String invoicePrefix = Config.configured(properties, "invoice.number.prefix", null);
        if (invoicePrefix.isBlank()) {
            invoicePrefix = "FAC-";
        }
        String invoiceNumber = invoicePrefix + payment.getId();

        String paymentMethodLabel = "Mode de paiement";
        String paymentStatusLabel = Config.configured(properties, "invoice.payment.status.label", null);
        if (paymentStatusLabel.isBlank()) {
            paymentStatusLabel = "Payé";
        }
        String vatNotice = Config.configured(properties, "invoice.vat.notice", null);
        String footerContact = Config.configured(properties, "invoice.footer", "org.name");

        document.add(new org.openpdf.text.Paragraph("Reçu n° " + invoiceNumber, titleFont));
        document.add(new org.openpdf.text.Paragraph(" "));

        addLineIfNotBlank(document, normalFont, sellerName);
        addLineIfNotBlank(document, normalFont, sellerAddress1);
        addLineIfNotBlank(document, normalFont, sellerAddress2);
        addLineIfNotBlank(document, normalFont, joinNotBlank(" ", sellerZip, sellerCity));
        addLineIfNotBlank(document, normalFont, sellerCountry);
        addLineIfNotBlank(
                document,
                smallFont,
                joinNotBlank(
                        " | ",
                        isBlank(sellerEmail) ? "" : "Email: " + sellerEmail,
                        isBlank(sellerPhone) ? "" : "Tel: " + sellerPhone,
                        sellerWebsite));
        addLineIfNotBlank(
                document,
                smallFont,
                joinNotBlank(
                        " | ",
                        isBlank(sellerSiret) ? "" : "SIRET: " + sellerSiret,
                        isBlank(sellerRna) ? "" : "RNA: " + sellerRna));
        addLineIfNotBlank(document, smallFont, sellerPrefecture);
        document.add(new org.openpdf.text.Paragraph(" "));

        String client = loggerUser.getUsername();
        if (!subs.isEmpty()) {
            PlayerSubscription sub = subs.get(0);
            IPlayer player = find.player(sub.getNrFfe(), null);
            if (player != null) {
                client = buildFullName(player);
            }
        }

        document.add(new org.openpdf.text.Paragraph("Client : " + client, normalFont));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        document.add(new org.openpdf.text.Paragraph(
                "Date : "
                        + (payment.getUpdatedAt() != null
                                ? payment.getUpdatedAt().toLocalDate().format(formatter)
                                : payment.getCreatedAt() != null
                                        ? payment.getCreatedAt().toLocalDate().format(formatter)
                                        : "-"),
                normalFont));
        document.add(new org.openpdf.text.Paragraph("Email : " + payment.getUserEmail(), normalFont));
        document.add(new org.openpdf.text.Paragraph(
                paymentMethodLabel + " : " + translatePaymentType(payment.getType()), normalFont));
        document.add(new org.openpdf.text.Paragraph("Statut : " + paymentStatusLabel, normalFont));
        document.add(new org.openpdf.text.Paragraph(" "));

        if (!subs.isEmpty()) {
            org.openpdf.text.pdf.PdfPTable table = new org.openpdf.text.pdf.PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[] {40f, 40f, 20f});

            org.openpdf.text.pdf.PdfPCell c1 =
                    new org.openpdf.text.pdf.PdfPCell(new org.openpdf.text.Phrase("Tournoi", headerFont));
            org.openpdf.text.pdf.PdfPCell c2 =
                    new org.openpdf.text.pdf.PdfPCell(new org.openpdf.text.Phrase("Joueur", headerFont));
            org.openpdf.text.pdf.PdfPCell c3 =
                    new org.openpdf.text.pdf.PdfPCell(new org.openpdf.text.Phrase("Montant", headerFont));
            c1.setBackgroundColor(new java.awt.Color(220, 220, 220));
            c2.setBackgroundColor(new java.awt.Color(220, 220, 220));
            c3.setBackgroundColor(new java.awt.Color(220, 220, 220));
            table.addCell(c1);
            table.addCell(c2);
            table.addCell(c3);

            for (PlayerSubscription sub : subs) {
                String eventName = sub.getEvent() != null ? sub.getEvent().getName() : "-";
                String playerInfo = sub.getNrFfe() != null ? sub.getNrFfe() : "-";
                IPlayer player = find.player(sub.getNrFfe(), null);
                if (player != null) {
                    playerInfo = (player.getFirstname() != null ? player.getFirstname() + " " : "")
                            + (player.getName() != null ? player.getName() : "");
                }
                String price = sub.getAmountCents() != null
                        ? String.format("%.2f €", ServiceUtils.toEuros(sub.getAmountCents()))
                        : "-";
                table.addCell(new org.openpdf.text.Phrase(eventName, normalFont));
                table.addCell(new org.openpdf.text.Phrase(playerInfo, normalFont));
                table.addCell(new org.openpdf.text.Phrase(price, normalFont));
            }
            document.add(table);
            document.add(new org.openpdf.text.Paragraph(" "));
        }

        String totalStr = payment.getAmountCents() != null
                ? String.format("%.2f €", ServiceUtils.toEuros(payment.getAmountCents()))
                : (payment.getAmount() != null ? String.format("%.2f €", payment.getAmount()) : "-");
        document.add(new org.openpdf.text.Paragraph("Montant payé : " + totalStr, headerFont));
        addLineIfNotBlank(document, smallFont, vatNotice);
        addLineIfNotBlank(document, smallFont, footerContact);

        document.close();
        return baos.toByteArray();
    }

    private static void addLineIfNotBlank(org.openpdf.text.Document document, org.openpdf.text.Font font, String value)
            throws org.openpdf.text.DocumentException {
        if (!isBlank(value)) {
            document.add(new org.openpdf.text.Paragraph(value, font));
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String translatePaymentType(PaymentType type) {
        if (type == null) {
            return "-";
        }
        return switch (type) {
            case CARD -> "Carte bancaire";
            case BANK_TRANSFER -> "Virement bancaire";
            case FREE -> "Gratuit";
            case CASH -> "Espèces";
        };
    }

    private static String joinNotBlank(String separator, String... values) {
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (isBlank(value)) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(separator);
            }
            sb.append(value.trim());
        }
        return sb.toString();
    }
}

package com.github.gcolin.platform;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

class JteCompileTest {

    private static TemplateEngine engine;

    @BeforeAll
    static void init() {
        engine = TemplateEngine.createPrecompiled(ContentType.Html);
    }

    static Stream<String> pageTemplates() throws IOException {
        Path root = Path.of("src/main/jte");
        List<String> templates = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(p -> p.toString().endsWith(".jte"))
                    .filter(p -> !p.toString().contains("tmpl"))
                    .filter(p -> !p.getFileName().toString().equals("head.jte"))
                    .filter(p -> !p.getFileName().toString().equals("menubar.jte"))
                    .filter(p -> !p.getFileName().toString().equals("footer.jte"))
                    .forEach(p -> templates.add(root.relativize(p).toString().replace('\\', '/')));
        }
        return templates.stream();
    }

    @ParameterizedTest
    @MethodSource("pageTemplates")
    void shouldCompileTemplate(String template) throws IOException {
        StringOutput output = new StringOutput();
        Map<String, Object> model = minimalModel();
        if (template.contains("subOptionEdit")) {
            model.put("option", new com.github.gcolin.registration.PlayerSubscriptionOption());
        }
        if (template.contains("customplayer")) {
            com.github.gcolin.player.CustomPlayer customPlayer = new com.github.gcolin.player.CustomPlayer();
            customPlayer.setLicence("");
            customPlayer.setName("");
            customPlayer.setFirstname("");
            customPlayer.setBirthDate("");
            customPlayer.setElo("");
            customPlayer.setCreationUser("");
            model.put("player", customPlayer);
        }
        try {
            engine.render(template, model, output);
        } catch (RuntimeException e) {
            throw new AssertionError("Failed to compile/render " + template + ": " + e.getMessage(), e);
        }
        String html = output.toString();
        if (html.contains("@template(") || html.contains("@!var")) {
            throw new AssertionError("Template " + template + " still contains invalid JTE directives in output");
        }
    }

    private Map<String, Object> minimalModel() {
        Map<String, Object> model = new HashMap<>();
        model.put("contextPath", "");
        model.put("msg", new Messages(java.util.Locale.FRENCH));
        model.put("user", new com.github.gcolin.auth.LoggedUser());
        model.put("state", new StateService() {
            @Override
            public String getLogin() {
                return "/login";
            }

            @Override
            public String getValue() {
                return "";
            }
        });
        com.github.gcolin.notification.Notifications notifications =
                Mockito.mock(com.github.gcolin.notification.Notifications.class);
        Mockito.when(notifications.getGlobal()).thenReturn(List.of());
        model.put("notifications", notifications);
        Page pageConfig = new Page();
        pageConfig.setTitle("Test Club");
        pageConfig.setOrgName("Test Club");
        pageConfig.setContactUrl("https://example.org/contact");
        model.put("pageConfig", pageConfig);
        model.put("page", "events");
        model.put("title", "Test");
        model.put("events", List.of());
        model.put("pendingEvents", List.of());
        model.put("stripePublic", null);
        model.put("paidPayments", List.of());
        model.put("eventGroups", List.of());
        model.put("eventCollections", List.of());
        model.put("eventGroup", null);
        model.put("eventCollection", null);
        com.github.gcolin.event.Event testEvent = new com.github.gcolin.event.Event();
        testEvent.setId(1);
        testEvent.setName("Test Event");
        model.put("event", testEvent);
        model.put("eventInfo", new com.github.gcolin.event.EventInfo());
        model.put("eventOptions", new java.util.HashMap<String, String>());
        model.put("missingPlayers", List.of());
        model.put("eventgroups", List.of());
        model.put("currentEventId", null);
        model.put("playerSubscription", new com.github.gcolin.registration.PlayerSubscription());
        model.put("created", false);
        model.put("updated", false);
        model.put("removed", false);
        model.put("createMode", false);
        com.github.gcolin.event.EventCollection testCollection = new com.github.gcolin.event.EventCollection();
        testCollection.setId(1);
        testCollection.setName("Test Collection");
        model.put("eventCollection", testCollection);
        model.put("eventGroup", new com.github.gcolin.event.EventGroup());
        model.put("totalPayments", 0.0);
        model.put("unpaidPlayerSubscriptions", 0.0);
        model.put("closestEvents", List.of());
        model.put("topEvents", List.of());
        model.put("agePyramid", List.of());
        model.put("agePyramidMax", 0);
        model.put("multiEventPlayers", List.of());
        model.put("unknownPaymentPlayers", List.of());
        model.put("recipients", List.of());
        model.put("isNew", false);
        model.put("errors", List.of());
        model.put("status", "ACTIVE");
        model.put("filterUrl", "/event");
        model.put("ribAvailable", false);
        model.put("stripeSimulated", true);
        model.put("showFinished", false);
        model.put("hasFinishedEvents", false);
        model.put("toggleDisplayUrl", "/event/my");
        model.put("players", List.of());
        model.put("memberships", List.of());
        model.put("membershipSubscriptions", Map.of());
        model.put("byClub", Map.of());
        model.put("byCategory", Map.of());
        model.put("byFederation", Map.of());
        model.put("amountByPaymentType", Map.of());
        model.put("canSelectEvent", Map.of());
        model.put("ineligibleReason", Map.of());
        model.put("total", 0);
        model.put("totalAmount", 0.0);
        model.put("totalUnpaidAmount", 0.0);
        model.put("rows", List.of());
        model.put("pendingRows", List.of());
        model.put("options", List.of());
        model.put("licenses", List.of());
        model.put("membership", new com.github.gcolin.membership.Membership());
        model.put("license", new com.github.gcolin.membership.License());
        model.put("price", new com.github.gcolin.membership.LicensePrice());
        model.put("option", new com.github.gcolin.membership.MembershipOption());
        model.put("statuses", com.github.gcolin.membership.MembershipStatus.values());
        model.put("optionTypes", com.github.gcolin.membership.MembershipOptionType.values());
        model.put("accessRules", com.github.gcolin.membership.MembershipOptionAccessRule.values());
        model.put("membershipOptions", List.of());
        model.put("availableOptions", List.of());
        model.put("prices", List.of());
        model.put("payment", new com.github.gcolin.payment.Payment());
        model.put("payments", List.of());
        model.put("paymentStatuses", com.github.gcolin.payment.PaymentStatus.values());
        model.put("paymentTypes", com.github.gcolin.payment.PaymentType.values());
        model.put("subscriptionDisplays", List.of());
        model.put("optionDisplays", List.of());
        model.put("paymentSearchNames", Map.of());
        model.put("paymentSearchLicences", Map.of());
        model.put("search", "");
        model.put("pageSize", 20);
        model.put("totalItems", 0L);
        model.put("currentPage", 1);
        model.put("totalPages", 1);
        model.put("hasPrev", false);
        model.put("hasNext", false);
        model.put("prevPage", 0);
        model.put("nextPage", 2);
        model.put("mismatches", List.of());
        model.put("notifs", List.of());
        model.put("notif", new com.github.gcolin.notification.Notification());
        model.put("authorizations", List.of());
        model.put("permissions", List.of());
        model.put("scopeTypes", List.of());
        model.put("sessions", List.of());
        model.put("sessionCount", 0);
        model.put("users", List.of());
        model.put("admins", java.util.Set.of());
        model.put("egnotifications", List.of());
        model.put("templates", new String[] {});
        model.put("cfg", Map.of());
        model.put("violations", List.of());
        model.put("statusCode", 404);
        model.put("statusMessage", "Not Found");
        model.put("tab", "club");
        model.put("secretsConfigured", Map.of());
        model.put("cancelledSubs", List.of());
        model.put("notPaidSubs", List.of());
        model.put("subsWithoutPayment", List.of());
        model.put("registrationClosed", false);
        model.put("registrationClosedMessageKey", "event.registrationClosed");
        model.put("logoAvailable", false);
        model.put("backgroundAvailable", false);
        model.put("ribSize", null);
        model.put("player", new com.github.gcolin.player.DisplayPlayer());
        model.put("eventId", null);
        model.put("startIndex", 0);
        model.put("manualPlayersFile", "");
        model.put("hasCollection", false);
        model.put("eventsJson", "[]");
        model.put("deskTicket", "");
        model.put("currentStatus", "NOT_PAID");
        model.put("isNew", false);
        model.put("clubRegisterEnabled", false);
        model.put("rowCount", 0);
        model.put("replaceableCount", 0);
        model.put("pendingRowCount", 0);
        model.put("orphanCount", 0);
        model.put("orphanCustomPlayers", List.of());
        model.put("manualPlayer", false);
        model.put("firstname", "");
        model.put("lastname", "");
        model.put("birthdate", "");
        model.put("category", "");
        model.put("nrffe", "");
        model.put("selectedLicenseType", null);
        model.put("playerYoung", false);
        model.put("favoritePlayers", List.of());
        com.github.gcolin.registration.PlayerSubscription testSub = new com.github.gcolin.registration.PlayerSubscription();
        testSub.setEvent(testEvent);
        model.put("sub", testSub);
        return model;
    }
}

package com.github.gcolin.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.gcolin.auth.RoleCode;
import com.github.gcolin.club.ClubSeasonFilter;
import com.github.gcolin.club.SeasonScope;

import com.github.gcolin.event.Event;
import com.github.gcolin.event.EventPaymentsReportService;
import com.github.gcolin.membership.Membership;
import com.github.gcolin.membership.MembershipDao;
import com.github.gcolin.membership.MembershipDisplay;
import com.github.gcolin.membership.MembershipOptionSubscriptionDao;
import com.github.gcolin.membership.MembershipStatus;
import com.github.gcolin.payment.Payment;
import com.github.gcolin.payment.PaymentStatus;
import com.github.gcolin.payment.PaymentType;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.platform.JteHtml;
import com.github.gcolin.platform.MailAttachment;
import com.github.gcolin.platform.PagedList;
import com.github.gcolin.platform.SendMail;
import com.github.gcolin.player.Player;
import com.github.gcolin.player.Find;
import com.github.gcolin.payment.PaymentDao;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import com.github.gcolin.registration.PlayerSubscriptionOptionDao;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Field;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PaymentApiTest {

    @Test
    void pageShouldBuildPaginationModel() throws Exception {
        PaymentApi api = new PaymentApi();
        PaymentDao paymentDao = mock(PaymentDao.class);
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);

        Payment p = new Payment();
        p.setId(1L);

        PagedList<Payment> paged = new PagedList<>(List.of(p), 0, 30);
        when(paymentDao.page(eq(0), eq(25), any(SeasonScope.class))).thenReturn(paged);
        when(subDao.findByPaymentId(1)).thenReturn(List.of());

        ClubSeasonFilter clubSeasonFilter = mock(ClubSeasonFilter.class);
        when(clubSeasonFilter.resolve(null)).thenReturn(SeasonScope.all());
        doAnswer(invocation -> {
            Map<String, Object> model = invocation.getArgument(0);
            model.put("seasons", List.of());
            model.put("seasonId", invocation.getArgument(1));
            return null;
        }).when(clubSeasonFilter).addToModel(any(), any());

        inject(api, "paymentService", paymentDao);
        inject(api, "playerSubscriptionService", subDao);
        inject(api, "membershipDao", mock(MembershipDao.class));
        inject(api, "find", mock(Find.class));
        inject(api, "clubSeasonFilter", clubSeasonFilter);

        JteHtml html = api.page(0, 25, "", null);
        Map<String, Object> model = html.getModel();

        assertEquals("payment/payments.jte", html.getTemplate());
        assertEquals(1, model.get("currentPage"));
        assertEquals(25, model.get("pageSize"));
        assertEquals(30L, model.get("totalItems"));
        assertEquals(2, model.get("totalPages"));
        assertFalse((Boolean) model.get("hasPrev"));
        assertTrue((Boolean) model.get("hasNext"));
    }

    @Test
    void editPaymentNullShouldCreateDefaultPayment() {
        PaymentApi api = new PaymentApi();

        JteHtml html = api.editPayment(null, "");
        Payment payment = (Payment) html.getModel().get("payment");

        assertEquals("payment/paymentEdit.jte", html.getTemplate());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals(PaymentType.CARD, payment.getType());
        assertEquals(0d, payment.getAmount());
        assertEquals(0L, payment.getAmountCents());
        assertFalse(payment.isDonation());
        assertFalse((Boolean) html.getModel().get("donationMode"));
    }

    @Test
    void newDonationShouldCreatePaidChequePayment() {
        PaymentApi api = new PaymentApi();

        JteHtml html = api.newPayment(true, null);
        Payment payment = (Payment) html.getModel().get("payment");

        assertTrue(payment.isDonation());
        assertTrue((Boolean) html.getModel().get("donationMode"));
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals(PaymentType.CHEQUE, payment.getType());
    }

    @Test
    void newPaymentFromMembershipShouldPrefillPaidChequePayment() throws Exception {
        PaymentApi api = new PaymentApi();
        MembershipDao membershipDao = mock(MembershipDao.class);

        Membership membership = new Membership();
        membership.setId(62);
        membership.setUser("member@example.com");
        membership.setAmountCents(4500);
        membership.setStatus(MembershipStatus.PAID);
        membership.setFirstname("Ada");
        membership.setLastname("Lovelace");
        when(membershipDao.find(62)).thenReturn(membership);

        inject(api, "membershipDao", membershipDao);
        inject(api, "playerSubscriptionService", mock(PlayerSubscriptionDao.class));
        inject(api, "playerSubscriptionOptionService", mock(PlayerSubscriptionOptionDao.class));
        inject(api, "find", mock(Find.class));

        JteHtml html = api.newPayment(false, 62);
        Payment payment = (Payment) html.getModel().get("payment");
        @SuppressWarnings("unchecked")
        List<MembershipDisplay> membershipDisplays =
                (List<MembershipDisplay>) html.getModel().get("membershipDisplays");

        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals(PaymentType.CHEQUE, payment.getType());
        assertEquals("member@example.com", payment.getUserEmail());
        assertEquals(45.0, payment.getAmount());
        assertEquals(1, membershipDisplays.size());
        assertEquals(62, membershipDisplays.get(0).getMembershipId());
        assertEquals("Ada Lovelace", membershipDisplays.get(0).getMemberName());
    }

    @Test
    void newPaymentFromMembershipShouldOpenExistingPaymentWhenLinked() throws Exception {
        PaymentApi api = new PaymentApi();
        MembershipDao membershipDao = mock(MembershipDao.class);
        PaymentDao paymentDao = mock(PaymentDao.class);

        Payment existing = new Payment();
        existing.setId(99L);
        existing.setUserEmail("member@example.com");
        existing.setStatus(PaymentStatus.PAID);
        existing.setType(PaymentType.CASH);
        existing.setAmount(45.0);

        Membership membership = new Membership();
        membership.setId(62);
        membership.setPayment(existing);
        when(membershipDao.find(62)).thenReturn(membership);
        when(paymentDao.find(99)).thenReturn(existing);
        when(membershipDao.findByPaymentId(99)).thenReturn(List.of(membership));

        inject(api, "membershipDao", membershipDao);
        inject(api, "paymentService", paymentDao);
        inject(api, "playerSubscriptionService", mock(PlayerSubscriptionDao.class));
        inject(api, "playerSubscriptionOptionService", mock(PlayerSubscriptionOptionDao.class));
        inject(api, "find", mock(Find.class));

        JteHtml html = api.newPayment(false, 62);
        Payment payment = (Payment) html.getModel().get("payment");

        assertSame(existing, payment);
        assertEquals("payment/paymentEdit.jte", html.getTemplate());
    }

    @Test
    void editPaymentShouldThrowWhenUnknownId() throws Exception {
        PaymentApi api = new PaymentApi();
        PaymentDao paymentDao = mock(PaymentDao.class);

        when(paymentDao.find(999)).thenReturn(null);
        inject(api, "paymentService", paymentDao);
        inject(api, "playerSubscriptionService", mock(PlayerSubscriptionDao.class));
        inject(api, "playerSubscriptionOptionService", mock(PlayerSubscriptionOptionDao.class));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> api.editPayment(999, ""));

        assertEquals(404, ex.getResponse().getStatus());
    }

    @Test
    void saveToRemoveShouldDetachSubscriptionsAndDeletePayment() throws Exception {
        PaymentApi api = new PaymentApi();
        PaymentDao paymentDao = mock(PaymentDao.class);
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        PlayerSubscriptionOptionDao optionDao = mock(PlayerSubscriptionOptionDao.class);
        MembershipDao membershipDao = mock(MembershipDao.class);

        PlayerSubscription sub = new PlayerSubscription();
        sub.setId(7);
        when(subDao.findByPaymentId(42)).thenReturn(List.of(sub));
        when(optionDao.findByPaymentId(42)).thenReturn(List.of());
        when(membershipDao.findByPaymentId(42)).thenReturn(List.of());

        inject(api, "paymentService", paymentDao);
        inject(api, "playerSubscriptionService", subDao);
        inject(api, "playerSubscriptionOptionService", optionDao);
        inject(api, "membershipDao", membershipDao);
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/payment")));

        Response response = api.save(42, "true", null, null, null, null, null, null, null, null, null, null, null, null);

        assertEquals(303, response.getStatus());
        assertEquals(URI.create("http://localhost:8080/payment"), response.getLocation());
        verify(subDao).persist(sub);
        verify(paymentDao).remove(42);
    }

    @Test
    void saveShouldThrowBadRequestOnInvalidSubscriptionId() throws Exception {
        PaymentApi api = new PaymentApi();
        PaymentDao paymentDao = mock(PaymentDao.class);
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        PlayerSubscriptionOptionDao optionDao = mock(PlayerSubscriptionOptionDao.class);
        MembershipDao membershipDao = mock(MembershipDao.class);

        Payment payment = new Payment();
        payment.setId(5L);
        when(paymentDao.find(5)).thenReturn(payment);
        when(paymentDao.merge(payment)).thenReturn(payment);
        when(subDao.findByPaymentId(5)).thenReturn(List.of());
        when(optionDao.findByPaymentId(5)).thenReturn(List.of());
        when(membershipDao.findByPaymentId(5)).thenReturn(List.of());

        inject(api, "paymentService", paymentDao);
        inject(api, "playerSubscriptionService", subDao);
        inject(api, "playerSubscriptionOptionService", optionDao);
        inject(api, "membershipDao", membershipDao);
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/payment/5/edit")));

        WebApplicationException ex = assertThrows(
                WebApplicationException.class,
                () -> api.save(5, "false", "u@test.com", null, null, "false", "PENDING", "CARD", 10.0, "", "", List.of("x"), null, null));

        assertEquals(400, ex.getResponse().getStatus());
    }

    @Test
    void saveShouldAttachSelectedSubscriptionsAndRedirect() throws Exception {
        PaymentApi api = new PaymentApi();
        PaymentDao paymentDao = mock(PaymentDao.class);
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        PlayerSubscriptionOptionDao optionDao = mock(PlayerSubscriptionOptionDao.class);
        MembershipDao membershipDao = mock(MembershipDao.class);

        Payment payment = new Payment();
        payment.setId(10L);

        PlayerSubscription previouslyAttached = new PlayerSubscription();
        previouslyAttached.setId(1);
        previouslyAttached.setPayment(payment);

        PlayerSubscription selected = new PlayerSubscription();
        selected.setId(21);
        selected.setAmountCents(3500L);

        when(paymentDao.find(10)).thenReturn(payment);
        when(paymentDao.merge(payment)).thenReturn(payment);
        when(subDao.findByPaymentId(10)).thenReturn(List.of(previouslyAttached));
        when(subDao.find(21)).thenReturn(selected);
        when(optionDao.findByPaymentId(10)).thenReturn(List.of());
        when(membershipDao.findByPaymentId(10)).thenReturn(List.of());

        inject(api, "paymentService", paymentDao);
        inject(api, "playerSubscriptionService", subDao);
        inject(api, "playerSubscriptionOptionService", optionDao);
        inject(api, "membershipDao", membershipDao);
        inject(api, "find", mock(Find.class));
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/payment/10/edit")));

        Response response = api.save(
                10,
                "false",
                "user@test.com",
                "Jean Dupont",
                null,
                "false",
                "PAID",
                "CARD",
                35.0,
                "sess_10",
                "pi_10",
                List.of("21"),
                null,
                null);

        assertEquals(303, response.getStatus());
        assertEquals(URI.create("http://localhost:8080/payment/10/edit"), response.getLocation());
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals(PaymentType.CARD, payment.getType());
        assertEquals("user@test.com", payment.getUserEmail());
        assertEquals("Jean Dupont", payment.getPayerName());
        assertEquals(3500L, payment.getAmountCents());
        assertEquals(payment, selected.getPayment());
        verify(subDao).persist(previouslyAttached);
        verify(subDao).persist(selected);
    }

    @Test
    void saveShouldAttachSelectedMembershipsAndRedirect() throws Exception {
        PaymentApi api = new PaymentApi();
        PaymentDao paymentDao = mock(PaymentDao.class);
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        PlayerSubscriptionOptionDao optionDao = mock(PlayerSubscriptionOptionDao.class);
        MembershipDao membershipDao = mock(MembershipDao.class);

        Payment payment = new Payment();
        payment.setId(11L);

        Membership previouslyAttached = new Membership();
        previouslyAttached.setId(1);
        previouslyAttached.setPayment(payment);

        Membership selected = new Membership();
        selected.setId(55);
        selected.setAmountCents(4000);

        when(paymentDao.find(11)).thenReturn(payment);
        when(paymentDao.merge(payment)).thenReturn(payment);
        when(subDao.findByPaymentId(11)).thenReturn(List.of());
        when(optionDao.findByPaymentId(11)).thenReturn(List.of());
        when(membershipDao.findByPaymentId(11)).thenReturn(List.of(previouslyAttached));
        when(membershipDao.find(55)).thenReturn(selected);

        inject(api, "paymentService", paymentDao);
        inject(api, "playerSubscriptionService", subDao);
        inject(api, "playerSubscriptionOptionService", optionDao);
        inject(api, "membershipDao", membershipDao);
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/payment/11/edit")));

        Response response = api.save(
                11, "false", "user@test.com", null, null, "false", "PAID", "CARD", 40.0, "sess_11", "pi_11", null, null, List.of("55"));

        assertEquals(303, response.getStatus());
        assertEquals(URI.create("http://localhost:8080/payment/11/edit"), response.getLocation());
        assertNull(previouslyAttached.getPayment());
        assertEquals(payment, selected.getPayment());
        verify(membershipDao).merge(previouslyAttached);
        verify(membershipDao).merge(selected);
    }

    @Test
    void editPaymentShouldIncludeMembershipDisplays() throws Exception {
        PaymentApi api = new PaymentApi();
        PaymentDao paymentDao = mock(PaymentDao.class);
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        PlayerSubscriptionOptionDao optionDao = mock(PlayerSubscriptionOptionDao.class);
        MembershipDao membershipDao = mock(MembershipDao.class);

        Payment payment = new Payment();
        payment.setId(329L);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setType(PaymentType.CARD);
        payment.setAmount(40.0);

        Membership membership = new Membership();
        membership.setId(88);
        membership.setFirstname("Jean");
        membership.setLastname("Dupont");
        membership.setAmountCents(4000);
        membership.setStatus(MembershipStatus.APPROVED);

        when(paymentDao.find(329)).thenReturn(payment);
        when(subDao.findByPaymentId(329)).thenReturn(List.of());
        when(optionDao.findByPaymentId(329)).thenReturn(List.of());
        when(membershipDao.findByPaymentId(329)).thenReturn(List.of(membership));

        inject(api, "paymentService", paymentDao);
        inject(api, "playerSubscriptionService", subDao);
        inject(api, "playerSubscriptionOptionService", optionDao);
        inject(api, "membershipDao", membershipDao);
        inject(api, "find", mock(Find.class));

        JteHtml html = api.editPayment(329, "");
        @SuppressWarnings("unchecked")
        List<MembershipDisplay> displays = (List<MembershipDisplay>) html.getModel().get("membershipDisplays");

        assertEquals(1, displays.size());
        assertEquals(88, displays.get(0).getMembershipId());
        assertEquals("/membership/88/edit", displays.get(0).getEditLink());
        assertEquals("Jean Dupont", displays.get(0).getMemberName());
        assertEquals("APPROVED", displays.get(0).getStatus());
        assertEquals(4000, displays.get(0).getAmountCents());
    }

    @Test
    void saveDonationShouldPersistWithoutLinks() throws Exception {
        PaymentApi api = new PaymentApi();
        PaymentDao paymentDao = mock(PaymentDao.class);
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        PlayerSubscriptionOptionDao optionDao = mock(PlayerSubscriptionOptionDao.class);
        MembershipDao membershipDao = mock(MembershipDao.class);

        doAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);
                    payment.setId(100L);
                    return null;
                })
                .when(paymentDao)
                .persist(any(Payment.class));
        when(subDao.findByPaymentId(100)).thenReturn(List.of());
        when(optionDao.findByPaymentId(100)).thenReturn(List.of());
        when(membershipDao.findByPaymentId(100)).thenReturn(List.of());

        inject(api, "paymentService", paymentDao);
        inject(api, "playerSubscriptionService", subDao);
        inject(api, "playerSubscriptionOptionService", optionDao);
        inject(api, "membershipDao", membershipDao);
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/payment/100/edit")));

        Response response = api.save(
                null,
                "false",
                "donor@test.com",
                "Marie Curie",
                "12 rue de la Paix\n75002 Paris",
                "true",
                "PAID",
                "CHEQUE",
                50.0,
                "",
                "",
                null,
                null,
                null);

        assertEquals(303, response.getStatus());
        verify(paymentDao).persist(any(Payment.class));
        verify(membershipDao, org.mockito.Mockito.never()).find(any());
        verify(subDao, org.mockito.Mockito.never()).find(any());
    }

    @Test
    void saveDonationShouldRejectMembershipLink() throws Exception {
        PaymentApi api = new PaymentApi();

        WebApplicationException ex = assertThrows(
                WebApplicationException.class,
                () -> api.save(
                        null,
                        "false",
                        "donor@test.com",
                        "Marie Curie",
                        "12 rue de la Paix",
                        "true",
                        "PAID",
                        "CHEQUE",
                        50.0,
                        "",
                        "",
                        null,
                        null,
                        List.of("55")));

        assertEquals(400, ex.getResponse().getStatus());
    }

    @Test
    void invoiceShouldReturnDonationAttestationPdf() throws Exception {
        PaymentApi api = new PaymentApi();
        PaymentDao paymentDao = mock(PaymentDao.class);
        PaymentReceiptPdfService pdfService = mock(PaymentReceiptPdfService.class);
        com.github.gcolin.auth.LoggedUser loggedUser = mock(com.github.gcolin.auth.LoggedUser.class);

        Payment payment = new Payment();
        payment.setId(77L);
        payment.setUserEmail("donor@test.com");
        payment.setStatus(PaymentStatus.PAID);
        payment.setDonation(true);
        payment.setPayerName("Marie Curie");
        payment.setPayerAddress("12 rue de la Paix");
        payment.setAmount(50.0);

        when(loggedUser.getEmail()).thenReturn("donor@test.com");
        when(loggedUser.hasRole(RoleCode.TRESORIER)).thenReturn(true);
        when(paymentDao.find(77)).thenReturn(payment);
        when(pdfService.generateDonationAttestation(payment)).thenReturn(new byte[] {1, 2, 3});

        inject(api, "loggerUser", loggedUser);
        inject(api, "paymentService", paymentDao);
        inject(api, "playerSubscriptionService", mock(PlayerSubscriptionDao.class));
        inject(api, "membershipDao", mock(MembershipDao.class));
        inject(api, "membershipOptionSubscriptionDao", mock(MembershipOptionSubscriptionDao.class));
        inject(api, "paymentReceiptPdfService", pdfService);

        Response response = api.invoice(77);

        assertEquals(200, response.getStatus());
        assertEquals("attachment; filename=attestation-fiscale-77.pdf", response.getHeaderString("Content-Disposition"));
        assertTrue(java.util.Arrays.equals(new byte[] {1, 2, 3}, (byte[]) response.getEntity()));
        verify(pdfService).generateDonationAttestation(payment);
    }

    @Test
    void exportCsvShouldEscapeQuotesInEmail() throws Exception {
        PaymentApi api = new PaymentApi();
        PaymentDao paymentDao = mock(PaymentDao.class);

        Payment payment = new Payment();
        payment.setId(3L);
        payment.setUserEmail("a\"b@test.com");
        payment.setStatus(PaymentStatus.PAID);
        payment.setType(PaymentType.CARD);
        payment.setAmount(12.5);
        payment.setCreatedAt(LocalDateTime.of(2026, 5, 1, 10, 0));
        payment.setUpdatedAt(LocalDateTime.of(2026, 5, 2, 10, 0));
        payment.setStripeSessionId("sess_1");

        when(paymentDao.all(any(SeasonScope.class))).thenReturn(List.of(payment));

        ClubSeasonFilter clubSeasonFilter = mock(ClubSeasonFilter.class);
        when(clubSeasonFilter.resolve(null)).thenReturn(SeasonScope.all());

        inject(api, "paymentService", paymentDao);
        inject(api, "clubSeasonFilter", clubSeasonFilter);

        Response response = api.exportCsv(null);
        String csv = (String) response.getEntity();

        assertEquals(200, response.getStatus());
        assertTrue(csv.contains("id;email;status;type;amount;createdAt;updatedAt;stripeSessionId"));
        assertTrue(csv.contains("\"a\"\"b@test.com\""));
        assertTrue(csv.contains("\"sess_1\""));
    }

    @Test
    void exportCsvDetailsShouldHandleNoSubAndDetailedRows() throws Exception {
        PaymentApi api = new PaymentApi();
        PaymentDao paymentDao = mock(PaymentDao.class);
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        Find find = mock(Find.class);

        Payment p1 = new Payment();
        p1.setId(1L);
        p1.setType(PaymentType.CARD);
        p1.setStatus(PaymentStatus.PENDING);
        p1.setAmount(10.0);

        Payment p2 = new Payment();
        p2.setId(2L);
        p2.setType(PaymentType.BANK_TRANSFER);
        p2.setStatus(PaymentStatus.PAID);
        p2.setAmount(20.0);

        Event event = new Event();
        event.setName("Open");
        PlayerSubscription sub = new PlayerSubscription();
        sub.setNrFfe("LIC1");
        sub.setEvent(event);

        Player player = new Player();
        player.setFirstname("Ada");
        player.setName("Lovelace");

        when(paymentDao.all(any(SeasonScope.class))).thenReturn(List.of(p1, p2));
        when(subDao.findByPaymentId(1)).thenReturn(List.of());
        when(subDao.findByPaymentId(2)).thenReturn(List.of(sub));
        when(find.player("LIC1", null)).thenReturn(player);

        ClubSeasonFilter clubSeasonFilter = mock(ClubSeasonFilter.class);
        when(clubSeasonFilter.resolve(null)).thenReturn(SeasonScope.all());

        inject(api, "paymentService", paymentDao);
        inject(api, "playerSubscriptionService", subDao);
        inject(api, "membershipDao", mock(MembershipDao.class));
        inject(api, "find", find);
        inject(api, "clubSeasonFilter", clubSeasonFilter);

        Response response = api.exportCsvDetails(null);
        String csv = (String) response.getEntity();

        assertEquals(200, response.getStatus());
        assertTrue(csv.contains("line_type"));
        assertTrue(csv.contains("1;CARD;PENDING;10.0;"));
        assertTrue(csv.contains("2;BANK_TRANSFER;PAID;20.0;event;Open"));
        assertTrue(csv.contains("Ada Lovelace;LIC1"));
    }

    @Test
    void exportShouldDetachElementsAndReturnPagedList() throws Exception {
        PaymentApi api = new PaymentApi();
        PaymentDao paymentDao = mock(PaymentDao.class);

        Payment payment = new Payment();
        payment.setId(4L);

        PagedList<Payment> paged = new PagedList<>(List.of(payment), 0, 1);

        when(paymentDao.page(eq(0), eq(50), any(SeasonScope.class))).thenReturn(paged);
        when(paymentDao.detachAll(paged.getElements())).thenReturn(paged.getElements());

        ClubSeasonFilter clubSeasonFilter = mock(ClubSeasonFilter.class);
        when(clubSeasonFilter.resolve(null)).thenReturn(SeasonScope.all());

        inject(api, "paymentService", paymentDao);
        inject(api, "clubSeasonFilter", clubSeasonFilter);

        PagedList<Payment> result = api.export(0, 50, null);

        assertSame(paged, result);
        assertEquals(1, result.getElements().size());
        verify(paymentDao).detachAll(paged.getElements());
    }

    @Test
    void exportAccountingPdfShouldUseSelectedSeason() throws Exception {
        PaymentApi api = new PaymentApi();
        PaymentDao paymentDao = mock(PaymentDao.class);
        ClubSeasonFilter seasonFilter = mock(ClubSeasonFilter.class);
        EventPaymentsReportService reportService = mock(EventPaymentsReportService.class);
        SeasonScope scope = SeasonScope.all();
        byte[] pdf = "%PDF-test".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        when(seasonFilter.resolve(4)).thenReturn(scope);
        when(paymentDao.findPaid(scope)).thenReturn(List.of());
        when(reportService.generateForAccountingDetails(any(), eq(scope))).thenReturn(pdf);

        inject(api, "paymentService", paymentDao);
        inject(api, "playerSubscriptionService", mock(PlayerSubscriptionDao.class));
        inject(api, "membershipDao", mock(MembershipDao.class));
        inject(api, "clubSeasonFilter", seasonFilter);
        inject(api, "eventPaymentsReportService", reportService);

        Response response = api.exportAccountingPdf(4);

        assertEquals(200, response.getStatus());
        assertSame(pdf, response.getEntity());
        assertTrue(response.getHeaderString("Content-Disposition").startsWith("attachment; filename=journal-recettes-"));
        verify(paymentDao).findPaid(scope);
    }

    @Test
    void savePayerShouldUpdateOwnedPaidPaymentAndRedirect() throws Exception {
        PaymentApi api = new PaymentApi();
        PaymentDao paymentDao = mock(PaymentDao.class);
        MembershipDao membershipDao = mock(MembershipDao.class);
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        com.github.gcolin.auth.LoggedUser loggedUser = mock(com.github.gcolin.auth.LoggedUser.class);

        Payment payment = new Payment();
        payment.setId(15L);
        payment.setUserEmail("owner@test.com");
        payment.setStatus(PaymentStatus.PAID);

        when(loggedUser.getEmail()).thenReturn("owner@test.com");
        when(paymentDao.find(15)).thenReturn(payment);
        when(paymentDao.merge(payment)).thenReturn(payment);
        when(membershipDao.findByPaymentId(15)).thenReturn(List.of());
        when(subDao.findByPaymentId(15)).thenReturn(List.of());

        inject(api, "loggerUser", loggedUser);
        inject(api, "paymentService", paymentDao);
        inject(api, "membershipDao", membershipDao);
        inject(api, "playerSubscriptionService", subDao);
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/event/my?success=payerUpdated")));

        Response response = api.savePayer(15, "Alice Martin", "/event/my");

        assertEquals(303, response.getStatus());
        assertEquals("Alice Martin", payment.getPayerName());
        verify(paymentDao).merge(payment);
        assertEquals(URI.create("/event/my?success=payerUpdated"), response.getLocation());
    }

    @Test
    void savePayerShouldForbidNonOwner() throws Exception {
        PaymentApi api = new PaymentApi();
        PaymentDao paymentDao = mock(PaymentDao.class);
        com.github.gcolin.auth.LoggedUser loggedUser = mock(com.github.gcolin.auth.LoggedUser.class);

        Payment payment = new Payment();
        payment.setId(15L);
        payment.setUserEmail("owner@test.com");
        payment.setStatus(PaymentStatus.PAID);

        when(loggedUser.getEmail()).thenReturn("other@test.com");
        when(paymentDao.find(15)).thenReturn(payment);
        inject(api, "loggerUser", loggedUser);
        inject(api, "paymentService", paymentDao);

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> api.savePayer(15, "X", null));
        assertEquals(403, ex.getResponse().getStatus());
    }

    @Test
    void sendReceiptShouldEmailPdfAndRedirect() throws Exception {
        PaymentApi api = new PaymentApi();
        PaymentDao paymentDao = mock(PaymentDao.class);
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        MembershipDao membershipDao = mock(MembershipDao.class);
        MembershipOptionSubscriptionDao optionSubDao = mock(MembershipOptionSubscriptionDao.class);
        PaymentReceiptPdfService pdfService = mock(PaymentReceiptPdfService.class);
        SendMail sendMail = mock(SendMail.class);
        Find find = mock(Find.class);
        com.github.gcolin.auth.LoggedUser loggedUser = mock(com.github.gcolin.auth.LoggedUser.class);

        Payment payment = new Payment();
        payment.setId(44L);
        payment.setUserEmail("payer@test.com");
        payment.setPayerName("Alice");
        payment.setStatus(PaymentStatus.PAID);
        payment.setAmountCents(8200L);
        payment.setDonation(false);

        when(paymentDao.find(44)).thenReturn(payment);
        when(subDao.findByPaymentId(44)).thenReturn(List.of());
        when(membershipDao.findByPaymentId(44)).thenReturn(List.of());
        when(optionSubDao.findByMembershipIds(any())).thenReturn(List.of());
        when(pdfService.generatePaymentReceipt(eq(payment), any(), any(), any(), eq(find), anyString()))
                .thenReturn("%PDF".getBytes());
        when(loggedUser.getUsername()).thenReturn("admin");

        inject(api, "paymentService", paymentDao);
        inject(api, "playerSubscriptionService", subDao);
        inject(api, "membershipDao", membershipDao);
        inject(api, "membershipOptionSubscriptionDao", optionSubDao);
        inject(api, "paymentReceiptPdfService", pdfService);
        inject(api, "sendMail", sendMail);
        inject(api, "find", find);
        inject(api, "loggerUser", loggedUser);
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/payment/44/edit?success=receiptSent")));

        Response response = api.sendReceipt(44);

        assertEquals(303, response.getStatus());
        assertEquals(URI.create("/payment/44/edit?success=receiptSent"), response.getLocation());
        verify(sendMail).send(any(PaymentReceiptMail.class), eq("payer@test.com"), eq("Votre reçu de paiement"), any(MailAttachment.class));
    }

    @Test
    void sendReceiptShouldEmailDonationAttestationAndRedirect() throws Exception {
        PaymentApi api = new PaymentApi();
        PaymentDao paymentDao = mock(PaymentDao.class);
        PaymentReceiptPdfService pdfService = mock(PaymentReceiptPdfService.class);
        SendMail sendMail = mock(SendMail.class);
        com.github.gcolin.auth.LoggedUser loggedUser = mock(com.github.gcolin.auth.LoggedUser.class);

        Payment payment = new Payment();
        payment.setId(78L);
        payment.setUserEmail("donor@test.com");
        payment.setPayerName("Marie Curie");
        payment.setPayerAddress("12 rue de la Paix");
        payment.setStatus(PaymentStatus.PAID);
        payment.setAmountCents(5000L);
        payment.setDonation(true);

        when(paymentDao.find(78)).thenReturn(payment);
        when(pdfService.generateDonationAttestation(payment)).thenReturn("%PDF-donation".getBytes());
        when(loggedUser.getUsername()).thenReturn("admin");

        inject(api, "paymentService", paymentDao);
        inject(api, "playerSubscriptionService", mock(PlayerSubscriptionDao.class));
        inject(api, "membershipDao", mock(MembershipDao.class));
        inject(api, "membershipOptionSubscriptionDao", mock(MembershipOptionSubscriptionDao.class));
        inject(api, "paymentReceiptPdfService", pdfService);
        inject(api, "sendMail", sendMail);
        inject(api, "find", mock(Find.class));
        inject(api, "loggerUser", loggedUser);
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/payment/78/edit?success=receiptSent")));

        Response response = api.sendReceipt(78);

        assertEquals(303, response.getStatus());
        assertEquals(URI.create("/payment/78/edit?success=receiptSent"), response.getLocation());
        verify(pdfService).generateDonationAttestation(payment);
        verify(sendMail)
                .send(
                        any(PaymentReceiptMail.class),
                        eq("donor@test.com"),
                        eq("Votre attestation fiscale"),
                        any(MailAttachment.class));
    }

    @Test
    void exportByIdAndSubByIdShouldReturnDaoValues() throws Exception {
        PaymentApi api = new PaymentApi();
        PaymentDao paymentDao = mock(PaymentDao.class);
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);

        Payment payment = new Payment();
        payment.setId(7L);

        PlayerSubscription sub = new PlayerSubscription();
        sub.setId(70);

        when(paymentDao.find(7)).thenReturn(payment);
        when(subDao.findByPaymentId(7)).thenReturn(List.of(sub));

        inject(api, "paymentService", paymentDao);
        inject(api, "playerSubscriptionService", subDao);

        Payment byId = api.exportById(7);
        List<PlayerSubscription> subs = api.subById(7);

        assertSame(payment, byId);
        assertEquals(1, subs.size());
        assertEquals(70, subs.get(0).getId());
    }

    private static UriInfo mockUriInfo(URI target) {
        UriInfo uriInfo = mock(UriInfo.class);
        UriBuilder uriBuilder = mock(UriBuilder.class);

        when(uriInfo.getBaseUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        when(uriBuilder.queryParam(anyString(), any())).thenReturn(uriBuilder);
        when(uriBuilder.replaceQuery(anyString())).thenReturn(uriBuilder);
        when(uriBuilder.replaceQueryParam(anyString(), any())).thenReturn(uriBuilder);
        when(uriBuilder.fragment(anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(target);

        return uriInfo;
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

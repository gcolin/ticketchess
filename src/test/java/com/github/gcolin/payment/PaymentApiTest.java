package com.github.gcolin.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import com.github.gcolin.club.ClubSeasonFilter;
import com.github.gcolin.club.SeasonScope;

import com.github.gcolin.event.Event;
import com.github.gcolin.event.EventPaymentsReportService;
import com.github.gcolin.payment.Payment;
import com.github.gcolin.payment.PaymentStatus;
import com.github.gcolin.payment.PaymentType;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.platform.PagedList;
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
import com.github.gcolin.platform.JteHtml;

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

        JteHtml html = api.editPayment(null);
        Payment payment = (Payment) html.getModel().get("payment");

        assertEquals("payment/paymentEdit.jte", html.getTemplate());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals(PaymentType.CARD, payment.getType());
        assertEquals(0d, payment.getAmount());
        assertEquals(0L, payment.getAmountCents());
    }

    @Test
    void editPaymentShouldThrowWhenUnknownId() throws Exception {
        PaymentApi api = new PaymentApi();
        PaymentDao paymentDao = mock(PaymentDao.class);

        when(paymentDao.find(999)).thenReturn(null);
        inject(api, "paymentService", paymentDao);
        inject(api, "playerSubscriptionService", mock(PlayerSubscriptionDao.class));
        inject(api, "playerSubscriptionOptionService", mock(PlayerSubscriptionOptionDao.class));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> api.editPayment(999));

        assertEquals(404, ex.getResponse().getStatus());
    }

    @Test
    void saveToRemoveShouldDetachSubscriptionsAndDeletePayment() throws Exception {
        PaymentApi api = new PaymentApi();
        PaymentDao paymentDao = mock(PaymentDao.class);
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        PlayerSubscriptionOptionDao optionDao = mock(PlayerSubscriptionOptionDao.class);

        PlayerSubscription sub = new PlayerSubscription();
        sub.setId(7);
        when(subDao.findByPaymentId(42)).thenReturn(List.of(sub));
        when(optionDao.findByPaymentId(42)).thenReturn(List.of());

        inject(api, "paymentService", paymentDao);
        inject(api, "playerSubscriptionService", subDao);
        inject(api, "playerSubscriptionOptionService", optionDao);
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/payment")));

        Response response = api.save(42, "true", null, null, null, null, null, null, null, null);

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

        Payment payment = new Payment();
        payment.setId(5L);
        when(paymentDao.find(5)).thenReturn(payment);
        when(paymentDao.merge(payment)).thenReturn(payment);
        when(subDao.findByPaymentId(5)).thenReturn(List.of());
        when(optionDao.findByPaymentId(5)).thenReturn(List.of());

        inject(api, "paymentService", paymentDao);
        inject(api, "playerSubscriptionService", subDao);
        inject(api, "playerSubscriptionOptionService", optionDao);
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/payment/5/edit")));

        WebApplicationException ex = assertThrows(
                WebApplicationException.class,
                () -> api.save(5, "false", "u@test.com", "PENDING", "CARD", 10.0, "", "", List.of("x"), null));

        assertEquals(400, ex.getResponse().getStatus());
    }

    @Test
    void saveShouldAttachSelectedSubscriptionsAndRedirect() throws Exception {
        PaymentApi api = new PaymentApi();
        PaymentDao paymentDao = mock(PaymentDao.class);
        PlayerSubscriptionDao subDao = mock(PlayerSubscriptionDao.class);
        PlayerSubscriptionOptionDao optionDao = mock(PlayerSubscriptionOptionDao.class);

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

        inject(api, "paymentService", paymentDao);
        inject(api, "playerSubscriptionService", subDao);
        inject(api, "playerSubscriptionOptionService", optionDao);
        inject(api, "find", mock(Find.class));
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/payment/10/edit")));

        Response response =
                api.save(10, "false", "user@test.com", "PAID", "CARD", 35.0, "sess_10", "pi_10", List.of("21"), null);

        assertEquals(303, response.getStatus());
        assertEquals(URI.create("http://localhost:8080/payment/10/edit"), response.getLocation());
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals(PaymentType.CARD, payment.getType());
        assertEquals("user@test.com", payment.getUserEmail());
        assertEquals(3500L, payment.getAmountCents());
        assertEquals(payment, selected.getPayment());
        verify(subDao).persist(previouslyAttached);
        verify(subDao).persist(selected);
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
        inject(api, "find", find);
        inject(api, "clubSeasonFilter", clubSeasonFilter);

        Response response = api.exportCsvDetails(null);
        String csv = (String) response.getEntity();

        assertEquals(200, response.getStatus());
        assertTrue(csv.contains("id;type;status;amount;event_name;player_name;player_license"));
        assertTrue(csv.contains("1;CARD;PENDING;10.0;;;"));
        assertTrue(csv.contains("2;BANK_TRANSFER;PAID;20.0;Open;Ada Lovelace;LIC1"));
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
        when(reportService.generateForAccounting(List.of(), scope)).thenReturn(pdf);

        inject(api, "paymentService", paymentDao);
        inject(api, "clubSeasonFilter", seasonFilter);
        inject(api, "eventPaymentsReportService", reportService);

        Response response = api.exportAccountingPdf(4);

        assertEquals(200, response.getStatus());
        assertSame(pdf, response.getEntity());
        assertTrue(response.getHeaderString("Content-Disposition").startsWith("attachment; filename=journal-recettes-"));
        verify(paymentDao).findPaid(scope);
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
        when(uriBuilder.build()).thenReturn(target);

        return uriInfo;
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

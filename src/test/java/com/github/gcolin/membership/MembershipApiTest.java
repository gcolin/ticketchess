package com.github.gcolin.membership;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.github.gcolin.club.ClubSeasonFilter;
import com.github.gcolin.club.SeasonScope;
import com.github.gcolin.membership.MembershipOption;
import com.github.gcolin.membership.MembershipOptionSubscription;
import com.github.gcolin.membership.MembershipStatus;
import com.github.gcolin.membership.MembershipDao;
import com.github.gcolin.membership.MembershipOptionDao;
import com.github.gcolin.membership.MembershipOptionSubscriptionDao;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Field;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.github.gcolin.platform.JteHtml;

class MembershipApiTest {

    // --- list ---

    @Test
    void listShouldReturnMembershipsSortedByIdDescAndGroupSubscriptions() throws Exception {
        MembershipApi api = new MembershipApi();

        MembershipDao membershipDao = mock(MembershipDao.class);
        MembershipOptionSubscriptionDao subscriptionDao = mock(MembershipOptionSubscriptionDao.class);
        MembershipOptionDao membershipOptionDao = mock(MembershipOptionDao.class);

        Membership m1 = new Membership();
        m1.setId(1);
        Membership m2 = new Membership();
        m2.setId(5);

        when(membershipDao.all(any(SeasonScope.class))).thenReturn(new ArrayList<>(List.of(m1, m2)));
        when(subscriptionDao.findByMembershipIds(any())).thenReturn(List.of());

        inject(api, "membershipDao", membershipDao);
        inject(api, "membershipOptionSubscriptionDao", subscriptionDao);
        inject(api, "membershipOptionDao", membershipOptionDao);
        inject(api, "clubSeasonFilter", mockClubSeasonFilter());

        JteHtml html = api.list(null);
        Map<String, Object> model = html.getModel();

        assertEquals("membership/membership.jte", html.getTemplate());
        @SuppressWarnings("unchecked")
        List<Membership> memberships = (List<Membership>) model.get("memberships");
        // sorted descending by id: 5, 1
        assertEquals(5, memberships.get(0).getId());
        assertEquals(1, memberships.get(1).getId());
    }

    @Test
    void listShouldGroupSubscriptionsByMembershipId() throws Exception {
        MembershipApi api = new MembershipApi();

        MembershipDao membershipDao = mock(MembershipDao.class);
        MembershipOptionSubscriptionDao subscriptionDao = mock(MembershipOptionSubscriptionDao.class);
        MembershipOptionDao membershipOptionDao = mock(MembershipOptionDao.class);

        Membership m = new Membership();
        m.setId(3);

        MembershipOptionSubscription sub = new MembershipOptionSubscription();
        sub.setMembership(m);

        when(membershipDao.all(any(SeasonScope.class))).thenReturn(new ArrayList<>(List.of(m)));
        when(subscriptionDao.findByMembershipIds(any())).thenReturn(List.of(sub));

        inject(api, "membershipDao", membershipDao);
        inject(api, "membershipOptionSubscriptionDao", subscriptionDao);
        inject(api, "membershipOptionDao", membershipOptionDao);
        inject(api, "clubSeasonFilter", mockClubSeasonFilter());

        JteHtml html = api.list(null);
        @SuppressWarnings("unchecked")
        Map<String, List<MembershipOptionSubscription>> subscriptionsByMembership =
                (Map<String, List<MembershipOptionSubscription>>) html.getModel().get("membershipSubscriptions");

        assertTrue(subscriptionsByMembership.containsKey("3"));
        assertEquals(1, subscriptionsByMembership.get("3").size());
    }

    // --- createPage ---

    @Test
    void createPageShouldReturnEmptyMembershipWithStatuses() throws Exception {
        MembershipApi api = new MembershipApi();

        inject(api, "membershipDao", mock(MembershipDao.class));
        inject(api, "membershipOptionSubscriptionDao", mock(MembershipOptionSubscriptionDao.class));
        inject(api, "membershipOptionDao", mock(MembershipOptionDao.class));
        LicenseDao licenseDao = mock(LicenseDao.class);
        when(licenseDao.all()).thenReturn(List.of());
        inject(api, "licenseDao", licenseDao);

        JteHtml html = api.createPage();
        Map<String, Object> model = html.getModel();

        assertEquals("membership/membershipNew.jte", html.getTemplate());
        assertTrue(model.get("membership") instanceof Membership);
        MembershipStatus[] statuses = (MembershipStatus[]) model.get("statuses");
        assertEquals(MembershipStatus.values().length, statuses.length);
    }

    // --- create ---

    @Test
    void createShouldPersistMembershipAndRedirect() throws Exception {
        MembershipApi api = new MembershipApi();

        MembershipDao membershipDao = mock(MembershipDao.class);

        inject(api, "membershipDao", membershipDao);
        inject(api, "membershipOptionSubscriptionDao", mock(MembershipOptionSubscriptionDao.class));
        inject(api, "membershipOptionDao", mock(MembershipOptionDao.class));
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/membership")));

        Response response = api.create("user@test.com", "A12345", "Doe", "John", "2000-01-01", "APPROVED", 1500, null);

        assertEquals(303, response.getStatus());
        verify(membershipDao).persist(any(Membership.class));
    }

    @Test
    void createShouldDefaultNullAmountCentsToZero() throws Exception {
        MembershipApi api = new MembershipApi();

        MembershipDao membershipDao = mock(MembershipDao.class);
        List<Membership> captured = new ArrayList<>();
        org.mockito.Mockito.doAnswer(inv -> {
            captured.add(inv.getArgument(0));
            return null;
        }).when(membershipDao).persist(any(Membership.class));

        inject(api, "membershipDao", membershipDao);
        inject(api, "membershipOptionSubscriptionDao", mock(MembershipOptionSubscriptionDao.class));
        inject(api, "membershipOptionDao", mock(MembershipOptionDao.class));
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/membership")));

        api.create("user@test.com", "A12345", "Doe", "John", "2000-01-01", null, null, null);

        assertEquals(1, captured.size());
        assertEquals(0, captured.get(0).getAmountCents());
        assertEquals(MembershipStatus.PENDING_APPROVAL, captured.get(0).getStatus());
    }

    @Test
    void createShouldPersistLicenseType() throws Exception {
        MembershipApi api = new MembershipApi();

        MembershipDao membershipDao = mock(MembershipDao.class);
        List<Membership> captured = new ArrayList<>();
        org.mockito.Mockito.doAnswer(inv -> {
            captured.add(inv.getArgument(0));
            return null;
        }).when(membershipDao).persist(any(Membership.class));

        inject(api, "membershipDao", membershipDao);
        inject(api, "membershipOptionSubscriptionDao", mock(MembershipOptionSubscriptionDao.class));
        inject(api, "membershipOptionDao", mock(MembershipOptionDao.class));
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/membership")));

        api.create("user@test.com", "A12345", "Doe", "John", "2000-01-01", "APPROVED", 1500, "a");

        assertEquals(1, captured.size());
        assertEquals("A", captured.get(0).getLicenseType());
    }

    @Test
    void createShouldDefaultLicenseTypeToAWhenBlank() throws Exception {
        MembershipApi api = new MembershipApi();

        MembershipDao membershipDao = mock(MembershipDao.class);
        List<Membership> captured = new ArrayList<>();
        org.mockito.Mockito.doAnswer(inv -> {
            captured.add(inv.getArgument(0));
            return null;
        }).when(membershipDao).persist(any(Membership.class));

        inject(api, "membershipDao", membershipDao);
        inject(api, "membershipOptionSubscriptionDao", mock(MembershipOptionSubscriptionDao.class));
        inject(api, "membershipOptionDao", mock(MembershipOptionDao.class));
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/membership")));

        api.create("user@test.com", "A12345", "Doe", "John", "2000-01-01", "APPROVED", 1500, "");

        assertEquals(1, captured.size());
        assertEquals("A", captured.get(0).getLicenseType());
    }

    // --- editPage ---

    @Test
    void editPageShouldReturnEditTemplateWithMembership() throws Exception {
        MembershipApi api = new MembershipApi();

        MembershipDao membershipDao = mock(MembershipDao.class);
        MembershipOptionSubscriptionDao subscriptionDao = mock(MembershipOptionSubscriptionDao.class);
        MembershipOptionDao membershipOptionDao = mock(MembershipOptionDao.class);
        LicenseDao licenseDao = mock(LicenseDao.class);

        Membership m = new Membership();
        m.setId(10);

        when(membershipDao.find(10)).thenReturn(m);
        when(subscriptionDao.findByMembershipIds(any())).thenReturn(List.of());
        when(membershipOptionDao.all()).thenReturn(List.of());
        when(licenseDao.all()).thenReturn(List.of());

        inject(api, "membershipDao", membershipDao);
        inject(api, "membershipOptionSubscriptionDao", subscriptionDao);
        inject(api, "membershipOptionDao", membershipOptionDao);
        inject(api, "licenseDao", licenseDao);

        JteHtml html = api.editPage(10);
        Map<String, Object> model = html.getModel();

        assertEquals("membership/membershipEdit.jte", html.getTemplate());
        assertEquals(m, model.get("membership"));
    }

    @Test
    void editPageShouldThrowNotFoundWhenMembershipMissing() throws Exception {
        MembershipApi api = new MembershipApi();

        MembershipDao membershipDao = mock(MembershipDao.class);
        when(membershipDao.find(99)).thenReturn(null);

        inject(api, "membershipDao", membershipDao);
        inject(api, "membershipOptionSubscriptionDao", mock(MembershipOptionSubscriptionDao.class));
        inject(api, "membershipOptionDao", mock(MembershipOptionDao.class));

        assertThrows(NotFoundException.class, () -> api.editPage(99));
    }

    // --- update ---

    @Test
    void updateShouldMergeAndRedirect() throws Exception {
        MembershipApi api = new MembershipApi();

        MembershipDao membershipDao = mock(MembershipDao.class);
        Membership m = new Membership();
        m.setId(5);
        when(membershipDao.find(5)).thenReturn(m);

        inject(api, "membershipDao", membershipDao);
        inject(api, "membershipOptionSubscriptionDao", mock(MembershipOptionSubscriptionDao.class));
        inject(api, "membershipOptionDao", mock(MembershipOptionDao.class));
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/membership")));

        Response response = api.update(5, "user@test.com", "B99", "Smith", "Jane", "1990-05-05", "PAID", 2000, "B");

        assertEquals(303, response.getStatus());
        verify(membershipDao).merge(m);
    }

    @Test
    void updateShouldThrowNotFoundWhenMembershipMissing() throws Exception {
        MembershipApi api = new MembershipApi();

        MembershipDao membershipDao = mock(MembershipDao.class);
        when(membershipDao.find(77)).thenReturn(null);

        inject(api, "membershipDao", membershipDao);
        inject(api, "membershipOptionSubscriptionDao", mock(MembershipOptionSubscriptionDao.class));
        inject(api, "membershipOptionDao", mock(MembershipOptionDao.class));

        assertThrows(NotFoundException.class,
                () -> api.update(77, "u", "n", "l", "f", "d", "APPROVED", 100, null));
    }

    // --- addOption ---

    @Test
    void addOptionShouldPersistSubscriptionAndRedirect() throws Exception {
        MembershipApi api = new MembershipApi();

        MembershipDao membershipDao = mock(MembershipDao.class);
        MembershipOptionDao optionDao = mock(MembershipOptionDao.class);
        MembershipOptionSubscriptionDao subscriptionDao = mock(MembershipOptionSubscriptionDao.class);

        Membership m = new Membership();
        m.setId(3);
        m.setNrFfe("A111");
        MembershipOption opt = new MembershipOption();
        opt.setId(7);

        when(membershipDao.find(3)).thenReturn(m);
        when(optionDao.find(7)).thenReturn(opt);

        inject(api, "membershipDao", membershipDao);
        inject(api, "membershipOptionDao", optionDao);
        inject(api, "membershipOptionSubscriptionDao", subscriptionDao);
        inject(api, "uriInfo", mockUriInfoMultiPath(URI.create("http://localhost:8080/membership/3/edit")));

        Response response = api.addOption(3, 7);

        assertEquals(303, response.getStatus());
        verify(subscriptionDao).persist(any(MembershipOptionSubscription.class));
    }

    @Test
    void addOptionShouldThrowNotFoundWhenMembershipMissing() throws Exception {
        MembershipApi api = new MembershipApi();

        MembershipDao membershipDao = mock(MembershipDao.class);
        when(membershipDao.find(1)).thenReturn(null);

        inject(api, "membershipDao", membershipDao);
        inject(api, "membershipOptionDao", mock(MembershipOptionDao.class));
        inject(api, "membershipOptionSubscriptionDao", mock(MembershipOptionSubscriptionDao.class));

        assertThrows(NotFoundException.class, () -> api.addOption(1, 2));
    }

    @Test
    void addOptionShouldThrowNotFoundWhenOptionMissing() throws Exception {
        MembershipApi api = new MembershipApi();

        MembershipDao membershipDao = mock(MembershipDao.class);
        MembershipOptionDao optionDao = mock(MembershipOptionDao.class);

        Membership m = new Membership();
        m.setId(1);
        when(membershipDao.find(1)).thenReturn(m);
        when(optionDao.find(99)).thenReturn(null);

        inject(api, "membershipDao", membershipDao);
        inject(api, "membershipOptionDao", optionDao);
        inject(api, "membershipOptionSubscriptionDao", mock(MembershipOptionSubscriptionDao.class));

        assertThrows(NotFoundException.class, () -> api.addOption(1, 99));
    }

    // --- removeOption ---

    @Test
    void removeOptionShouldRemoveAndRedirect() throws Exception {
        MembershipApi api = new MembershipApi();

        MembershipOptionSubscriptionDao subscriptionDao = mock(MembershipOptionSubscriptionDao.class);
        MembershipDao membershipDao = mock(MembershipDao.class);

        Membership m = new Membership();
        m.setId(4);

        MembershipOptionSubscription sub = new MembershipOptionSubscription();
        sub.setId(8);
        sub.setMembership(m);

        when(subscriptionDao.find(8)).thenReturn(sub);

        inject(api, "membershipDao", membershipDao);
        inject(api, "membershipOptionDao", mock(MembershipOptionDao.class));
        inject(api, "membershipOptionSubscriptionDao", subscriptionDao);
        inject(api, "uriInfo", mockUriInfoMultiPath(URI.create("http://localhost:8080/membership/4/edit")));

        Response response = api.removeOption(4, 8);

        assertEquals(303, response.getStatus());
        verify(subscriptionDao).remove(8);
    }

    @Test
    void removeOptionShouldThrowNotFoundWhenSubscriptionMissing() throws Exception {
        MembershipApi api = new MembershipApi();

        MembershipOptionSubscriptionDao subscriptionDao = mock(MembershipOptionSubscriptionDao.class);
        when(subscriptionDao.find(55)).thenReturn(null);

        inject(api, "membershipDao", mock(MembershipDao.class));
        inject(api, "membershipOptionDao", mock(MembershipOptionDao.class));
        inject(api, "membershipOptionSubscriptionDao", subscriptionDao);

        assertThrows(NotFoundException.class, () -> api.removeOption(1, 55));
    }

    @Test
    void removeOptionShouldThrowBadRequestWhenMembershipMismatch() throws Exception {
        MembershipApi api = new MembershipApi();

        MembershipOptionSubscriptionDao subscriptionDao = mock(MembershipOptionSubscriptionDao.class);

        Membership wrong = new Membership();
        wrong.setId(999);

        MembershipOptionSubscription sub = new MembershipOptionSubscription();
        sub.setId(10);
        sub.setMembership(wrong);

        when(subscriptionDao.find(10)).thenReturn(sub);

        inject(api, "membershipDao", mock(MembershipDao.class));
        inject(api, "membershipOptionDao", mock(MembershipOptionDao.class));
        inject(api, "membershipOptionSubscriptionDao", subscriptionDao);

        assertThrows(BadRequestException.class, () -> api.removeOption(1, 10));
    }

    // --- delete ---

    @Test
    void deleteShouldRemoveSubscriptionsAndMembershipAndRedirect() throws Exception {
        MembershipApi api = new MembershipApi();

        MembershipDao membershipDao = mock(MembershipDao.class);
        MembershipOptionSubscriptionDao subscriptionDao = mock(MembershipOptionSubscriptionDao.class);

        Membership membership = new Membership();
        membership.setId(4);

        MembershipOptionSubscription sub = new MembershipOptionSubscription();
        sub.setId(8);
        sub.setMembership(membership);

        when(membershipDao.find(4)).thenReturn(membership);
        when(subscriptionDao.findByMembershipIds(List.of(4))).thenReturn(List.of(sub));

        inject(api, "membershipDao", membershipDao);
        inject(api, "membershipOptionDao", mock(MembershipOptionDao.class));
        inject(api, "membershipOptionSubscriptionDao", subscriptionDao);
        inject(api, "uriInfo", mockUriInfoMultiPath(URI.create("http://localhost:8080/membership?seasonId=2")));

        Response response = api.delete(4, 2);

        assertEquals(303, response.getStatus());
        verify(subscriptionDao).remove(8);
        verify(membershipDao).remove(4);
        assertEquals(URI.create("http://localhost:8080/membership?seasonId=2"), response.getLocation());
    }

    @Test
    void deleteShouldThrowNotFoundWhenMembershipMissing() throws Exception {
        MembershipApi api = new MembershipApi();

        MembershipDao membershipDao = mock(MembershipDao.class);
        when(membershipDao.find(99)).thenReturn(null);

        inject(api, "membershipDao", membershipDao);
        inject(api, "membershipOptionDao", mock(MembershipOptionDao.class));
        inject(api, "membershipOptionSubscriptionDao", mock(MembershipOptionSubscriptionDao.class));

        assertThrows(NotFoundException.class, () -> api.delete(99, null));
    }

    @Test
    void exportCsvShouldIncludeMembershipsAndOptions() throws Exception {
        MembershipApi api = new MembershipApi();

        MembershipDao membershipDao = mock(MembershipDao.class);
        MembershipOptionSubscriptionDao subscriptionDao = mock(MembershipOptionSubscriptionDao.class);

        Membership membership = new Membership();
        membership.setId(42);
        membership.setUser("user@test.com");
        membership.setNrFfe("A12345");
        membership.setLastname("Dupont");
        membership.setFirstname("Jean");
        membership.setBirthDate("2000-01-01");
        membership.setStatus(MembershipStatus.PAID);
        membership.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        membership.setUpdatedAt(LocalDateTime.of(2026, 2, 1, 12, 30));

        MembershipOption option = new MembershipOption();
        option.setOptionValue("Licence adulte");

        MembershipOptionSubscription subscription = new MembershipOptionSubscription();
        subscription.setMembership(membership);
        subscription.setMembershipOption(option);

        when(membershipDao.all(any(SeasonScope.class))).thenReturn(List.of(membership));
        when(subscriptionDao.findByMembershipIds(any())).thenReturn(List.of(subscription));

        inject(api, "membershipDao", membershipDao);
        inject(api, "membershipOptionSubscriptionDao", subscriptionDao);
        inject(api, "clubSeasonFilter", mockClubSeasonFilter());

        Response response = api.exportCsv(null);
        String csv = (String) response.getEntity();

        assertEquals("attachment; filename=memberships.csv", response.getHeaderString("Content-Disposition"));
        assertTrue(csv.startsWith("id;User;Nr FFE;Nom;Prénom;Date de naissance;Status;Options;Created;Updated\n"));
        assertTrue(csv.contains("42;\"user@test.com\";\"A12345\";\"Dupont\";\"Jean\";\"2000-01-01\";\"PAID\";\"Licence adulte\";"));
    }

    @Test
    void exportPdfShouldUseSelectedSeason() throws Exception {
        MembershipApi api = new MembershipApi();
        MembershipDao membershipDao = mock(MembershipDao.class);
        MembershipOptionSubscriptionDao subscriptionDao = mock(MembershipOptionSubscriptionDao.class);
        LicenseDao licenseDao = mock(LicenseDao.class);
        ClubSeasonFilter seasonFilter = mockClubSeasonFilter();
        MembershipReportService reportService = mock(MembershipReportService.class);
        SeasonScope scope = SeasonScope.all();
        byte[] pdf = "%PDF-test".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        when(seasonFilter.resolve(3)).thenReturn(scope);
        when(membershipDao.all(scope)).thenReturn(List.of());
        when(subscriptionDao.findByMembershipIds(any())).thenReturn(List.of());
        when(licenseDao.all()).thenReturn(List.of());
        when(reportService.generate(List.of(), Map.of(), Map.of(), scope)).thenReturn(pdf);

        inject(api, "membershipDao", membershipDao);
        inject(api, "membershipOptionSubscriptionDao", subscriptionDao);
        inject(api, "licenseDao", licenseDao);
        inject(api, "clubSeasonFilter", seasonFilter);
        inject(api, "membershipReportService", reportService);
        inject(api, "licensePriceService", mock(LicensePriceService.class));

        Response response = api.exportPdf(3);

        assertEquals(200, response.getStatus());
        assertSame(pdf, response.getEntity());
        assertTrue(response.getHeaderString("Content-Disposition").startsWith("attachment; filename=adhesions-"));
        verify(membershipDao).all(scope);
        verify(reportService).generate(List.of(), Map.of(), Map.of(), scope);
    }

    @Test
    void exportPdfShouldCountPersistedLicenseType() throws Exception {
        MembershipApi api = new MembershipApi();
        MembershipDao membershipDao = mock(MembershipDao.class);
        MembershipOptionSubscriptionDao subscriptionDao = mock(MembershipOptionSubscriptionDao.class);
        LicenseDao licenseDao = mock(LicenseDao.class);
        ClubSeasonFilter seasonFilter = mockClubSeasonFilter();
        MembershipReportService reportService = mock(MembershipReportService.class);
        SeasonScope scope = SeasonScope.all();
        byte[] pdf = "%PDF-test".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        Membership membership = new Membership();
        membership.setId(1);
        membership.setLicenseType("A");
        membership.setAmountCents(4000);
        membership.setBirthDate("2000-01-01");
        membership.setStatus(MembershipStatus.APPROVED);

        when(seasonFilter.resolve(null)).thenReturn(scope);
        when(membershipDao.all(scope)).thenReturn(List.of(membership));
        when(subscriptionDao.findByMembershipIds(any())).thenReturn(List.of());
        when(licenseDao.all()).thenReturn(List.of(new License("A"), new License("B")));
        when(reportService.generate(
                        eq(List.of(membership)),
                        eq(Map.of()),
                        org.mockito.ArgumentMatchers.any(),
                        eq(scope)))
                .thenReturn(pdf);

        inject(api, "membershipDao", membershipDao);
        inject(api, "membershipOptionSubscriptionDao", subscriptionDao);
        inject(api, "licenseDao", licenseDao);
        inject(api, "clubSeasonFilter", seasonFilter);
        inject(api, "membershipReportService", reportService);
        inject(api, "licensePriceService", mock(LicensePriceService.class));

        Response response = api.exportPdf(null);

        assertEquals(200, response.getStatus());
        org.mockito.ArgumentCaptor<Map<String, MembershipSummaryLine>> summaryCaptor =
                org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(reportService)
                .generate(
                        eq(List.of(membership)),
                        eq(Map.of()),
                        summaryCaptor.capture(),
                        eq(scope));
        assertEquals(1, summaryCaptor.getValue().get("Licence A").count());
        assertEquals(1, summaryCaptor.getValue().get("Licence A").approvedCount());
        assertEquals(4000, summaryCaptor.getValue().get("Licence A").amountCents());
        assertEquals(4000, summaryCaptor.getValue().get("Licence A").approvedAmountCents());
    }

    @Test
    void exportPdfShouldCountPersistedLicenseTypeB() throws Exception {
        MembershipApi api = new MembershipApi();
        MembershipDao membershipDao = mock(MembershipDao.class);
        MembershipOptionSubscriptionDao subscriptionDao = mock(MembershipOptionSubscriptionDao.class);
        LicenseDao licenseDao = mock(LicenseDao.class);
        ClubSeasonFilter seasonFilter = mockClubSeasonFilter();
        MembershipReportService reportService = mock(MembershipReportService.class);
        SeasonScope scope = SeasonScope.all();
        byte[] pdf = "%PDF-test".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        Membership membership = new Membership();
        membership.setId(1);
        membership.setLicenseType("B");
        membership.setAmountCents(2500);
        membership.setBirthDate("2000-01-01");
        membership.setStatus(MembershipStatus.APPROVED);

        when(seasonFilter.resolve(null)).thenReturn(scope);
        when(membershipDao.all(scope)).thenReturn(List.of(membership));
        when(subscriptionDao.findByMembershipIds(any())).thenReturn(List.of());
        when(licenseDao.all()).thenReturn(List.of(new License("A"), new License("B")));
        when(reportService.generate(
                        eq(List.of(membership)),
                        eq(Map.of()),
                        org.mockito.ArgumentMatchers.any(),
                        eq(scope)))
                .thenReturn(pdf);

        inject(api, "membershipDao", membershipDao);
        inject(api, "membershipOptionSubscriptionDao", subscriptionDao);
        inject(api, "licenseDao", licenseDao);
        inject(api, "clubSeasonFilter", seasonFilter);
        inject(api, "membershipReportService", reportService);
        inject(api, "licensePriceService", mock(LicensePriceService.class));

        api.exportPdf(null);

        org.mockito.ArgumentCaptor<Map<String, MembershipSummaryLine>> summaryCaptor =
                org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(reportService)
                .generate(
                        eq(List.of(membership)),
                        eq(Map.of()),
                        summaryCaptor.capture(),
                        eq(scope));
        assertEquals(1, summaryCaptor.getValue().get("Licence B").count());
        assertEquals(1, summaryCaptor.getValue().get("Licence B").approvedCount());
        assertEquals(2500, summaryCaptor.getValue().get("Licence B").amountCents());
        assertEquals(2500, summaryCaptor.getValue().get("Licence B").approvedAmountCents());
    }

    @Test
    void exportPdfShouldInferLicenseBWhenTypeMissing() throws Exception {
        MembershipApi api = new MembershipApi();
        MembershipDao membershipDao = mock(MembershipDao.class);
        MembershipOptionSubscriptionDao subscriptionDao = mock(MembershipOptionSubscriptionDao.class);
        LicenseDao licenseDao = mock(LicenseDao.class);
        LicensePriceService licensePriceService = mock(LicensePriceService.class);
        ClubSeasonFilter seasonFilter = mockClubSeasonFilter();
        MembershipReportService reportService = mock(MembershipReportService.class);
        SeasonScope scope = SeasonScope.all();
        byte[] pdf = "%PDF-test".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        Membership membership = new Membership();
        membership.setId(1);
        membership.setAmountCents(2500);
        membership.setBirthDate("2000-01-01");

        when(seasonFilter.resolve(null)).thenReturn(scope);
        when(membershipDao.all(scope)).thenReturn(List.of(membership));
        when(subscriptionDao.findByMembershipIds(any())).thenReturn(List.of());
        when(licenseDao.all()).thenReturn(List.of(new License("A"), new License("B")));
        when(licensePriceService.getLicensePrice(org.mockito.ArgumentMatchers.anyString(), eq('B')))
                .thenReturn(2500);
        when(reportService.generate(
                        eq(List.of(membership)),
                        eq(Map.of()),
                        org.mockito.ArgumentMatchers.any(),
                        eq(scope)))
                .thenReturn(pdf);

        inject(api, "membershipDao", membershipDao);
        inject(api, "membershipOptionSubscriptionDao", subscriptionDao);
        inject(api, "licenseDao", licenseDao);
        inject(api, "clubSeasonFilter", seasonFilter);
        inject(api, "membershipReportService", reportService);
        inject(api, "licensePriceService", licensePriceService);

        api.exportPdf(null);

        org.mockito.ArgumentCaptor<Map<String, MembershipSummaryLine>> summaryCaptor =
                org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(reportService)
                .generate(
                        eq(List.of(membership)),
                        eq(Map.of()),
                        summaryCaptor.capture(),
                        eq(scope));
        assertEquals(1, summaryCaptor.getValue().get("Licence B").count());
    }

    @Test
    void exportPdfShouldDefaultLicenseATypeWhenMissingAndAmountUnknown() throws Exception {
        MembershipApi api = new MembershipApi();
        MembershipDao membershipDao = mock(MembershipDao.class);
        MembershipOptionSubscriptionDao subscriptionDao = mock(MembershipOptionSubscriptionDao.class);
        LicenseDao licenseDao = mock(LicenseDao.class);
        LicensePriceService licensePriceService = mock(LicensePriceService.class);
        ClubSeasonFilter seasonFilter = mockClubSeasonFilter();
        MembershipReportService reportService = mock(MembershipReportService.class);
        SeasonScope scope = SeasonScope.all();
        byte[] pdf = "%PDF-test".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        Membership membership = new Membership();
        membership.setId(1);
        membership.setAmountCents(16400);
        membership.setBirthDate("2000-01-01");

        when(seasonFilter.resolve(null)).thenReturn(scope);
        when(membershipDao.all(scope)).thenReturn(List.of(membership));
        when(subscriptionDao.findByMembershipIds(any())).thenReturn(List.of());
        when(licenseDao.all()).thenReturn(List.of(new License("A"), new License("B")));
        when(reportService.generate(
                        eq(List.of(membership)),
                        eq(Map.of()),
                        org.mockito.ArgumentMatchers.any(),
                        eq(scope)))
                .thenReturn(pdf);

        inject(api, "membershipDao", membershipDao);
        inject(api, "membershipOptionSubscriptionDao", subscriptionDao);
        inject(api, "licenseDao", licenseDao);
        inject(api, "clubSeasonFilter", seasonFilter);
        inject(api, "membershipReportService", reportService);
        inject(api, "licensePriceService", licensePriceService);

        api.exportPdf(null);

        org.mockito.ArgumentCaptor<Map<String, MembershipSummaryLine>> summaryCaptor =
                org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(reportService)
                .generate(
                        eq(List.of(membership)),
                        eq(Map.of()),
                        summaryCaptor.capture(),
                        eq(scope));
        assertEquals(1, summaryCaptor.getValue().get("Licence A").count());
        assertEquals(16400, summaryCaptor.getValue().get("Licence A").amountCents());
        assertEquals(0, summaryCaptor.getValue().get("Licence A").approvedAmountCents());
    }

    @Test
    void exportPdfShouldSumApprovedAmountOnly() throws Exception {
        MembershipApi api = new MembershipApi();
        MembershipDao membershipDao = mock(MembershipDao.class);
        MembershipOptionSubscriptionDao subscriptionDao = mock(MembershipOptionSubscriptionDao.class);
        LicenseDao licenseDao = mock(LicenseDao.class);
        ClubSeasonFilter seasonFilter = mockClubSeasonFilter();
        MembershipReportService reportService = mock(MembershipReportService.class);
        SeasonScope scope = SeasonScope.all();
        byte[] pdf = "%PDF-test".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        Membership approved = new Membership();
        approved.setId(1);
        approved.setLicenseType("A");
        approved.setAmountCents(4000);
        approved.setBirthDate("2000-01-01");
        approved.setStatus(MembershipStatus.APPROVED);

        Membership pending = new Membership();
        pending.setId(2);
        pending.setLicenseType("B");
        pending.setAmountCents(2500);
        pending.setBirthDate("2000-01-01");
        pending.setStatus(MembershipStatus.PENDING_APPROVAL);

        when(seasonFilter.resolve(null)).thenReturn(scope);
        when(membershipDao.all(scope)).thenReturn(List.of(approved, pending));
        when(subscriptionDao.findByMembershipIds(any())).thenReturn(List.of());
        when(licenseDao.all()).thenReturn(List.of(new License("A"), new License("B")));
        when(reportService.generate(
                        eq(List.of(approved, pending)),
                        eq(Map.of()),
                        org.mockito.ArgumentMatchers.any(),
                        eq(scope)))
                .thenReturn(pdf);

        inject(api, "membershipDao", membershipDao);
        inject(api, "membershipOptionSubscriptionDao", subscriptionDao);
        inject(api, "licenseDao", licenseDao);
        inject(api, "clubSeasonFilter", seasonFilter);
        inject(api, "membershipReportService", reportService);
        inject(api, "licensePriceService", mock(LicensePriceService.class));

        api.exportPdf(null);

        org.mockito.ArgumentCaptor<Map<String, MembershipSummaryLine>> summaryCaptor =
                org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(reportService)
                .generate(
                        eq(List.of(approved, pending)),
                        eq(Map.of()),
                        summaryCaptor.capture(),
                        eq(scope));
        assertEquals(4000, summaryCaptor.getValue().get("Licence A").approvedAmountCents());
        assertEquals(1, summaryCaptor.getValue().get("Licence A").approvedCount());
        assertEquals(2500, summaryCaptor.getValue().get("Licence B").amountCents());
        assertEquals(0, summaryCaptor.getValue().get("Licence B").approvedAmountCents());
        assertEquals(0, summaryCaptor.getValue().get("Licence B").approvedCount());
    }

    // --- helpers ---

    private static ClubSeasonFilter mockClubSeasonFilter() {
        ClubSeasonFilter filter = mock(ClubSeasonFilter.class);
        when(filter.resolve(any())).thenReturn(SeasonScope.all());
        doAnswer(invocation -> {
            Map<String, Object> model = invocation.getArgument(0);
            model.put("seasons", List.of());
            model.put("seasonId", invocation.getArgument(1));
            return null;
        }).when(filter).addToModel(any(), any());
        return filter;
    }

    private static UriInfo mockUriInfo(URI target) {
        UriInfo uriInfo = mock(UriInfo.class);
        UriBuilder uriBuilder = mock(UriBuilder.class);
        when(uriInfo.getBaseUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(any(String.class))).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(target);
        return uriInfo;
    }

    private static UriInfo mockUriInfoMultiPath(URI target) {
        UriInfo uriInfo = mock(UriInfo.class);
        UriBuilder uriBuilder = mock(UriBuilder.class);
        when(uriInfo.getBaseUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(any(String.class))).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(target);
        return uriInfo;
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName + " not found in " + target.getClass().getName());
    }
}

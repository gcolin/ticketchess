package com.github.gcolin.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.player.IPlayer;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.github.gcolin.payment.Payment;
import com.github.gcolin.payment.PaymentType;
import com.github.gcolin.player.Club;
import com.github.gcolin.player.Find;
import com.github.gcolin.platform.TestContext;

class StatisticsReportServiceTest {

    @Test
    void generateCsvShouldExportPaidPlayersWithClubOrFederation() throws Exception {
        StatisticsReportService service = new StatisticsReportService();
        Find find = mock(Find.class);

        IPlayer frenchPlayer = mock(IPlayer.class);
        when(frenchPlayer.getClub()).thenReturn("Club Lannion");
        when(frenchPlayer.getClubRef()).thenReturn("CL001");
        when(frenchPlayer.getFederation()).thenReturn("FFE");
        when(frenchPlayer.getCategory()).thenReturn("SenM");
        when(frenchPlayer.getBirthDate()).thenReturn("1990-05-12");

        IPlayer foreignPlayer = mock(IPlayer.class);
        when(foreignPlayer.getClub()).thenReturn("");
        when(foreignPlayer.getClubRef()).thenReturn(null);
        when(foreignPlayer.getFederation()).thenReturn("FIDE");
        when(foreignPlayer.getCategory()).thenReturn("SenF");
        when(foreignPlayer.getBirthDate()).thenReturn(null);

        PlayerSubscription paidFrench = new PlayerSubscription();
        paidFrench.setStatus(PlayerSubscriptionStatus.PAID);
        paidFrench.setNrFfe("A12345");

        PlayerSubscription paidForeign = new PlayerSubscription();
        paidForeign.setStatus(PlayerSubscriptionStatus.PAID);
        paidForeign.setNrFfe("B99999");

        PlayerSubscription notPaid = new PlayerSubscription();
        notPaid.setStatus(PlayerSubscriptionStatus.NOT_PAID);
        notPaid.setNrFfe("C11111");

        when(find.player("A12345", null)).thenReturn(frenchPlayer);
        when(find.player("B99999", null)).thenReturn(foreignPlayer);

        try (TestContext ignored = TestContext.open(find)) {
        String csv = service.generateCsv(List.of(paidFrench, paidForeign, notPaid), false);

        assertTrue(csv.startsWith("Licence;"));
        assertTrue(csv.contains("A12345;Club Lannion;SenM;"));
        assertTrue(csv.contains("B99999;FIDE;SenF;"));
        assertEquals(3, csv.lines().count());
        }
    }

    @Test
    void computeForEventCollectionShouldListPlayersRegisteredInMultipleEvents() throws Exception {
        StatisticsReportService service = new StatisticsReportService();
        Find find = mock(Find.class);

        IPlayer player = mock(IPlayer.class);
        when(player.getName()).thenReturn("Dupont");
        when(player.getFirstname()).thenReturn("Jean");
        when(find.player("A12345", null)).thenReturn(player);

        com.github.gcolin.event.Event eventA = new com.github.gcolin.event.Event();
        eventA.setName("Rapide");

        com.github.gcolin.event.Event eventB = new com.github.gcolin.event.Event();
        eventB.setName("Blitz");

        com.github.gcolin.event.Event eventC = new com.github.gcolin.event.Event();
        eventC.setName("Classique");

        PlayerSubscription subA = new PlayerSubscription();
        subA.setStatus(PlayerSubscriptionStatus.PAID);
        subA.setNrFfe("A12345");
        subA.setEvent(eventA);

        PlayerSubscription subB = new PlayerSubscription();
        subB.setStatus(PlayerSubscriptionStatus.NOT_PAID);
        subB.setNrFfe("A12345");
        subB.setEvent(eventB);

        PlayerSubscription subC = new PlayerSubscription();
        subC.setStatus(PlayerSubscriptionStatus.PAID);
        subC.setNrFfe("B99999");
        subC.setEvent(eventC);

        try (TestContext ignored = TestContext.open(find)) {
        StatisticsReport report = service.computeForEventCollection(List.of(subA, subB, subC));

        assertEquals(1, report.getMultiEventPlayers().size());
        Map<String, Object> row = report.getMultiEventPlayers().get(0);
        assertEquals("A12345", row.get("licence"));
        assertEquals("Dupont", row.get("name"));
        assertEquals("Jean", row.get("firstname"));
        assertEquals("Blitz, Rapide", row.get("events"));
        }
    }

    @Test
    void computeForEventShouldAggregateAmountByPaymentType() throws Exception {
        StatisticsReportService service = new StatisticsReportService();
        Find find = mock(Find.class);

        IPlayer player = mock(IPlayer.class);
        when(player.getClub()).thenReturn("ClubA");
        when(player.getCategory()).thenReturn("SenM");
        when(player.getFederation()).thenReturn("FFE");
        when(player.getBirthDate()).thenReturn("1990-01-01");
        when(player.getName()).thenReturn("Martin");
        when(player.getFirstname()).thenReturn("Paul");
        when(find.player("A12345", null)).thenReturn(player);
        when(find.player("B99999", null)).thenReturn(player);
        when(find.player("C11111", null)).thenReturn(player);

        com.github.gcolin.payment.Payment card = new com.github.gcolin.payment.Payment();
        card.setType(com.github.gcolin.payment.PaymentType.CARD);

        com.github.gcolin.payment.Payment transfer = new com.github.gcolin.payment.Payment();
        transfer.setType(com.github.gcolin.payment.PaymentType.BANK_TRANSFER);

        PlayerSubscription cardSub = new PlayerSubscription();
        cardSub.setStatus(PlayerSubscriptionStatus.PAID);
        cardSub.setNrFfe("A12345");
        cardSub.setAmountCents(7000L);
        cardSub.setPayment(card);

        PlayerSubscription transferSub = new PlayerSubscription();
        transferSub.setStatus(PlayerSubscriptionStatus.PAID);
        transferSub.setNrFfe("B99999");
        transferSub.setAmountCents(3000L);
        transferSub.setPayment(transfer);

        PlayerSubscription unknownSub = new PlayerSubscription();
        unknownSub.setStatus(PlayerSubscriptionStatus.PAID);
        unknownSub.setNrFfe("C11111");
        unknownSub.setAmountCents(1000L);

        com.github.gcolin.payment.Payment stripeWithoutType = new com.github.gcolin.payment.Payment();
        stripeWithoutType.setStripeSessionId("cs_live_abc");
        PlayerSubscription legacyStripeSub = new PlayerSubscription();
        legacyStripeSub.setStatus(PlayerSubscriptionStatus.PAID);
        legacyStripeSub.setNrFfe("D22222");
        legacyStripeSub.setAmountCents(2000L);
        legacyStripeSub.setPayment(stripeWithoutType);

        PlayerSubscription unpaidWithAmount = new PlayerSubscription();
        unpaidWithAmount.setStatus(PlayerSubscriptionStatus.NOT_PAID);
        unpaidWithAmount.setNrFfe("E33333");
        unpaidWithAmount.setAmountCents(5000L);

        when(find.player("D22222", null)).thenReturn(player);
        when(find.player("E33333", null)).thenReturn(player);

        try (TestContext ignored = TestContext.open(find)) {
        StatisticsReport report =
                service.computeForEvent(
                        new com.github.gcolin.event.Event(),
                        List.of(cardSub, transferSub, unknownSub, legacyStripeSub, unpaidWithAmount));

        assertEquals(130.0, report.getTotalAmount(), 0.001);
        assertEquals(90.0, report.getAmountByPaymentType().get("CARD"), 0.001);
        assertEquals(30.0, report.getAmountByPaymentType().get("BANK_TRANSFER"), 0.001);
        assertEquals(10.0, report.getAmountByPaymentType().get("UNKNOWN"), 0.001);
        assertEquals(1, report.getUnknownPaymentPlayers().size());
        Map<String, Object> unknown = report.getUnknownPaymentPlayers().get(0);
        assertEquals("C11111", unknown.get("licence"));
        assertEquals("Martin", unknown.get("name"));
        assertEquals("Paul", unknown.get("firstname"));
        assertEquals(10.0, (Double) unknown.get("amount"), 0.001);
        }
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

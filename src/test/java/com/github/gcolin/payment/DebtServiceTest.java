package com.github.gcolin.payment;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.gcolin.event.Event;
import com.github.gcolin.event.EventCache;
import com.github.gcolin.platform.Caches;
import com.github.gcolin.platform.SendMail;
import com.github.gcolin.player.Find;
import com.github.gcolin.player.IPlayer;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import com.github.gcolin.registration.PlayerSubscriptionOptionDao;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DebtServiceTest {

    private DebtService debtService;
    private Caches caches;
    private PlayerSubscriptionDao playerSubscriptionService;
    private PlayerSubscriptionOptionDao playerSubscriptionOptionService;
    private Find find;
    private SendMail sendMail;
    private PaymentDao paymentService;

    @BeforeEach
    void setUp() {
        caches = Mockito.mock(Caches.class);
        playerSubscriptionService = Mockito.mock(PlayerSubscriptionDao.class);
        playerSubscriptionOptionService = Mockito.mock(PlayerSubscriptionOptionDao.class);
        find = Mockito.mock(Find.class);
        sendMail = Mockito.mock(SendMail.class);
        paymentService = Mockito.mock(PaymentDao.class);

        debtService = new DebtService();
        debtService.setPlayerSubscriptionDao(playerSubscriptionService);
        debtService.setPlayerSubscriptionOptionDao(playerSubscriptionOptionService);
        debtService.setFind(find);
        debtService.setSendMail(sendMail);
        debtService.setCaches(caches);
        debtService.setPaymentDao(paymentService);
    }

    @Test
    public void testCalculateDebtReturnsSumForNotPaidSubscriptions() {
        Event event = new Event();
        event.setPriceCents(1250L);

        PlayerSubscription subscription = new PlayerSubscription();
        subscription.setNrFfe("12345");
        subscription.setStatus(PlayerSubscriptionStatus.NOT_PAID);
        subscription.setEvent(event);

        IPlayer player = Mockito.mock(IPlayer.class);
        Mockito.when(player.getFideTitre()).thenReturn(null);
        Mockito.when(player.isYoung()).thenReturn(false);
        Mockito.when(find.player("12345", null)).thenReturn(player);
        Mockito.when(playerSubscriptionService.findByCreationUser("bob@example.com"))
                .thenReturn(List.of(subscription));
        Mockito.when(playerSubscriptionOptionService.findNotPaidByCreationUser("bob@example.com"))
                .thenReturn(List.of());

        double result = debtService.calculateDebt("bob@example.com");

        Assertions.assertEquals(12.5, result);
    }

    @Test
    public void testCalculateDebtIgnoresPaidSubscriptions() {
        Event event = new Event();
        event.setPriceCents(1500L);

        PlayerSubscription paidSubscription = new PlayerSubscription();
        paidSubscription.setNrFfe("12345");
        paidSubscription.setStatus(PlayerSubscriptionStatus.PAID);
        paidSubscription.setEvent(event);

        Mockito.when(playerSubscriptionService.findByCreationUser("alice@example.com"))
                .thenReturn(List.of(paidSubscription));
        Mockito.when(playerSubscriptionOptionService.findNotPaidByCreationUser("alice@example.com"))
                .thenReturn(List.of());

        double result = debtService.calculateDebt("alice@example.com");

        Assertions.assertEquals(0.0, result);
        Mockito.verifyNoInteractions(find);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPaymentCompletesSubscriptionAndInvalidatesCaches() throws Exception {
        Cache<String, EventCache> eventCache = Mockito.mock(Cache.class);
        Cache<String, Double> debtCache = Mockito.mock(Cache.class);

        Event event = new Event();
        event.setId(1);
        event.setName("Tournoi");
        event.setStartDate(LocalDateTime.of(2024, 4, 1, 10, 0));
        event.setPriceCents(2000L);

        PlayerSubscription subscription = new PlayerSubscription();
        subscription.setNrFfe("12345");
        subscription.setStatus(PlayerSubscriptionStatus.NOT_PAID);
        subscription.setEvent(event);

        IPlayer player = Mockito.mock(IPlayer.class);
        Mockito.when(player.getFideTitre()).thenReturn(null);
        Mockito.when(player.isYoung()).thenReturn(false);
        Mockito.when(player.getFirstname()).thenReturn("Bob");
        Mockito.when(player.getName()).thenReturn("Martin");
        Mockito.when(find.player("12345", null)).thenReturn(player);
        Mockito.when(playerSubscriptionService.findByCreationUserWithEvents("bob@example.com"))
                .thenReturn(List.of(subscription));
        Mockito.when(playerSubscriptionOptionService.findNotPaidByCreationUser("bob@example.com"))
                .thenReturn(List.of());
        Mockito.when(caches.getEvent()).thenReturn(eventCache);
        Mockito.when(caches.getDebtCache()).thenReturn(debtCache);
        Mockito.when(eventCache.getIfPresent("1")).thenReturn(new EventCache());

        debtService.payment(30.0, "bob@example.com", "sess-1", "123");

        Assertions.assertEquals(PlayerSubscriptionStatus.PAID, subscription.getStatus());
        Mockito.verify(playerSubscriptionService).persist(subscription);
        Mockito.verify(sendMail)
                .send(
                        Mockito.any(PaymentMail.class),
                        Mockito.eq("bob@example.com"),
                        Mockito.eq("Confirmation paiement Tournoi"));
        Mockito.verify(eventCache).invalidate("1");
        Mockito.verify(debtCache).invalidateAll();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPaymentDoesNotPersistWhenAmountIsInsufficient() throws Exception {
        Cache<String, EventCache> eventCache = Mockito.mock(Cache.class);
        Cache<String, Double> debtCache = Mockito.mock(Cache.class);

        Event event = new Event();
        event.setId(2);
        event.setName("Rapide");
        event.setStartDate(LocalDateTime.of(2024, 6, 1, 10, 0));
        event.setPriceCents(5000L);

        PlayerSubscription subscription = new PlayerSubscription();
        subscription.setNrFfe("54321");
        subscription.setStatus(PlayerSubscriptionStatus.NOT_PAID);
        subscription.setEvent(event);

        IPlayer player = Mockito.mock(IPlayer.class);
        Mockito.when(player.getFideTitre()).thenReturn(null);
        Mockito.when(player.isYoung()).thenReturn(false);
        Mockito.when(player.getFirstname()).thenReturn("Alice");
        Mockito.when(player.getName()).thenReturn("Durand");
        Mockito.when(find.player("54321", null)).thenReturn(player);
        Mockito.when(playerSubscriptionService.findByCreationUserWithEvents("alice@example.com"))
                .thenReturn(List.of(subscription));
        Mockito.when(playerSubscriptionOptionService.findNotPaidByCreationUser("alice@example.com"))
                .thenReturn(List.of());
        Mockito.when(caches.getEvent()).thenReturn(eventCache);
        Mockito.when(caches.getDebtCache()).thenReturn(debtCache);
        Mockito.when(eventCache.getIfPresent("2")).thenReturn(null);
        Mockito.when(paymentService.findBySessionId("sess-2")).thenReturn(null);

        debtService.payment(10.0, "alice@example.com", "sess-2", null);

        Assertions.assertEquals(PlayerSubscriptionStatus.NOT_PAID, subscription.getStatus());
        Mockito.verify(playerSubscriptionService, Mockito.never()).persist(Mockito.any());
        Mockito.verify(paymentService, Mockito.never()).persist(Mockito.any());
        Mockito.verify(sendMail, Mockito.never()).send(Mockito.any(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(eventCache, Mockito.never()).invalidate(Mockito.anyString());
        Mockito.verify(debtCache).invalidateAll();
    }
}

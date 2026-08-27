package com.github.gcolin.registration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.gcolin.platform.Config;
import com.github.gcolin.event.Event;
import com.github.gcolin.event.EventCollection;
import com.github.gcolin.event.EventOption;
import com.github.gcolin.event.EventOptionType;
import com.github.gcolin.event.EventCollectionOptionType;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.player.IPlayer;
import com.github.gcolin.event.EventDao;
import com.github.gcolin.event.EventCollectionOptionDao;
import com.github.gcolin.event.EventOptionDao;
import com.github.gcolin.payment.PaymentDao;
import com.github.gcolin.registration.PlayerPendingSubscriptionDao;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import com.github.gcolin.platform.AbstractMail;
import com.github.gcolin.platform.SendMail;
import com.github.gcolin.event.EventCache;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.github.gcolin.platform.Caches;
import com.github.gcolin.player.Find;

class RegisterServiceTest {

    private RegisterService registerService;
    private SendMail sendMail;
    private Properties properties;
    private PlayerSubscriptionDao playerSubscriptionService;
    private PlayerPendingSubscriptionDao playerPendingSubscriptionDao;
    private Find find;
    private EventDao eventService;
    private EventOptionDao eventOptionService;
    private EventCollectionOptionDao eventCollectionOptionService;
    private Caches caches;
    private AbstractMail abstractMail;
    private PaymentDao paymentService;
    private Config config;

    @BeforeEach
    void setUp() {
        sendMail = mock(SendMail.class);
        properties = mock(Properties.class);
        playerSubscriptionService = mock(PlayerSubscriptionDao.class);
        playerPendingSubscriptionDao = mock(PlayerPendingSubscriptionDao.class);
        find = mock(Find.class);
        eventService = mock(EventDao.class);
        eventOptionService = mock(EventOptionDao.class);
        eventCollectionOptionService = mock(EventCollectionOptionDao.class);
        caches = mock(Caches.class);
        abstractMail = mock(AbstractMail.class);
        paymentService = mock(PaymentDao.class);
        config = mock(Config.class);

        registerService = new RegisterService();
        registerService.setSendMail(sendMail);
        registerService.setProperties(properties);
        registerService.setPlayerSubscriptionDao(playerSubscriptionService);
        registerService.setPlayerPendingSubscriptionDao(playerPendingSubscriptionDao);
        registerService.setFind(find);
        registerService.setEventDao(eventService);
        registerService.setEventOptionDao(eventOptionService);
        registerService.setEventCollectionOptionDao(eventCollectionOptionService);
        registerService.setCaches(caches);
        registerService.setPaymentDao(paymentService);
        registerService.setConfig(config);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testRegisterPlayerToEvent() throws Exception {
        Event event = new Event();
        event.setId(1);
        event.setName("Chess Event");
        event.setStartDate(LocalDateTime.now());

        IPlayer player = mock(IPlayer.class);
        when(player.getNrffe()).thenReturn("123456");
        when(player.getFirstname()).thenReturn("John");
        when(player.getName()).thenReturn("Doe");

        when(eventService.find(1)).thenReturn(event);
        when(find.player("123456", null)).thenReturn(player);
        when(properties.getProperty("baseurl")).thenReturn("http://example.com");
        when(config.getKeys()).thenReturn(testSecretKey());

        when(playerSubscriptionService.detach(any(PlayerSubscription.class)))
                .then(invocation -> invocation.getArgument(0));

        Cache<String, EventCache> eventCache = mock(Cache.class);
        when(caches.getEvent()).thenReturn(eventCache);

        PlayerSubscription result = registerService.registerPlayerToEvent(1, "123456", "john@example.com");

        Assertions.assertNotNull(result);
        Assertions.assertEquals("123456", result.getNrFfe());
        Assertions.assertEquals("john@example.com", result.getCreationUser());
        verify(playerSubscriptionService).persist(result);
        verify(sendMail).send(any(), eq("john@example.com"), eq("Confirmation inscription Chess Event"));
        verify(eventCache).invalidateAll();
    }

    @Test
    void testRegisterPlayerToEventShouldCreatePendingWhenEventIsFull() throws Exception {
        Event event = new Event();
        event.setId(1);

        IPlayer player = mock(IPlayer.class);
        when(player.getNrffe()).thenReturn("123456");

        when(eventService.find(1)).thenReturn(event);
        when(find.player("123456", null)).thenReturn(player);
        when(eventOptionService.findIntOptionValue(1, com.github.gcolin.event.EventOptionType.MAX_SUBSCRIPTIONS))
                .thenReturn(1);
        when(playerSubscriptionService.countByEvent(event)).thenReturn(1L);

        PlayerSubscription result = registerService.registerPlayerToEvent(1, "123456", "john@example.com");

        Assertions.assertNull(result);
        verify(playerPendingSubscriptionDao).persist(any(com.github.gcolin.registration.PlayerPendingSubscription.class));
        verify(playerSubscriptionService, never()).persist(any(PlayerSubscription.class));
    }

    @Test
    void testRegisterPlayerToEventShouldCreatePendingWhenLegacyMaxSubscriptionsIsReached() throws Exception {
        Event event = new Event();
        event.setId(1);
        EventOption maxSubscriptionsOption = new EventOption();
        maxSubscriptionsOption.setOptionType(EventOptionType.MAX_SUBSCRIPTIONS);
        maxSubscriptionsOption.setValue("1");
        Map<EventOptionType, EventOption> eventOptions = new EnumMap<>(EventOptionType.class);
        eventOptions.put(EventOptionType.MAX_SUBSCRIPTIONS, maxSubscriptionsOption);
        event.setEventOptions(eventOptions);

        IPlayer player = mock(IPlayer.class);
        when(player.getNrffe()).thenReturn("123456");

        when(eventService.find(1)).thenReturn(event);
        when(find.player("123456", null)).thenReturn(player);
        when(eventOptionService.findIntOptionValue(1, com.github.gcolin.event.EventOptionType.MAX_SUBSCRIPTIONS))
                .thenReturn(null);
        when(playerSubscriptionService.countByEvent(event)).thenReturn(1L);

        PlayerSubscription result = registerService.registerPlayerToEvent(1, "123456", "john@example.com");

        Assertions.assertNull(result);
        verify(playerPendingSubscriptionDao).persist(any(com.github.gcolin.registration.PlayerPendingSubscription.class));
        verify(playerSubscriptionService, never()).persist(any(PlayerSubscription.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testUnregisterPlayerToEvent() throws Exception {
        Event event = new Event();
        event.setId(1);
        event.setName("Chess Event");
        event.setStartDate(LocalDateTime.now());

        PlayerSubscription ps = new PlayerSubscription();
        ps.setId(10);
        ps.setCreationUser("john@example.com");

        IPlayer player = mock(IPlayer.class);
        when(player.getFirstname()).thenReturn("John");
        when(player.getName()).thenReturn("Doe");

        when(eventService.find(1)).thenReturn(event);
        when(playerSubscriptionService.findByEventAndNrffe(event, "123456")).thenReturn(ps);
        when(find.player("123456", null)).thenReturn(player);
        when(properties.getProperty("baseurl")).thenReturn("http://example.com");
        when(config.getKeys()).thenReturn(testSecretKey());

        Cache<String, EventCache> eventCache = mock(Cache.class);
        Cache<String, Double> debtCache = mock(Cache.class);
        when(caches.getEvent()).thenReturn(eventCache);
        when(caches.getDebtCache()).thenReturn(debtCache);

        boolean result = registerService.unregisterPlayerToEvent(1, "123456");

        Assertions.assertTrue(result);
        verify(sendMail).send(any(), eq("john@example.com"), eq("Confirmation annulation Chess Event"));
        verify(eventCache).invalidateAll();
        verify(debtCache).invalidateAll();
    }

    @Test
    void testUnregisterPlayerToEventShouldPromotePendingPlayer() throws Exception {
        Event event = new Event();
        event.setId(1);
        event.setName("Chess Event");
        event.setStartDate(LocalDateTime.now());

        PlayerSubscription ps = new PlayerSubscription();
        ps.setId(10);
        ps.setCreationUser("john@example.com");

        com.github.gcolin.registration.PlayerPendingSubscription pending = new com.github.gcolin.registration.PlayerPendingSubscription();
        pending.setCreationUser("queued@example.com");
        pending.setNrFfe("654321");
        pending.setEvent(event);

        IPlayer cancelledPlayer = mock(IPlayer.class);
        when(cancelledPlayer.getFirstname()).thenReturn("John");
        when(cancelledPlayer.getName()).thenReturn("Doe");

        IPlayer queuedPlayer = mock(IPlayer.class);
        when(queuedPlayer.getNrffe()).thenReturn("654321");
        when(queuedPlayer.getFirstname()).thenReturn("Jane");
        when(queuedPlayer.getName()).thenReturn("Smith");

        when(eventService.find(1)).thenReturn(event);
        when(playerSubscriptionService.findByEventAndNrffe(event, "123456")).thenReturn(ps);
        when(find.player("123456", null)).thenReturn(cancelledPlayer);
        when(find.player("654321", null)).thenReturn(queuedPlayer);
        when(playerPendingSubscriptionDao.findOldestByEvent(event)).thenReturn(pending, (com.github.gcolin.registration.PlayerPendingSubscription) null);
        when(playerSubscriptionService.countByEvent(event)).thenReturn(0L);
        when(properties.getProperty("baseurl")).thenReturn("http://example.com");
        when(config.getKeys()).thenReturn(testSecretKey());

        Cache<String, EventCache> eventCache = mock(Cache.class);
        Cache<String, Double> debtCache = mock(Cache.class);
        when(caches.getEvent()).thenReturn(eventCache);
        when(caches.getDebtCache()).thenReturn(debtCache);

        boolean result = registerService.unregisterPlayerToEvent(1, "123456");

        Assertions.assertTrue(result);
        verify(playerSubscriptionService).persist(any(PlayerSubscription.class));
        verify(playerPendingSubscriptionDao).remove(pending);
        verify(sendMail).send(any(), eq("queued@example.com"), eq("Confirmation inscription Chess Event"));
    }

    @Test
    void testRegisterPlayerToEventShouldRejectWhenCollectionIsFull() throws Exception {
        EventCollection eventCollection = new EventCollection();
        eventCollection.setId(4);

        Event event = new Event();
        event.setId(1);
        event.setName("Chess Event");
        event.setStartDate(LocalDateTime.now());
        event.setEventCollection(eventCollection);

        IPlayer player = mock(IPlayer.class);
        when(player.getNrffe()).thenReturn("123456");

        when(eventService.find(1)).thenReturn(event);
        when(find.player("123456", null)).thenReturn(player);
        when(eventCollectionOptionService.findIntOptionValue(4, EventCollectionOptionType.MAX_SUBSCRIPTIONS)).thenReturn(1);
        when(playerSubscriptionService.countByEventCollection(4)).thenReturn(1L);

        PlayerSubscription result = registerService.registerPlayerToEvent(1, "123456", "john@example.com");

        Assertions.assertNull(result);
        verify(playerSubscriptionService, never()).persist(any(PlayerSubscription.class));
    }

    @Test
    void testRegisterPlayerToEventShouldRejectWhenAlreadyRegisteredInCollection() throws Exception {
        EventCollection eventCollection = new EventCollection();
        eventCollection.setId(4);

        Event event = new Event();
        event.setId(1);
        event.setName("Chess Event");
        event.setStartDate(LocalDateTime.now());
        event.setEventCollection(eventCollection);

        IPlayer player = mock(IPlayer.class);
        when(player.getNrffe()).thenReturn("123456");

        when(eventService.find(1)).thenReturn(event);
        when(find.player("123456", null)).thenReturn(player);
        when(playerSubscriptionService.existsActiveByEventCollectionAndNrffe(4, "123456")).thenReturn(true);

        PlayerSubscription result = registerService.registerPlayerToEvent(1, "123456", "john@example.com");

        Assertions.assertNull(result);
        verify(playerSubscriptionService, never()).persist(any(PlayerSubscription.class));
        verify(playerPendingSubscriptionDao, never())
                .persist(any(com.github.gcolin.registration.PlayerPendingSubscription.class));
    }

    @Test
    void testUnregisterPlayerToEventShouldPromotePendingPlayerFromAnotherEventInCollection() throws Exception {
        EventCollection eventCollection = new EventCollection();
        eventCollection.setId(4);

        Event eventA = new Event();
        eventA.setId(1);
        eventA.setName("Chess Event A");
        eventA.setStartDate(LocalDateTime.now());
        eventA.setEventCollection(eventCollection);

        Event eventB = new Event();
        eventB.setId(2);
        eventB.setName("Chess Event B");
        eventB.setStartDate(LocalDateTime.now().plusDays(1));
        eventB.setEventCollection(eventCollection);

        PlayerSubscription cancelledSubscription = new PlayerSubscription();
        cancelledSubscription.setId(10);
        cancelledSubscription.setCreationUser("john@example.com");

        com.github.gcolin.registration.PlayerPendingSubscription pending = new com.github.gcolin.registration.PlayerPendingSubscription();
        pending.setCreationUser("queued@example.com");
        pending.setNrFfe("654321");
        pending.setEvent(eventB);

        IPlayer cancelledPlayer = mock(IPlayer.class);
        when(cancelledPlayer.getFirstname()).thenReturn("John");
        when(cancelledPlayer.getName()).thenReturn("Doe");

        IPlayer queuedPlayer = mock(IPlayer.class);
        when(queuedPlayer.getNrffe()).thenReturn("654321");
        when(queuedPlayer.getFirstname()).thenReturn("Jane");
        when(queuedPlayer.getName()).thenReturn("Smith");

        when(eventService.find(1)).thenReturn(eventA);
        when(playerSubscriptionService.findByEventAndNrffe(eventA, "123456")).thenReturn(cancelledSubscription);
        when(find.player("123456", null)).thenReturn(cancelledPlayer);
        when(find.player("654321", null)).thenReturn(queuedPlayer);
        when(eventCollectionOptionService.findIntOptionValue(4, EventCollectionOptionType.MAX_SUBSCRIPTIONS))
                .thenReturn(2);
        when(playerSubscriptionService.countByEventCollection(4)).thenReturn(1L, 2L);
        when(playerPendingSubscriptionDao.findByEventCollection(4)).thenReturn(List.of(pending), List.of());
        when(playerSubscriptionService.countByEvent(eventB)).thenReturn(0L);
        when(properties.getProperty("baseurl")).thenReturn("http://example.com");
        when(config.getKeys()).thenReturn(testSecretKey());

        Cache<String, EventCache> eventCache = mock(Cache.class);
        Cache<String, Double> debtCache = mock(Cache.class);
        when(caches.getEvent()).thenReturn(eventCache);
        when(caches.getDebtCache()).thenReturn(debtCache);

        boolean result = registerService.unregisterPlayerToEvent(1, "123456");

        Assertions.assertTrue(result);
        verify(playerSubscriptionService).persist(any(PlayerSubscription.class));
        verify(playerPendingSubscriptionDao).remove(pending);
        verify(sendMail).send(any(), eq("queued@example.com"), eq("Confirmation inscription Chess Event B"));
    }

    private SecretKey testSecretKey() {
        return Keys.hmacShaKeyFor(
                "0123456789012345678901234567890101234567890123456789012345678901"
                        .getBytes(StandardCharsets.UTF_8));
    }
}

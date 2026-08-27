package com.github.gcolin.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.gcolin.platform.TestContext;
import com.github.gcolin.player.Find;
import com.github.gcolin.player.DisplayPlayer;
import com.github.gcolin.player.IPlayer;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventServiceTest {

    private EntityManager em;
    private EventDao eventService;
    private EventOptionDao eventOptionDao;
    private Find finder;
    private EventInfoDao eventInfoService;
    private EventGroupDao eventGroupService;
    private TestContext testContext;

    @BeforeEach
    void setUp() {
        em = mock(EntityManager.class);
        eventOptionDao = mock(EventOptionDao.class);
        finder = mock(Find.class);
        eventInfoService = mock(EventInfoDao.class);
        eventGroupService = mock(EventGroupDao.class);
        testContext = TestContext.open(em);
        eventService = TestContext.createDao(EventDao.class, em);
        eventService.setFinder(finder);
        eventService.setEventInfoDao(() -> eventInfoService);
        eventService.setEventGroupDao(() -> eventGroupService);
        eventService.setEventOptionDao(() -> eventOptionDao);
    }

    @AfterEach
    void tearDown() {
        testContext.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    void testFindByStatus() {
        TypedQuery<Event> query = mock(TypedQuery.class);

        Event event = new Event();
        event.setId(1);
        event.setName("Chess Tournament 2024");
        event.setStatus(EventStatus.ACTIVE);

        when(em.createQuery(
                        "SELECT e FROM Event e where e.status = :status and e.eventGroup is null order by e.startDate ASC, e.name ASC",
                        Event.class))
                .thenReturn(query);
        when(query.setParameter("status", EventStatus.ACTIVE)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(event));

        List<Event> result = eventService.findByStatus(EventStatus.ACTIVE);

        Assertions.assertEquals(1, result.size());
        Assertions.assertSame(event, result.get(0));
        verify(query).setParameter("status", EventStatus.ACTIVE);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testFindByStatusWithEventGroup() {
        TypedQuery<Event> query = mock(TypedQuery.class);

        EventGroup eventGroup = new EventGroup();
        eventGroup.setId(10);

        Event event = new Event();
        event.setId(2);
        event.setName("Group Tournament");
        event.setStatus(EventStatus.ACTIVE);
        event.setEventGroup(eventGroup);

        when(em.createQuery(
                        "SELECT e FROM Event e where e.status = :status and e.eventGroup = :eventGroup order by e.startDate ASC, e.name DESC",
                        Event.class))
                .thenReturn(query);
        when(query.setParameter("status", EventStatus.ACTIVE)).thenReturn(query);
        when(query.setParameter("eventGroup", eventGroup)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(event));

        List<Event> result = eventService.findByStatus(EventStatus.ACTIVE, eventGroup);

        Assertions.assertEquals(1, result.size());
        Assertions.assertSame(event, result.get(0));
        verify(query).setParameter("status", EventStatus.ACTIVE);
        verify(query).setParameter("eventGroup", eventGroup);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testSaveEvent() {
        TypedQuery<EventGroup> query = mock(TypedQuery.class);
        when(em.createQuery("SELECT e FROM EventGroup e where e.id = :id", EventGroup.class))
                .thenReturn(query);

        doAnswer(invocation -> {
                    Event event = invocation.getArgument(0);
                    event.setId(5);
                    return null;
                })
                .when(em)
                .persist(any(Event.class));

        Integer id = eventService.saveEvent(
                null,
                "New Tournament",
                "2024-06-01",
                "2024-06-03",
                "ACTIVE",
                "BLITZ",
                "15.0",
                "10.0",
                null,
                null,
                "7",
                "90",
                "Haley",
                "CLUB1",
                null);

        Assertions.assertEquals(5, id);
        verify(eventOptionDao).setOption(5, EventOptionType.ROUNDS, "7");
        verify(eventOptionDao).setOption(5, EventOptionType.CADENCE, "90");
        verify(eventOptionDao).setOption(5, EventOptionType.PAIRING, "Haley");
        verify(eventOptionDao).setOption(5, EventOptionType.CLUB_REF, "CLUB1");
        verify(eventOptionDao).setOption(5, EventOptionType.MAX_SUBSCRIPTIONS, "");
    }

    @Test
    void fillNbSubscriptionsShouldUseBatchCount() {
        PlayerSubscriptionDao subscriptionDao = mock(PlayerSubscriptionDao.class);
        eventService.setPlayerSubscriptionDao(() -> subscriptionDao);

        Event event1 = new Event();
        event1.setId(1);
        Event event2 = new Event();
        event2.setId(2);
        Event event3 = new Event();
        event3.setId(3);

        when(subscriptionDao.countByEventIds(List.of(1, 2, 3))).thenReturn(Map.of(1, 5L, 2, 10L));

        eventService.fillNbSubscriptions(List.of(event1, event2, event3));

        Assertions.assertEquals(5, event1.getNbSubscriptions());
        Assertions.assertEquals(10, event2.getNbSubscriptions());
        Assertions.assertEquals(0, event3.getNbSubscriptions());
        Assertions.assertTrue(event1.getSubscriptions().isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testBuildCache() {
        Event event = new Event();
        event.setId(1);
        event.setName("Test Event");
        event.setEventType(EventType.STANDARD);
        event.setSubscriptions(Collections.emptyList());

        PlayerSubscription sub = new PlayerSubscription();
        sub.setNrFfe("123");
        sub.setId(1);
        sub.setStatus(PlayerSubscriptionStatus.PAID);

        event.setSubscriptions(List.of(sub));

        IPlayer player = mock(IPlayer.class);
        when(player.getNrffe()).thenReturn("123");
        when(player.getName()).thenReturn("Doe");
        when(player.getFirstname()).thenReturn("John");
        when(player.getRating()).thenReturn("1500");

        TypedQuery<Event> query = mock(TypedQuery.class);
        when(em.createQuery(anyString(), eq(Event.class))).thenReturn(query);
        when(query.setParameter("id", 1)).thenReturn(query);
        when(query.getResultStream()).thenReturn(Stream.of(event));

        when(finder.player("123", EventType.STANDARD)).thenReturn(player);

        EventInfo eventInfo = new EventInfo();
        eventInfo.setDescription("**Test** description");
        when(eventInfoService.find(event)).thenReturn(eventInfo);

        doAnswer(invocation -> invocation.getArgument(0)).when(em).detach(any());

        EventCache cache = eventService.buildCache(1);

        Assertions.assertNotNull(cache);
        Assertions.assertSame(event, cache.event);
        Assertions.assertEquals(1, cache.players.size());
        DisplayPlayer dp = cache.players.get(0);
        Assertions.assertEquals("Doe", dp.getName());
        Assertions.assertEquals("John", dp.getFirstname());
        Assertions.assertEquals("1500", dp.getRating());
        Assertions.assertTrue(cache.missingPlayerCodes.isEmpty());
        Assertions.assertTrue(cache.eventInfo.contains("<strong>Test</strong>"));
        Assertions.assertTrue(cache.event.getSubscriptions().isEmpty());
    }

    @Test
    void buildCacheShouldCollectMissingPlayerCodes() {
        Event event = new Event();
        event.setId(1);
        event.setEventType(EventType.STANDARD);

        PlayerSubscription found = new PlayerSubscription();
        found.setId(1);
        found.setNrFfe("123");
        found.setStatus(PlayerSubscriptionStatus.PAID);

        PlayerSubscription missing = new PlayerSubscription();
        missing.setId(2);
        missing.setNrFfe("MISSING");
        missing.setStatus(PlayerSubscriptionStatus.PAID);

        event.setSubscriptions(List.of(found, missing));

        IPlayer player = mock(IPlayer.class);
        when(player.getNrffe()).thenReturn("123");
        when(player.getName()).thenReturn("Doe");
        when(player.getFirstname()).thenReturn("John");
        when(player.getRating()).thenReturn("1500");

        TypedQuery<Event> query = mock(TypedQuery.class);
        when(em.createQuery(anyString(), eq(Event.class))).thenReturn(query);
        when(query.setParameter("id", 1)).thenReturn(query);
        when(query.getResultStream()).thenReturn(Stream.of(event));

        when(finder.player("123", EventType.STANDARD)).thenReturn(player);
        when(finder.player("MISSING", EventType.STANDARD)).thenReturn(null);
        when(eventInfoService.find(event)).thenReturn(null);
        doAnswer(invocation -> invocation.getArgument(0)).when(em).detach(any());

        EventCache cache = eventService.buildCache(1);

        Assertions.assertEquals(1, cache.players.size());
        Assertions.assertEquals(List.of("MISSING"), cache.missingPlayerCodes);
    }
}

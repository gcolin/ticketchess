package com.github.gcolin.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.gcolin.platform.TestContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventInfoServiceTest {

    private EntityManager em;
    private EventInfoDao eventInfoService;
    private EventDao eventService;
    private TestContext testContext;

    @BeforeEach
    void setUp() {
        em = mock(EntityManager.class);
        eventService = mock(EventDao.class);
        testContext = TestContext.open(em);
        eventInfoService = TestContext.createDao(EventInfoDao.class, em);
        eventInfoService.setEventDao(() -> eventService);
    }

    @AfterEach
    void tearDown() {
        testContext.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    void testFind() {
        TypedQuery<EventInfo> query = mock(TypedQuery.class);

        Event event = new Event();
        event.setId(1);

        EventInfo eventInfo = new EventInfo();
        eventInfo.setDescription("Test description");

        when(em.createQuery("SELECT e FROM EventInfo e where e.event = :event", EventInfo.class))
                .thenReturn(query);
        when(query.setParameter("event", event)).thenReturn(query);
        when(query.getResultStream()).thenReturn(Stream.of(eventInfo));

        EventInfo result = eventInfoService.find(event);

        Assertions.assertSame(eventInfo, result);
        verify(query).setParameter("event", event);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testFindByEventId() {
        TypedQuery<EventInfo> query = mock(TypedQuery.class);

        EventInfo eventInfo = new EventInfo();
        eventInfo.setDescription("Description by ID");

        when(em.createQuery("SELECT e FROM EventInfo e where e.event.id = :id", EventInfo.class))
                .thenReturn(query);
        when(query.setParameter("id", 1)).thenReturn(query);
        when(query.getResultStream()).thenReturn(Stream.of(eventInfo));

        EventInfo result = eventInfoService.findByEventId(1);

        Assertions.assertSame(eventInfo, result);
        verify(query).setParameter("id", 1);
    }

    @Test
    void testSetEventInfo_New() {
        Event event = new Event();
        event.setId(1);

        when(eventService.find(1)).thenReturn(event);

        eventInfoService.setEventInfo(1, "New description");

        verify(eventService).find(1);
    }
}

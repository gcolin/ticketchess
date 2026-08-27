package com.github.gcolin.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.gcolin.platform.TestContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventGroupServiceTest {

    private EntityManager em;
    private EventGroupDao eventGroupService;
    private TestContext testContext;

    @BeforeEach
    void setUp() {
        em = mock(EntityManager.class);
        testContext = TestContext.open(em);
        eventGroupService = TestContext.createDao(EventGroupDao.class, em);
    }

    @AfterEach
    void tearDown() {
        testContext.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    void testFindByShortname() {
        TypedQuery<EventGroup> query = mock(TypedQuery.class);

        EventGroup eventGroup = new EventGroup();
        eventGroup.setShortname("chess2024");

        when(em.createQuery("SELECT e FROM EventGroup e where e.shortname = :shortname", EventGroup.class))
                .thenReturn(query);
        when(query.setParameter("shortname", "chess2024")).thenReturn(query);
        when(query.getResultStream()).thenReturn(Stream.of(eventGroup));

        EventGroup result = eventGroupService.findByShortname("chess2024");

        Assertions.assertSame(eventGroup, result);
        verify(query).setParameter("shortname", "chess2024");
    }

    @SuppressWarnings("unchecked")
    @Test
    void testFindByShortname_NotFound() {
        TypedQuery<EventGroup> query = mock(TypedQuery.class);

        when(em.createQuery("SELECT e FROM EventGroup e where e.shortname = :shortname", EventGroup.class))
                .thenReturn(query);
        when(query.setParameter("shortname", "unknown")).thenReturn(query);
        when(query.getResultStream()).thenReturn(Stream.empty());

        EventGroup result = eventGroupService.findByShortname("unknown");

        Assertions.assertNull(result);
        verify(query).setParameter("shortname", "unknown");
    }

    @SuppressWarnings("unchecked")
    @Test
    void testFindDetachedForEdit() {
        EventGroup eventGroup = new EventGroup();
        eventGroup.setId(42);
        eventGroup.setEvents(List.of());
        eventGroup.setNotifications(List.of());

        TypedQuery<EventGroup> query = mock(TypedQuery.class);

        when(em.createQuery("SELECT e FROM EventGroup e where e.id = :id", EventGroup.class))
                .thenReturn(query);
        when(query.getResultStream()).thenReturn(Stream.of(eventGroup));

        EventGroup result = eventGroupService.findDetachedForEdit(42);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(42, result.getId());
    }
}

package com.github.gcolin.notification;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.gcolin.event.EventDao;
import com.github.gcolin.event.EventGroupDao;
import com.github.gcolin.event.EventStatus;
import com.github.gcolin.platform.TestContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationServiceTest {

    private EntityManager em;
    private NotificationDao notificationService;
    private EventDao eventService;
    private EventGroupDao eventGroupService;
    private TestContext testContext;

    @BeforeEach
    void setUp() {
        em = mock(EntityManager.class);
        eventService = mock(EventDao.class);
        eventGroupService = mock(EventGroupDao.class);
        testContext = TestContext.open(em);
        notificationService = TestContext.createDao(NotificationDao.class, em);
        notificationService.setEventDao(eventService);
        notificationService.setEventGroupDao(eventGroupService);
    }

    @AfterEach
    void tearDown() {
        testContext.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    void testFindByStatus() {
        TypedQuery<Notification> query = mock(TypedQuery.class);

        Notification notification = new Notification();
        notification.setId(1);
        notification.setContent("Test notification");

        when(em.createQuery("SELECT e FROM Notification e where e.event_group = :status", Notification.class))
                .thenReturn(query);
        when(query.setParameter("status", EventStatus.ACTIVE)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(notification));

        List<Notification> result = notificationService.findByStatus(EventStatus.ACTIVE);

        Assertions.assertEquals(1, result.size());
        Assertions.assertSame(notification, result.get(0));
        verify(query).setParameter("status", EventStatus.ACTIVE);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testFindGlobal() {
        TypedQuery<Notification> query = mock(TypedQuery.class);

        Notification notification = new Notification();
        notification.setContent("Global notification");

        when(em.createQuery(
                        "SELECT n FROM Notification n where n.event is NULL and n.eventGroup is NULL",
                        Notification.class))
                .thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(notification));

        List<Notification> result = notificationService.findGlobal();

        Assertions.assertEquals(1, result.size());
        Assertions.assertSame(notification, result.get(0));
    }

    @Test
    void testSetNotification_New() {
        when(eventService.find(1)).thenReturn(null);
        when(eventGroupService.find(2)).thenReturn(null);

        notificationService.setNotification(null, 1, 2, "New content");

        verify(eventService).find(1);
        verify(eventGroupService).find(2);
    }
}

package com.github.gcolin.desk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.gcolin.event.Event;
import com.github.gcolin.payment.Payment;
import com.github.gcolin.payment.PaymentStatus;
import com.github.gcolin.payment.PaymentType;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.registration.PlayerSubscriptionOption;
import com.github.gcolin.desk.EventDeskOp;
import com.github.gcolin.desk.EventDeskPlayerDto;
import com.github.gcolin.event.EventType;
import com.github.gcolin.player.Player;
import com.github.gcolin.registration.PlayerSubscriptionOptionStatus;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import com.github.gcolin.platform.SendMail;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.github.gcolin.platform.Caches;
import com.github.gcolin.platform.TestContext;
import com.github.gcolin.player.Find;

public class EventDeskServiceTest {

    private EventDeskService service;
    private EntityManagerFactory emf;
    private EntityManager em;
    private EntityTransaction tx;
    private Find find;
    private Caches caches;

    @BeforeEach
    void setUp() throws Exception {
        service = new EventDeskService();
        emf = mock(EntityManagerFactory.class);
        em = mock(EntityManager.class);
        tx = mock(EntityTransaction.class);
        find = mock(Find.class);
        caches = new Caches();
        SendMail mail = mock(SendMail.class);

        when(emf.createEntityManager()).thenReturn(em);
        when(em.getTransaction()).thenReturn(tx);
        when(tx.isActive()).thenReturn(false);

        inject(service, "emf", emf);
        inject(service, "caches", caches);
        inject(service, "mail", mail);
    }

    private void withFind(Runnable test) {
        try (TestContext ignored = TestContext.open(em, find)) {
            test.run();
        }
    }

    @Test
    void applyOpsShouldUpdateAttendanceAndStatus() {
        withFind(() -> {
        Event event = new Event();
        event.setId(3);
        PlayerSubscription sub = new PlayerSubscription();
        sub.setId(22);
        sub.setEvent(event);
        sub.setStatus(PlayerSubscriptionStatus.NOT_PAID);
        sub.setNrFfe("A00001");
        sub.setCreationUser("player@example.com");
        sub.setAmountCents(1000L);

        when(em.find(PlayerSubscription.class, 22)).thenReturn(sub);

        EventDeskOp op = new EventDeskOp();
        op.setId("op-1");
        op.setSubId(22);
        op.setPresent(true);
        op.setStatus("PAID");
        op.setClientTs(1L);

        Player player = new Player();
        player.setName("Doe");
        player.setFirstname("John");
        when(find.player(anyString(), any())).thenReturn(player);

        List<String> acked = service.applyOps(3, List.of(op));

        assertEquals(List.of("op-1"), acked);
        assertTrue(sub.getAttendanceAt() != null);
        assertEquals(PlayerSubscriptionStatus.PAID, sub.getStatus());
        assertNotNull(sub.getPayment());
        assertEquals(PaymentType.CASH, sub.getPayment().getType());
        assertEquals(PaymentStatus.PAID, sub.getPayment().getStatus());
        assertEquals(10.0, sub.getPayment().getAmount());
        assertEquals("player@example.com", sub.getPayment().getUserEmail());
        verify(em).persist(sub.getPayment());
        });
    }

    @Test
    void applyOpsShouldCreateCashPaymentForOption() {
        withFind(() -> {
        Event event = new Event();
        event.setId(3);
        PlayerSubscription sub = new PlayerSubscription();
        sub.setId(22);
        sub.setEvent(event);
        sub.setCreationUser("player@example.com");
        sub.setNrFfe("A00001");

        PlayerSubscriptionOption option = new PlayerSubscriptionOption();
        option.setId(55);
        option.setPlayerSubscription(sub);
        option.setStatus(PlayerSubscriptionOptionStatus.NOT_PAID);
        option.setAmountCents(500L);
        option.setDescription("Repas");

        when(em.find(PlayerSubscriptionOption.class, 55)).thenReturn(option);

        EventDeskOp op = new EventDeskOp();
        op.setId("op-opt-1");
        op.setSubId(22);
        op.setOptionId(55);
        op.setStatus("PAID");
        op.setClientTs(1L);

        List<String> acked = service.applyOps(3, List.of(op));

        assertEquals(List.of("op-opt-1"), acked);
        assertEquals(PlayerSubscriptionOptionStatus.PAID, option.getStatus());
        assertNotNull(option.getPayment());
        assertEquals(PaymentType.CASH, option.getPayment().getType());
        assertEquals(PaymentStatus.PAID, option.getPayment().getStatus());
        assertEquals(5.0, option.getPayment().getAmount());
        assertEquals("player@example.com", option.getPayment().getUserEmail());

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(em).persist(paymentCaptor.capture());
        assertEquals(PaymentType.CASH, paymentCaptor.getValue().getType());
        });
    }

    @Test
    void snapshotShouldMapPlayers() {
        withFind(() -> {
        Event event = new Event();
        event.setId(3);
        event.setEventType(EventType.STANDARD);
        event.setPriceCents(1000L);
        event.setYoungPriceCents(500L);

        PlayerSubscription sub = new PlayerSubscription();
        sub.setId(22);
        sub.setEvent(event);
        sub.setStatus(PlayerSubscriptionStatus.NOT_PAID);
        sub.setNrFfe("A00001");
        sub.setAmountCents(1000L);

        @SuppressWarnings("unchecked")
        TypedQuery<PlayerSubscription> subQuery = mock(TypedQuery.class);
        @SuppressWarnings("unchecked")
        TypedQuery<PlayerSubscriptionOption> optionQuery = mock(TypedQuery.class);
        when(em.find(Event.class, 3)).thenReturn(event);
        when(em.createQuery(anyString(), any(Class.class))).thenAnswer(invocation -> {
            Class<?> type = invocation.getArgument(1);
            if (type == PlayerSubscriptionOption.class) {
                return optionQuery;
            }
            return subQuery;
        });
        when(subQuery.setParameter(anyString(), any())).thenReturn(subQuery);
        when(subQuery.getResultList()).thenReturn(List.of(sub));
        when(optionQuery.setParameter(anyString(), any())).thenReturn(optionQuery);
        when(optionQuery.getResultList()).thenReturn(List.of());

        Player player = new Player();
        player.setName("Doe");
        player.setFirstname("John");
        player.setCategory("SenM");
        player.setRating("1800");
        player.setNrffe("A00001");
        when(find.player("A00001", EventType.STANDARD)).thenReturn(player);

        List<EventDeskPlayerDto> dtos = service.snapshot(3);
        assertEquals(1, dtos.size());
        assertEquals(22, dtos.get(0).getSubId());
        assertEquals("Doe", dtos.get(0).getName());
        assertFalse(dtos.get(0).isPresent());
        assertEquals("NOT_PAID", dtos.get(0).getStatus());
        });
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

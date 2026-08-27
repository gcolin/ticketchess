package com.github.gcolin.platform;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class DbInitTest {

    @SuppressWarnings("unchecked")
    @Test
    void testInitEventsDoesNothingWhenDatabaseAlreadyInitialized() {
        EntityManagerFactory emf = Mockito.mock(EntityManagerFactory.class);
        DbInit dbInit = new DbInit(emf);
        EntityManager em = Mockito.mock(EntityManager.class);
        TypedQuery<Long> query = Mockito.mock(TypedQuery.class);
        EntityTransaction tx = Mockito.mock(EntityTransaction.class);

        Mockito.when(emf.createEntityManager()).thenReturn(em);
        Mockito.when(em.createQuery("select count(e) from Event e", Long.class)).thenReturn(query);
        Mockito.when(query.getSingleResult()).thenReturn(1L);
        Mockito.when(em.getTransaction()).thenReturn(tx);

        Assertions.assertDoesNotThrow(dbInit::initEvents);

        Mockito.verify(tx, Mockito.never()).begin();
        Mockito.verify(tx, Mockito.never()).commit();
        Mockito.verify(em, Mockito.never()).persist(ArgumentMatchers.any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testInitEventsInitializesWhenDatabaseIsEmpty() {
        EntityManagerFactory emf = Mockito.mock(EntityManagerFactory.class);
        DbInit dbInit = new DbInit(emf);
        EntityManager em = Mockito.mock(EntityManager.class);
        TypedQuery<Long> query = Mockito.mock(TypedQuery.class);
        EntityTransaction tx = Mockito.mock(EntityTransaction.class);

        Mockito.when(emf.createEntityManager()).thenReturn(em);
        Mockito.when(em.createQuery("select count(e) from Event e", Long.class)).thenReturn(query);
        Mockito.when(query.getSingleResult()).thenReturn(0L);
        Mockito.when(em.getTransaction()).thenReturn(tx);

        Assertions.assertDoesNotThrow(dbInit::initEvents);

        Mockito.verify(tx).begin();
        Mockito.verify(tx).commit();
        Mockito.verify(em, Mockito.times(36)).persist(ArgumentMatchers.any());
    }
}

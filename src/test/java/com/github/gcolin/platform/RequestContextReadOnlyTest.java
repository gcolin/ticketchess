package com.github.gcolin.platform;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RequestContextReadOnlyTest {

    @AfterEach
    void tearDown() {
        RequestContext.close();
    }

    @Test
    void commitReadAndClearCommitsAndClearsPersistenceContext() {
        EntityManager em = Mockito.mock(EntityManager.class);
        EntityTransaction tx = Mockito.mock(EntityTransaction.class);
        Mockito.when(em.getTransaction()).thenReturn(tx);
        Mockito.when(tx.isActive()).thenReturn(true);
        Mockito.when(em.isOpen()).thenReturn(true);

        RequestContext.openForTestReadOnly(em);
        Assertions.assertTrue(RequestContext.isReadOnly());

        RequestContext.commitReadAndClear();

        Mockito.verify(tx).commit();
        Mockito.verify(em).clear();
    }

    @Test
    void touchEmStartsTransactionOnFirstDaoAccessInReadOnlyContext() {
        EntityManager em = Mockito.mock(EntityManager.class);
        EntityTransaction tx = Mockito.mock(EntityTransaction.class);
        Mockito.when(em.getTransaction()).thenReturn(tx);
        Mockito.when(tx.isActive()).thenReturn(false);
        Mockito.when(em.isOpen()).thenReturn(true);

        RequestContext.openForTestReadOnly(em);
        RequestContext.require().licenseDao();

        Mockito.verify(tx).begin();
    }
}

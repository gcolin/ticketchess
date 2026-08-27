package com.github.gcolin.platform;

import jakarta.persistence.EntityManager;
import java.util.function.Supplier;

public final class Transactions {

    private Transactions() {}

    public static void run(EntityManager em, Runnable work) {
        run(em, () -> {
            work.run();
            return null;
        });
    }

    public static <T> T run(EntityManager em, Supplier<T> work) {
        if (em.getTransaction().isActive()) {
            return work.get();
        }
        try {
            em.getTransaction().begin();
            T result = work.get();
            em.flush();
            em.getTransaction().commit();
            return result;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException(e);
        }
    }
}

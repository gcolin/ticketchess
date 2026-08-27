package com.github.gcolin.platform;

import com.github.gcolin.platform.PagedList;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;

public class AbstractDao<T> {

    protected EntityManager em;

    private final Class<T> type;

    public AbstractDao(Class<T> type) {
        this.type = type;
    }

    public void setEm(EntityManager em) {
        this.em = em;
    }

    public List<T> all() {
        TypedQuery<T> query = em.createQuery("SELECT e FROM " + type.getSimpleName() + " e", type);
        return query.getResultList();
    }

    public PagedList<T> page(int start, int pageSize) {
        TypedQuery<T> query = em.createQuery("SELECT e FROM " + type.getSimpleName() + " e order by e.id", type);
        query.setFirstResult(start);
        query.setMaxResults(pageSize);
        List<T> list = query.getResultList();

        TypedQuery<Long> countQuery = em.createQuery("SELECT COUNT(e) FROM " + type.getSimpleName() + " e", Long.class);
        long total = countQuery.getSingleResult();

        return new PagedList<T>(list, start, total);
    }

    public T find(Integer id) {
        TypedQuery<T> query = em.createQuery("SELECT e FROM " + type.getSimpleName() + " e where e.id = :id", type);
        query.setParameter("id", id);
        return query.getResultStream().findFirst().orElse(null);
    }

    public void remove(Integer id) {
        remove(find(id));
    }

    public void persist(T e) {
        em.persist(e);
    }

    public T merge(T e) {
        return em.merge(e);
    }

    public void remove(T e) {
        em.remove(e);
    }

    public <X> List<X> detachAll(List<X> list) {
        for (X e : list) {
            em.detach(e);
        }
        return list;
    }

    public <X> X detach(X e) {
        em.detach(e);
        return e;
    }
}

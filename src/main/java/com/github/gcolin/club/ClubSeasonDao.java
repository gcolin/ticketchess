package com.github.gcolin.club;

import com.github.gcolin.platform.AbstractDao;
import jakarta.persistence.TypedQuery;
import java.util.List;

public class ClubSeasonDao extends AbstractDao<ClubSeason> {

    public ClubSeasonDao() {
        super(ClubSeason.class);
    }

    public List<ClubSeason> allOrderByStartDateDesc() {
        TypedQuery<ClubSeason> query =
                em.createQuery("SELECT s FROM ClubSeason s ORDER BY s.startDate DESC", ClubSeason.class);
        return query.getResultList();
    }

    public ClubSeason findCurrent() {
        TypedQuery<ClubSeason> query = em.createQuery(
                "SELECT s FROM ClubSeason s WHERE s.current = true ORDER BY s.startDate DESC", ClubSeason.class);
        query.setMaxResults(1);
        return query.getResultStream().findFirst().orElse(null);
    }

    public void save(ClubSeason season, boolean markCurrent) {
        if (season.getId() == null) {
            persist(season);
        }
        if (markCurrent) {
            em.createQuery("UPDATE ClubSeason s SET s.current = false WHERE s.id <> :id")
                    .setParameter("id", season.getId())
                    .executeUpdate();
            season.setCurrent(true);
        } else {
            season.setCurrent(false);
        }
        merge(season);
    }
}

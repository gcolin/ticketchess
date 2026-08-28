package com.github.gcolin.club;

import java.time.LocalDateTime;

public final class SeasonScope {

    private final LocalDateTime start;
    private final LocalDateTime end;
    private final Integer seasonId;

    private SeasonScope(LocalDateTime start, LocalDateTime end, Integer seasonId) {
        this.start = start;
        this.end = end;
        this.seasonId = seasonId;
    }

    public static SeasonScope all() {
        return new SeasonScope(null, null, null);
    }

    public static SeasonScope of(ClubSeason season) {
        return new SeasonScope(
                season.getStartDate().atStartOfDay(),
                season.getEndDate().atTime(23, 59, 59),
                season.getId());
    }

    public boolean isFiltered() {
        return start != null && end != null;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public Integer getSeasonId() {
        return seasonId;
    }
}

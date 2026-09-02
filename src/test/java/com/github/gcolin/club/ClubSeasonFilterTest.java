package com.github.gcolin.club;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.gcolin.platform.SelectItem;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClubSeasonFilterTest {

    private ClubSeasonDao clubSeasonDao;
    private ClubSeasonFilter filter;

    @BeforeEach
    void setUp() {
        clubSeasonDao = mock(ClubSeasonDao.class);
        filter = new ClubSeasonFilter();
        filter.setClubSeasonDao(clubSeasonDao);
    }

    @Test
    void resolveShouldUseCurrentSeasonWhenSeasonIdIsMissing() {
        ClubSeason current = season(2, "2026/2027", true);
        when(clubSeasonDao.findCurrent()).thenReturn(current);

        SeasonScope scope = filter.resolve(null);

        assertTrue(scope.isFiltered());
        assertEquals(2, scope.getSeasonId());
    }

    @Test
    void buildSelectItemsShouldSelectCurrentSeasonByDefault() {
        ClubSeason current = season(2, "2026/2027", true);
        when(clubSeasonDao.findCurrent()).thenReturn(current);
        when(clubSeasonDao.allOrderByStartDateDesc()).thenReturn(List.of(current));

        List<SelectItem> items = filter.buildSelectItems(null);

        assertFalse(items.get(0).isSelected());
        assertTrue(items.get(1).isSelected());
    }

    @Test
    void resolveShouldReturnAllSeasonsWhenSeasonIdIsZero() {
        SeasonScope scope = filter.resolve(ClubSeasonFilter.ALL_SEASONS_ID);
        assertFalse(scope.isFiltered());
        assertFalse(scope.isSeasonIdFiltered());
    }

    @Test
    void resolveShouldFilterBySeasonIdWhenExplicitSeasonSelected() {
        ClubSeason season2025 = season(1, "2025", false);
        season2025.setStartDate(LocalDate.of(2025, 9, 1));
        season2025.setEndDate(LocalDate.of(2026, 8, 31));
        when(clubSeasonDao.find(1)).thenReturn(season2025);

        SeasonScope scope = filter.resolve(1);

        assertTrue(scope.isSeasonIdFiltered());
        assertTrue(scope.isFiltered());
        assertEquals(1, scope.getSeasonId());
    }

    @Test
    void effectiveSeasonIdShouldIgnoreAllSeasonsSelection() {
        ClubSeason current = season(2, "2026", true);
        when(clubSeasonDao.findCurrent()).thenReturn(current);

        assertEquals(2, filter.effectiveSeasonId(ClubSeasonFilter.ALL_SEASONS_ID));
    }

    private static ClubSeason season(int id, String name, boolean current) {
        ClubSeason season = new ClubSeason();
        season.setId(id);
        season.setName(name);
        season.setStartDate(LocalDate.of(2026, 9, 1));
        season.setEndDate(LocalDate.of(2027, 8, 31));
        season.setCurrent(current);
        return season;
    }
}

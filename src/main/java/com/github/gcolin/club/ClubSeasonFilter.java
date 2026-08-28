package com.github.gcolin.club;

import com.github.gcolin.platform.SelectItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class ClubSeasonFilter {

    public static final int ALL_SEASONS_ID = 0;

    private ClubSeasonDao clubSeasonDao;

    public void setClubSeasonDao(ClubSeasonDao clubSeasonDao) {
        this.clubSeasonDao = clubSeasonDao;
    }

    public Integer effectiveSeasonId(Integer seasonId) {
        if (seasonId != null) {
            return seasonId;
        }
        ClubSeason current = clubSeasonDao.findCurrent();
        return current != null ? current.getId() : null;
    }

    public SeasonScope resolve(Integer seasonId) {
        if (seasonId != null && seasonId == ALL_SEASONS_ID) {
            return SeasonScope.all();
        }
        if (seasonId != null) {
            ClubSeason season = clubSeasonDao.find(seasonId);
            if (season != null) {
                return SeasonScope.of(season);
            }
            return SeasonScope.all();
        }
        ClubSeason current = clubSeasonDao.findCurrent();
        if (current != null) {
            return SeasonScope.of(current);
        }
        return SeasonScope.all();
    }

    public List<SelectItem> buildSelectItems(Integer seasonId) {
        List<SelectItem> items = new ArrayList<>();
        boolean allSelected = seasonId != null && seasonId == ALL_SEASONS_ID;
        Integer selectedSeasonId = null;
        if (seasonId == null) {
            ClubSeason current = clubSeasonDao.findCurrent();
            if (current != null) {
                selectedSeasonId = current.getId();
            }
        } else if (seasonId != ALL_SEASONS_ID) {
            selectedSeasonId = seasonId;
        }

        SelectItem allItem = new SelectItem();
        allItem.setLabel(getAllSeasonsLabel());
        allItem.setValue(String.valueOf(ALL_SEASONS_ID));
        allItem.setSelected(allSelected);
        items.add(allItem);

        for (ClubSeason season : clubSeasonDao.allOrderByStartDateDesc()) {
            SelectItem item = new SelectItem();
            item.setLabel(season.getName());
            item.setValue(String.valueOf(season.getId()));
            item.setSelected(!allSelected && season.getId().equals(selectedSeasonId));
            items.add(item);
        }
        return items;
    }

    public void addToModel(Map<String, Object> model, Integer seasonId) {
        model.put("seasons", buildSelectItems(seasonId));
        model.put("seasonId", effectiveSeasonId(seasonId));
        model.put("allSeasonsSelected", seasonId != null && seasonId == ALL_SEASONS_ID);
    }

    private String getAllSeasonsLabel() {
        ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.getDefault());
        return bundle.getString("label.allSeasons");
    }
}

package com.github.gcolin.event;

import com.github.gcolin.platform.Caches;
import com.github.gcolin.platform.SelectItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class EventGroupFilter {

    private Caches caches;
    private EventGroupDao eventGroupService;

    public void setCaches(Caches caches) {
        this.caches = caches;
    }

    public void setEventGroupDao(EventGroupDao eventGroupService) {
        this.eventGroupService = eventGroupService;
    }

    public List<SelectItem> getAll(String group) {
        List<SelectItem> list = caches.getEventGroups().getIfPresent(String.valueOf(group));

        if (list == null) {
            List<EventGroup> eventGroups = eventGroupService.all();
            eventGroups.sort((p1, p2) -> {
                return p1.getName().compareTo(p2.getName());
            });
            list = new ArrayList<SelectItem>();
            SelectItem item0 = new SelectItem();
            item0.setLabel(getMainTournamentsLabel());
            item0.setValue("");
            item0.setSelected(group == null);
            list.add(item0);
            for (EventGroup eg : eventGroups) {
                SelectItem item = new SelectItem();
                item.setLabel(eg.getName());
                item.setValue(eg.getShortname());
                item.setSelected(eg.getShortname().equals(group));
                list.add(item);
            }
            caches.getEventGroups().put(String.valueOf(group), list);
        }

        return list;
    }

    private String getMainTournamentsLabel() {
        ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.getDefault());
        return bundle.getString("label.mainTournaments");
    }
}

package com.github.gcolin.event;

import com.github.gcolin.auth.RequireRole;
import com.github.gcolin.club.ClubSeasonFilter;
import com.github.gcolin.club.SeasonScope;
import com.github.gcolin.auth.RoleCode;
import com.github.gcolin.platform.JteHtml;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequireRole(RoleCode.EVENT_ADMIN)
@Path("admin/events")
public class EventAdminApi {

    @Inject
    private EventDao eventDao;

    @Inject
    private EventGroupDao eventGroupDao;

    @Inject
    private ClubSeasonFilter clubSeasonFilter;

    private final EventAdminTreeBuilder treeBuilder = new EventAdminTreeBuilder();

    @GET
    public JteHtml page(@QueryParam("seasonId") Integer seasonId) {
        SeasonScope scope = clubSeasonFilter.resolve(seasonId);
        List<Event> events = eventDao.findAllForAdmin(scope);
        eventDao.fillNbSubscriptions(events);
        eventDao.detachAll(events);

        Map<String, Object> model = new HashMap<>();
        model.put("groups", treeBuilder.build(eventGroupDao.all(), events));
        clubSeasonFilter.addToModel(model, seasonId);
        return new JteHtml(model, "event/eventAdminTree.jte");
    }
}

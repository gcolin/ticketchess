package com.github.gcolin.platform;

import com.github.gcolin.auth.RequireRole;
import com.github.gcolin.club.ClubSeasonFilter;
import com.github.gcolin.club.SeasonScope;
import com.github.gcolin.auth.RoleCode;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.platform.Transactional;
import com.github.gcolin.player.DisplayPlayer;
import com.github.gcolin.player.IPlayer;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import com.github.gcolin.player.Find;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("dashboard")
public class DashboardApi {

    @Inject
    private PlayerSubscriptionDao playerSubscriptionDao;

    @Inject
    private Find find;

    @Inject
    private ClubSeasonFilter clubSeasonFilter;

    @GET
    @RequireRole(RoleCode.EVENT_ADMIN)
    @Transactional
    public JteHtml page(@QueryParam("seasonId") Integer seasonId) {
        Map<String, Object> model = new HashMap<>();
        SeasonScope scope = clubSeasonFilter.resolve(seasonId);
        clubSeasonFilter.addToModel(model, seasonId);

        List<PlayerSubscription> subsWithoutPayment =
                playerSubscriptionDao.findWithoutPaymentWithEvent(PlayerSubscriptionStatus.PAID, scope);
        List<PlayerSubscription> cancelledSubs = playerSubscriptionDao.findCancelledWithEvent(scope);
        List<PlayerSubscription> notPaidSubs = playerSubscriptionDao.findNotPaidWithEvent(scope);
        fillPlayers(subsWithoutPayment);
        fillPlayers(cancelledSubs);
        fillPlayers(notPaidSubs);
        model.put("subsWithoutPayment", subsWithoutPayment);
        model.put("cancelledSubs", cancelledSubs);
        model.put("notPaidSubs", notPaidSubs);
        return new JteHtml(model, "platform/dashboard.jte");
    }

    private void fillPlayers(List<PlayerSubscription> subs) {
        for (PlayerSubscription sub : subs) {
            IPlayer player = find.player(sub.getNrFfe(), null);
            if (player != null) {
                sub.setDisplayPlayer(new DisplayPlayer(player));
            }
        }
    }
}

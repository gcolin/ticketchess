package com.github.gcolin.platform;

import com.github.gcolin.auth.RequirePermission;
import com.github.gcolin.auth.PermissionCode;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("dashboard")
public class DashboardApi {

    @Inject
    private PlayerSubscriptionDao playerSubscriptionDao;

    @Inject
    private Find find;

    @GET
    @RequirePermission(PermissionCode.ADMIN_PANEL)
    @Transactional
    public JteHtml page() {
        Map<String, Object> model = new HashMap<>();
        List<PlayerSubscription> subsWithoutPayment =
                playerSubscriptionDao.findWithoutPaymentWithEvent(PlayerSubscriptionStatus.PAID);
        List<PlayerSubscription> cancelledSubs = playerSubscriptionDao.findCancelledWithEvent();
        List<PlayerSubscription> notPaidSubs = playerSubscriptionDao.findNotPaidWithEvent();
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

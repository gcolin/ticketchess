package com.github.gcolin.event;

import com.github.gcolin.auth.RequirePermission;
import com.github.gcolin.event.Event;
import com.github.gcolin.platform.Transactional;
import com.github.gcolin.auth.PermissionCode;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.player.IPlayer;
import com.github.gcolin.player.Find;
import com.github.gcolin.platform.ServiceUtils;
import com.github.gcolin.event.EventDao;
import com.github.gcolin.payment.PaymentDao;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.github.gcolin.platform.JteHtml;

@Path("statistics")
public class StatisticsApi {

    @Inject
    private EventDao eventDao;

    @Inject
    private PaymentDao paymentDao;

    @Inject
    private PlayerSubscriptionDao playerSubscriptionDao;

    @Inject
    private Find find;

    @GET
    @RequirePermission(PermissionCode.ADMIN_PANEL)
    @Transactional
    public JteHtml page() {
        Map<String, Object> model = new HashMap<>();

        // Get 10 closest future events
        List<Event> closestEvents = eventDao.findClosestEvents(10);
        model.put("closestEvents", closestEvents);

        // Get 10 events with most participants
        List<Object[]> topEvents = eventDao.findTopEventsByParticipants(10);
        model.put("topEvents", topEvents);

        // Get sum of all payments
        Double totalPayments = paymentDao.sumAllPayments();
        model.put("totalPayments", totalPayments != null ? totalPayments : 0.0);

        // Get sum of unpaid player subscriptions using calculatePrice (considers young/senior status)
        Double unpaidPlayerSubscriptions = calculateUnpaidSubscriptionsTotal();
        model.put("unpaidPlayerSubscriptions", unpaidPlayerSubscriptions);

        return new JteHtml(model, "event/statistics.jte");
    }

    private Double calculateUnpaidSubscriptionsTotal() {
        List<PlayerSubscription> unpaidSubscriptions = playerSubscriptionDao.findNotPaidWithEvent();
        long totalCents = 0;

        for (PlayerSubscription subscription : unpaidSubscriptions) {
            if (subscription.getNrFfe() != null && subscription.getEvent() != null) {
                try {
                    IPlayer player = find.player(subscription.getNrFfe(), subscription.getEvent().getEventType());
                    if (player != null) {
                        long priceCents = ServiceUtils.calculatePrice(player, subscription.getEvent());
                        totalCents += priceCents;
                    }
                } catch (Exception e) {
                    // Ignore if player not found
                }
            }
        }

        return ServiceUtils.toEuros(totalCents);
    }
}

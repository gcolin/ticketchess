package com.github.gcolin.event;

import com.github.gcolin.auth.RequireRole;
import com.github.gcolin.event.Event;
import com.github.gcolin.auth.RoleCode;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.event.StatisticsReport;
import com.github.gcolin.event.StatisticsReportService;
import com.github.gcolin.event.EventDao;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import com.github.gcolin.platform.JteHtml;

@Path("event/{id:\\d+}/statistics")
@RequireRole(RoleCode.ADMIN)
public class EventStatisticsApi {

    @Inject
    private EventDao eventDao;

    @Inject
    private PlayerSubscriptionDao playerSubscriptionDao;

    @Inject
    private StatisticsReportService statisticsReportService;

    @Inject
    private EventPaymentsReportService eventPaymentsReportService;

    @GET
    public JteHtml show(@PathParam("id") Integer eventId) {
        Event event = eventDao.find(eventId);
        List<PlayerSubscription> subscriptions = playerSubscriptionDao.findByEvent(event);
        StatisticsReport report = statisticsReportService.computeForEvent(event, subscriptions);
        return new JteHtml(report.toEventModel(event), "event/eventStatistics.jte");
    }

    @GET
    @Path("csv")
    @Produces("text/csv; charset=UTF-8")
    public Response csv(@PathParam("id") Integer eventId) {
        Event event = eventDao.find(eventId);
        List<PlayerSubscription> subscriptions = playerSubscriptionDao.findByEvent(event);
        String csv = statisticsReportService.generateCsv(subscriptions, false);
        return Response.ok(csv)
                .header("Content-Disposition", "attachment; filename=statistiques-event-" + eventId + ".csv")
                .build();
    }

    @GET
    @Path("pdf")
    @Produces("application/pdf")
    public Response pdf(@PathParam("id") Integer eventId) {
        Event event = eventDao.find(eventId);
        List<PlayerSubscription> subscriptions = playerSubscriptionDao.findByEvent(event);
        StatisticsReport report = statisticsReportService.computeForEvent(event, subscriptions);
        try {
            byte[] pdf = statisticsReportService.generatePdf(event.getName(), report);
            return Response.ok(pdf)
                    .header("Content-Disposition", "attachment; filename=statistiques-event-" + eventId + ".pdf")
                    .build();
        } catch (RuntimeException e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("payments/pdf")
    @Produces("application/pdf")
    public Response paymentsPdf(@PathParam("id") Integer eventId) {
        Event event = eventDao.find(eventId);
        List<PlayerSubscription> subscriptions = playerSubscriptionDao.findByEvent(event);
        try {
            byte[] pdf = eventPaymentsReportService.generateForEvent(event, subscriptions);
            return Response.ok(pdf)
                    .header("Content-Disposition", "attachment; filename=paiements-event-" + eventId + ".pdf")
                    .build();
        } catch (RuntimeException e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}

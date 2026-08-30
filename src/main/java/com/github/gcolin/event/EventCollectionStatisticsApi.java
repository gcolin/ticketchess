package com.github.gcolin.event;

import com.github.gcolin.auth.RequireRole;
import com.github.gcolin.event.EventCollection;
import com.github.gcolin.auth.RoleCode;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.event.StatisticsReport;
import com.github.gcolin.event.StatisticsReportService;
import com.github.gcolin.event.EventCollectionDao;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import com.github.gcolin.platform.JteHtml;

@Path("eventcollection/{id:\\d+}/statistics")
@RequireRole(RoleCode.EVENT_ADMIN)
public class EventCollectionStatisticsApi {

    @Inject
    private EventCollectionDao eventCollectionDao;

    @Inject
    private PlayerSubscriptionDao playerSubscriptionDao;

    @Inject
    private StatisticsReportService statisticsReportService;

    @Inject
    private EventPaymentsReportService eventPaymentsReportService;

    @GET
    public JteHtml show(@PathParam("id") Integer eventCollectionId) {
        EventCollection eventCollection = eventCollectionDao.find(eventCollectionId);
        if (eventCollection == null) {
            throw new NotFoundException();
        }

        List<PlayerSubscription> subscriptions = playerSubscriptionDao.findByEventCollection(eventCollectionId);
        StatisticsReport report = statisticsReportService.computeForEventCollection(subscriptions);
        return new JteHtml(report.toEventCollectionModel(eventCollection), "event/eventCollectionStatistics.jte");
    }

    @GET
    @Path("csv")
    @Produces("text/csv; charset=UTF-8")
    public Response csv(@PathParam("id") Integer eventCollectionId) {
        EventCollection eventCollection = eventCollectionDao.find(eventCollectionId);
        if (eventCollection == null) {
            throw new NotFoundException();
        }

        List<PlayerSubscription> subscriptions = playerSubscriptionDao.findByEventCollection(eventCollectionId);
        String csv = statisticsReportService.generateCsv(subscriptions, true);
        return Response.ok(csv)
                .header(
                        "Content-Disposition",
                        "attachment; filename=statistiques-collection-" + eventCollectionId + ".csv")
                .build();
    }

    @GET
    @Path("pdf")
    @Produces("application/pdf")
    public Response pdf(@PathParam("id") Integer eventCollectionId) {
        EventCollection eventCollection = eventCollectionDao.find(eventCollectionId);
        if (eventCollection == null) {
            throw new NotFoundException();
        }

        List<PlayerSubscription> subscriptions = playerSubscriptionDao.findByEventCollection(eventCollectionId);
        StatisticsReport report = statisticsReportService.computeForEventCollection(subscriptions);
        try {
            byte[] pdf = statisticsReportService.generatePdf(eventCollection.getName(), report);
            return Response.ok(pdf)
                    .header(
                            "Content-Disposition",
                            "attachment; filename=statistiques-collection-" + eventCollectionId + ".pdf")
                    .build();
        } catch (RuntimeException e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("payments/pdf")
    @Produces("application/pdf")
    public Response paymentsPdf(@PathParam("id") Integer eventCollectionId) {
        EventCollection eventCollection = eventCollectionDao.find(eventCollectionId);
        if (eventCollection == null) {
            throw new NotFoundException();
        }

        List<PlayerSubscription> subscriptions = playerSubscriptionDao.findByEventCollection(eventCollectionId);
        try {
            byte[] pdf = eventPaymentsReportService.generateForEventCollection(eventCollection, subscriptions);
            return Response.ok(pdf)
                    .header(
                            "Content-Disposition",
                            "attachment; filename=paiements-collection-" + eventCollectionId + ".pdf")
                    .build();
        } catch (RuntimeException e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}

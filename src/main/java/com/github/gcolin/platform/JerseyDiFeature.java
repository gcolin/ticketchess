package com.github.gcolin.platform;

import com.github.gcolin.auth.ActiveLoggedUsers;
import com.github.gcolin.auth.LoggedUser;
import com.github.gcolin.auth.UserAuthorizationDao;
import com.github.gcolin.desk.EventDeskHub;
import com.github.gcolin.desk.EventDeskService;
import com.github.gcolin.event.EventCollectionDao;
import com.github.gcolin.event.EventCollectionOptionDao;
import com.github.gcolin.event.EventDao;
import com.github.gcolin.event.EventGroupDao;
import com.github.gcolin.event.EventGroupFilter;
import com.github.gcolin.event.EventInfoDao;
import com.github.gcolin.event.EventOptionDao;
import com.github.gcolin.event.EventPaymentsReportService;
import com.github.gcolin.event.PapiService;
import com.github.gcolin.event.PapiUlploadService;
import com.github.gcolin.event.StatisticsReportService;
import com.github.gcolin.club.ClubSeasonDao;
import com.github.gcolin.club.ClubSeasonFilter;
import com.github.gcolin.membership.LicenseDao;
import com.github.gcolin.membership.LicensePriceDao;
import com.github.gcolin.membership.LicensePriceService;
import com.github.gcolin.membership.MembershipDao;
import com.github.gcolin.membership.MembershipOptionDao;
import com.github.gcolin.membership.MembershipOptionSubscriptionDao;
import com.github.gcolin.membership.MembershipReportService;
import com.github.gcolin.notification.NotificationDao;
import com.github.gcolin.notification.Notifications;
import com.github.gcolin.payment.DebtService;
import com.github.gcolin.payment.PaymentDao;
import com.github.gcolin.payment.RibService;
import com.github.gcolin.player.CustomPlayerDao;
import com.github.gcolin.player.Find;
import com.github.gcolin.player.LuceneDb;
import com.github.gcolin.registration.PlayerPendingSubscriptionDao;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import com.github.gcolin.registration.PlayerSubscriptionOptionDao;
import com.github.gcolin.registration.RegisterService;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import java.util.Properties;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

public class JerseyDiFeature implements Feature {

    @Override
    public boolean configure(FeatureContext context) {
        context.register(ReadOnlyPersistenceFilter.class);
        context.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bindFactory(app(AppContext.get().config())).to(Config.class);
                bindFactory(app(AppContext.get().caches())).to(Caches.class);
                bindFactory(app(AppContext.get().sendMail())).to(SendMail.class);
                bindFactory(app(AppContext.get().jteConfig())).to(JteConfig.class);
                bindFactory(app(AppContext.get().luceneDb())).to(LuceneDb.class);
                bindFactory(app(AppContext.get().logoService())).to(LogoService.class);
                bindFactory(app(AppContext.get().ribService())).to(RibService.class);
                bindFactory(app(AppContext.get().backgroundService())).to(BackgroundService.class);
                bindFactory(app(AppContext.get().activeLoggedUsers())).to(ActiveLoggedUsers.class);
                bindFactory(app(AppContext.get().eventDeskHub())).to(EventDeskHub.class);
                bindFactory(app(AppContext.get().eventDeskService())).to(EventDeskService.class);
                bindFactory(app(AppContext.get().licensePriceService())).to(LicensePriceService.class);
                bindFactory(app(AppContext.get().statisticsReportService())).to(StatisticsReportService.class);
                bindFactory(app(AppContext.get().eventPaymentsReportService())).to(EventPaymentsReportService.class);
                bindFactory(app(AppContext.get().membershipReportService())).to(MembershipReportService.class);
                bindFactory(app(AppContext.get().config().getProperties())).to(Properties.class);

                bindFactory(req(RequestContext::clubSeasonDao)).to(ClubSeasonDao.class);
                bindFactory(req(RequestContext::clubSeasonFilter)).to(ClubSeasonFilter.class);
                bindFactory(req(RequestContext::licenseDao)).to(LicenseDao.class);
                bindFactory(req(RequestContext::licensePriceDao)).to(LicensePriceDao.class);
                bindFactory(req(RequestContext::membershipDao)).to(MembershipDao.class);
                bindFactory(req(RequestContext::membershipOptionDao)).to(MembershipOptionDao.class);
                bindFactory(req(RequestContext::membershipOptionSubscriptionDao))
                        .to(MembershipOptionSubscriptionDao.class);
                bindFactory(req(RequestContext::userAuthorizationDao)).to(UserAuthorizationDao.class);
                bindFactory(req(RequestContext::customPlayerDao)).to(CustomPlayerDao.class);
                bindFactory(req(RequestContext::eventGroupDao)).to(EventGroupDao.class);
                bindFactory(req(RequestContext::eventInfoDao)).to(EventInfoDao.class);
                bindFactory(req(RequestContext::eventOptionDao)).to(EventOptionDao.class);
                bindFactory(req(RequestContext::eventCollectionOptionDao)).to(EventCollectionOptionDao.class);
                bindFactory(req(RequestContext::eventCollectionDao)).to(EventCollectionDao.class);
                bindFactory(req(RequestContext::eventDao)).to(EventDao.class);
                bindFactory(req(RequestContext::playerSubscriptionDao)).to(PlayerSubscriptionDao.class);
                bindFactory(req(RequestContext::playerSubscriptionOptionDao)).to(PlayerSubscriptionOptionDao.class);
                bindFactory(req(RequestContext::playerPendingSubscriptionDao)).to(PlayerPendingSubscriptionDao.class);
                bindFactory(req(RequestContext::paymentDao)).to(PaymentDao.class);
                bindFactory(req(RequestContext::notificationDao)).to(NotificationDao.class);
                bindFactory(req(RequestContext::find)).to(Find.class);
                bindFactory(req(RequestContext::debtService)).to(DebtService.class);
                bindFactory(req(RequestContext::registerService)).to(RegisterService.class);
                bindFactory(req(RequestContext::stateService)).to(StateService.class);
                bindFactory(req(RequestContext::notifications)).to(Notifications.class);
                bindFactory(req(RequestContext::eventGroupFilter)).to(EventGroupFilter.class);
                bindFactory(req(RequestContext::papiService)).to(PapiService.class);
                bindFactory(req(RequestContext::papiUlploadService)).to(PapiUlploadService.class);
                bindFactory(req(RequestContext::loggedUser)).to(LoggedUser.class);
            }
        });
        return true;
    }

    private static <T> Factory<T> app(T instance) {
        return new Factory<>() {
            @Override
            public T provide() {
                return instance;
            }

            @Override
            public void dispose(T instance) {}
        };
    }

    private interface RequestSupplier<T> {
        T get(RequestContext ctx);
    }

    private static <T> Factory<T> req(RequestSupplier<T> supplier) {
        return new Factory<>() {
            @Override
            public T provide() {
                return supplier.get(RequestContext.require());
            }

            @Override
            public void dispose(T instance) {}
        };
    }
}

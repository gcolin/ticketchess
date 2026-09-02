package com.github.gcolin.platform;

import com.github.gcolin.auth.ActiveLoggedUsers;
import com.github.gcolin.desk.EventDeskHub;
import com.github.gcolin.desk.EventDeskService;
import com.github.gcolin.event.EventPaymentsReportService;
import com.github.gcolin.event.StatisticsReportService;
import com.github.gcolin.membership.LicensePriceService;
import com.github.gcolin.membership.MembershipReportService;
import com.github.gcolin.payment.RibService;
import com.github.gcolin.player.LuceneDb;
import jakarta.servlet.ServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AppContext {

    private static final Logger logger = LoggerFactory.getLogger(AppContext.class);
    private static AppContext instance;

    private Config config;
    private Caches caches;
    private PersistenceService persistence;
    private JteConfig jteConfig;
    private SendMail sendMail;
    private LuceneDb luceneDb;
    private ActiveLoggedUsers activeLoggedUsers;
    private EventDeskHub eventDeskHub;
    private EventDeskService eventDeskService;
    private LogoService logoService;
    private RibService ribService;
    private LicensePriceService licensePriceService;
    private StatisticsReportService statisticsReportService;
    private EventPaymentsReportService eventPaymentsReportService;
    private MembershipReportService membershipReportService;
    private BackgroundService backgroundService;

    private AppContext() {}

    public static void init(ServletContext servletContext) {
        if (instance != null) {
            return;
        }
        AppContext app = new AppContext();
        instance = app;

        app.config = new Config();
        app.config.init(servletContext);

        app.caches = new Caches();

        app.persistence = new PersistenceService(app.config);
        app.persistence.init();

        DbInit dbInit = new DbInit(app.persistence.getEmf());
        dbInit.initLicenses();
        dbInit.initClubSeason();
        dbInit.initMembershipOptions();
        dbInit.initEvents();

        app.jteConfig = new JteConfig();
        app.jteConfig.init();

        app.sendMail = new SendMail();
        app.sendMail.setProperties(app.config.getProperties());
        app.sendMail.setConfig(app.config);
        app.sendMail.init();

        app.luceneDb = new LuceneDb();
        app.luceneDb.setConfig(app.config);
        app.luceneDb.init();

        app.activeLoggedUsers = new ActiveLoggedUsers();
        app.eventDeskHub = new EventDeskHub();

        app.eventDeskService = new EventDeskService();
        app.eventDeskService.setEmf(app.persistence.getEmf());
        app.eventDeskService.setCaches(app.caches);
        app.eventDeskService.setSendMail(app.sendMail);

        app.logoService = new LogoService();
        app.logoService.setConfig(app.config);

        app.ribService = new RibService();
        app.ribService.setConfig(app.config);

        app.licensePriceService = new LicensePriceService();
        app.statisticsReportService = new StatisticsReportService();
        app.eventPaymentsReportService = new EventPaymentsReportService();
        app.eventPaymentsReportService.setProperties(app.config.getProperties());
        app.membershipReportService = new MembershipReportService();
        app.membershipReportService.setProperties(app.config.getProperties());

        app.backgroundService = new BackgroundService();
        app.backgroundService.setConfig(app.config);

        logger.info("Application context initialized");
    }

    public static void shutdown() {
        if (instance == null) {
            return;
        }
        try {
            instance.sendMail.close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while closing mail executor", e);
        }
        instance.persistence.shutdown();
        instance = null;
    }

    public static AppContext get() {
        if (instance == null) {
            throw new IllegalStateException("Application context not initialized");
        }
        return instance;
    }

    static void initForTest(ActiveLoggedUsers activeLoggedUsers) {
        AppContext app = new AppContext();
        app.activeLoggedUsers = activeLoggedUsers;
        instance = app;
    }

    static void clearTestInstance() {
        instance = null;
    }

    public Config config() {
        return config;
    }

    public Caches caches() {
        return caches;
    }

    public PersistenceService persistence() {
        return persistence;
    }

    public JteConfig jteConfig() {
        return jteConfig;
    }

    public SendMail sendMail() {
        return sendMail;
    }

    public LuceneDb luceneDb() {
        return luceneDb;
    }

    public ActiveLoggedUsers activeLoggedUsers() {
        return activeLoggedUsers;
    }

    public EventDeskHub eventDeskHub() {
        return eventDeskHub;
    }

    public EventDeskService eventDeskService() {
        return eventDeskService;
    }

    public LogoService logoService() {
        return logoService;
    }

    public RibService ribService() {
        return ribService;
    }

    public LicensePriceService licensePriceService() {
        return licensePriceService;
    }

    public StatisticsReportService statisticsReportService() {
        return statisticsReportService;
    }

    public EventPaymentsReportService eventPaymentsReportService() {
        return eventPaymentsReportService;
    }

    public MembershipReportService membershipReportService() {
        return membershipReportService;
    }

    public BackgroundService backgroundService() {
        return backgroundService;
    }
}

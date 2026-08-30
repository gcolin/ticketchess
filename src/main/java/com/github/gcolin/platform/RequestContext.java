package com.github.gcolin.platform;

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
import com.github.gcolin.event.PapiService;
import com.github.gcolin.event.PapiUlploadService;
import com.github.gcolin.event.ChessEventService;
import com.github.gcolin.club.ClubSeasonDao;
import com.github.gcolin.club.ClubSeasonFilter;
import com.github.gcolin.membership.LicenseDao;
import com.github.gcolin.membership.LicensePriceDao;
import com.github.gcolin.membership.MembershipDao;
import com.github.gcolin.membership.MembershipOptionDao;
import com.github.gcolin.membership.MembershipOptionSubscriptionDao;
import com.github.gcolin.notification.NotificationDao;
import com.github.gcolin.notification.Notifications;
import com.github.gcolin.payment.DebtService;
import com.github.gcolin.payment.PaymentDao;
import com.github.gcolin.player.CustomPlayerDao;
import com.github.gcolin.player.Find;
import com.github.gcolin.registration.PlayerPendingSubscriptionDao;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import com.github.gcolin.registration.PlayerSubscriptionOptionDao;
import com.github.gcolin.registration.RegisterService;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.function.Supplier;

public final class RequestContext {

    private static final ThreadLocal<RequestContext> CURRENT = new ThreadLocal<>();
    private static final String LOGGED_USER_KEY = "loggedUser";

    private HttpServletRequest request;
    private EntityManager em;
    private boolean readOnly;

    private ClubSeasonDao clubSeasonDao;
    private ClubSeasonFilter clubSeasonFilter;
    private LicenseDao licenseDao;
    private LicensePriceDao licensePriceDao;
    private MembershipDao membershipDao;
    private MembershipOptionDao membershipOptionDao;
    private MembershipOptionSubscriptionDao membershipOptionSubscriptionDao;
    private UserAuthorizationDao userAuthorizationDao;
    private CustomPlayerDao customPlayerDao;
    private EventGroupDao eventGroupDao;
    private EventInfoDao eventInfoDao;
    private EventOptionDao eventOptionDao;
    private EventCollectionOptionDao eventCollectionOptionDao;
    private EventCollectionDao eventCollectionDao;
    private EventDao eventDao;
    private PlayerSubscriptionDao playerSubscriptionDao;
    private PlayerSubscriptionOptionDao playerSubscriptionOptionDao;
    private PlayerPendingSubscriptionDao playerPendingSubscriptionDao;
    private PaymentDao paymentDao;
    private NotificationDao notificationDao;
    private Find find;
    private LoggedUser loggedUserOverride;
    private DebtService debtService;
    private RegisterService registerService;
    private StateService stateService;
    private Notifications notifications;
    private EventGroupFilter eventGroupFilter;
    private PapiService papiService;
    private PapiUlploadService papiUlploadService;
    private ChessEventService chessEventService;

    private RequestContext() {}

    public static void open(HttpServletRequest request) {
        RequestContext ctx = new RequestContext();
        ctx.request = request;
        ctx.em = AppContext.get().persistence().getEmf().createEntityManager();
        ctx.em.getTransaction().begin();
        CURRENT.set(ctx);
    }

    public static void openReadOnly(HttpServletRequest request) {
        RequestContext ctx = new RequestContext();
        ctx.request = request;
        ctx.readOnly = true;
        ctx.em = AppContext.get().persistence().getEmf().createEntityManager();
        CURRENT.set(ctx);
    }

    public static boolean isReadOnly() {
        RequestContext ctx = CURRENT.get();
        return ctx != null && ctx.readOnly;
    }

    public static void commitReadAndClear() {
        RequestContext ctx = CURRENT.get();
        if (ctx == null || !ctx.readOnly || ctx.em == null) {
            return;
        }
        if (ctx.em.getTransaction().isActive()) {
            ctx.em.getTransaction().commit();
        }
        ctx.em.clear();
    }

    private void touchEm() {
        if (readOnly && em != null && !em.getTransaction().isActive()) {
            em.getTransaction().begin();
        }
    }

    static void openForTest(EntityManager em) {
        RequestContext ctx = new RequestContext();
        ctx.em = em;
        CURRENT.set(ctx);
    }

    static void openForTestReadOnly(EntityManager em) {
        RequestContext ctx = new RequestContext();
        ctx.em = em;
        ctx.readOnly = true;
        CURRENT.set(ctx);
    }

    static void openForTest(HttpServletRequest request, EntityManager em) {
        RequestContext ctx = new RequestContext();
        ctx.request = request;
        ctx.em = em;
        CURRENT.set(ctx);
    }

    static void openForTest(Find find) {
        RequestContext ctx = new RequestContext();
        ctx.find = find;
        CURRENT.set(ctx);
    }

    static void openForTest(LoggedUser loggedUser) {
        RequestContext ctx = new RequestContext();
        ctx.loggedUserOverride = loggedUser;
        CURRENT.set(ctx);
    }

    static void openForTest(EntityManager em, Find find) {
        RequestContext ctx = new RequestContext();
        ctx.em = em;
        ctx.find = find;
        CURRENT.set(ctx);
    }

    public static void commit() {
        RequestContext ctx = CURRENT.get();
        if (ctx != null && ctx.em != null && ctx.em.getTransaction().isActive()) {
            ctx.em.flush();
            ctx.em.getTransaction().commit();
        }
    }

    public static void rollback() {
        RequestContext ctx = CURRENT.get();
        if (ctx != null && ctx.em != null && ctx.em.getTransaction().isActive()) {
            ctx.em.getTransaction().rollback();
        }
    }

    public static void close() {
        RequestContext ctx = CURRENT.get();
        try {
            if (ctx != null && ctx.em != null && ctx.em.isOpen()) {
                ctx.em.close();
            }
        } finally {
            CURRENT.remove();
        }
    }

    public static RequestContext require() {
        RequestContext ctx = CURRENT.get();
        if (ctx == null) {
            throw new IllegalStateException("No active request context");
        }
        return ctx;
    }

    public static void run(Runnable action) {
        run(() -> {
            action.run();
            return null;
        });
    }

    public static <T> T run(Supplier<T> action) {
        RequestContext ctx = new RequestContext();
        ctx.em = AppContext.get().persistence().getEmf().createEntityManager();
        ctx.em.getTransaction().begin();
        CURRENT.set(ctx);
        try {
            T result = action.get();
            ctx.em.flush();
            ctx.em.getTransaction().commit();
            return result;
        } catch (RuntimeException e) {
            if (ctx.em.getTransaction().isActive()) {
                ctx.em.getTransaction().rollback();
            }
            throw e;
        } finally {
            CURRENT.remove();
            if (ctx.em.isOpen()) {
                ctx.em.close();
            }
        }
    }

    public EntityManager em() {
        return em;
    }

    public HttpServletRequest request() {
        return request;
    }

    public LoggedUser loggedUser() {
        if (loggedUserOverride != null) {
            return loggedUserOverride;
        }
        if (request == null) {
            throw new IllegalStateException("LoggedUser requires an HTTP request");
        }
        HttpSession session = request.getSession(true);
        LoggedUser user = (LoggedUser) session.getAttribute(LOGGED_USER_KEY);
        boolean isNew = user == null;
        if (isNew) {
            user = new LoggedUser();
            session.setAttribute(LOGGED_USER_KEY, user);
        }
        LoggedUser.wire(
                user,
                AppContext.get().caches(),
                debtService(),
                request,
                AppContext.get().config(),
                userAuthorizationDao());
        if (isNew) {
            user.initFromCookies();
        }
        return user;
    }

    public ClubSeasonDao clubSeasonDao() {
        touchEm();
        if (clubSeasonDao == null) {
            clubSeasonDao = new ClubSeasonDao();
            clubSeasonDao.setEm(em);
        }
        return clubSeasonDao;
    }

    public ClubSeasonFilter clubSeasonFilter() {
        if (clubSeasonFilter == null) {
            clubSeasonFilter = new ClubSeasonFilter();
            clubSeasonFilter.setClubSeasonDao(clubSeasonDao());
        }
        return clubSeasonFilter;
    }

    public LicenseDao licenseDao() {
        touchEm();
        if (licenseDao == null) {
            licenseDao = new LicenseDao();
            licenseDao.setEm(em);
        }
        return licenseDao;
    }

    public LicensePriceDao licensePriceDao() {
        touchEm();
        if (licensePriceDao == null) {
            licensePriceDao = new LicensePriceDao();
            licensePriceDao.setEm(em);
        }
        return licensePriceDao;
    }

    public MembershipDao membershipDao() {
        touchEm();
        if (membershipDao == null) {
            membershipDao = new MembershipDao();
            membershipDao.setEm(em);
        }
        return membershipDao;
    }

    public MembershipOptionDao membershipOptionDao() {
        touchEm();
        if (membershipOptionDao == null) {
            membershipOptionDao = new MembershipOptionDao();
            membershipOptionDao.setEm(em);
        }
        return membershipOptionDao;
    }

    public MembershipOptionSubscriptionDao membershipOptionSubscriptionDao() {
        touchEm();
        if (membershipOptionSubscriptionDao == null) {
            membershipOptionSubscriptionDao = new MembershipOptionSubscriptionDao();
            membershipOptionSubscriptionDao.setEm(em);
        }
        return membershipOptionSubscriptionDao;
    }

    public UserAuthorizationDao userAuthorizationDao() {
        touchEm();
        if (userAuthorizationDao == null) {
            userAuthorizationDao = new UserAuthorizationDao();
            userAuthorizationDao.setEm(em);
        }
        return userAuthorizationDao;
    }

    public CustomPlayerDao customPlayerDao() {
        touchEm();
        if (customPlayerDao == null) {
            customPlayerDao = new CustomPlayerDao();
            customPlayerDao.setEm(em);
        }
        return customPlayerDao;
    }

    public EventGroupDao eventGroupDao() {
        touchEm();
        if (eventGroupDao == null) {
            eventGroupDao = new EventGroupDao();
            eventGroupDao.setEm(em);
        }
        return eventGroupDao;
    }

    public EventInfoDao eventInfoDao() {
        touchEm();
        if (eventInfoDao == null) {
            eventInfoDao = new EventInfoDao();
            eventInfoDao.setEm(em);
            eventInfoDao.setEventDao(this::eventDao);
        }
        return eventInfoDao;
    }

    public EventOptionDao eventOptionDao() {
        touchEm();
        if (eventOptionDao == null) {
            eventOptionDao = new EventOptionDao();
            eventOptionDao.setEm(em);
            eventOptionDao.setEventDao(this::eventDao);
        }
        return eventOptionDao;
    }

    public EventCollectionOptionDao eventCollectionOptionDao() {
        touchEm();
        if (eventCollectionOptionDao == null) {
            eventCollectionOptionDao = new EventCollectionOptionDao();
            eventCollectionOptionDao.setEm(em);
        }
        return eventCollectionOptionDao;
    }

    public EventCollectionDao eventCollectionDao() {
        touchEm();
        if (eventCollectionDao == null) {
            eventCollectionDao = new EventCollectionDao();
            eventCollectionDao.setEm(em);
            eventCollectionDao.setEventCollectionOptionDao(this::eventCollectionOptionDao);
            eventCollectionDao.setPlayerSubscriptionDao(this::playerSubscriptionDao);
        }
        return eventCollectionDao;
    }

    public EventDao eventDao() {
        touchEm();
        if (eventDao == null) {
            eventDao = new EventDao();
            eventDao.setEm(em);
            eventDao.setFinder(find());
            eventDao.setEventInfoDao(this::eventInfoDao);
            eventDao.setEventGroupDao(this::eventGroupDao);
            eventDao.setEventOptionDao(this::eventOptionDao);
            eventDao.setEventCollectionOptionDao(this::eventCollectionOptionDao);
            eventDao.setPlayerSubscriptionDao(this::playerSubscriptionDao);
        }
        return eventDao;
    }

    public PlayerSubscriptionDao playerSubscriptionDao() {
        touchEm();
        if (playerSubscriptionDao == null) {
            playerSubscriptionDao = new PlayerSubscriptionDao();
            playerSubscriptionDao.setEm(em);
        }
        return playerSubscriptionDao;
    }

    public PlayerSubscriptionOptionDao playerSubscriptionOptionDao() {
        touchEm();
        if (playerSubscriptionOptionDao == null) {
            playerSubscriptionOptionDao = new PlayerSubscriptionOptionDao();
            playerSubscriptionOptionDao.setEm(em);
        }
        return playerSubscriptionOptionDao;
    }

    public PlayerPendingSubscriptionDao playerPendingSubscriptionDao() {
        touchEm();
        if (playerPendingSubscriptionDao == null) {
            playerPendingSubscriptionDao = new PlayerPendingSubscriptionDao();
            playerPendingSubscriptionDao.setEm(em);
        }
        return playerPendingSubscriptionDao;
    }

    public PaymentDao paymentDao() {
        touchEm();
        if (paymentDao == null) {
            paymentDao = new PaymentDao();
            paymentDao.setEm(em);
        }
        return paymentDao;
    }

    public NotificationDao notificationDao() {
        touchEm();
        if (notificationDao == null) {
            notificationDao = new NotificationDao();
            notificationDao.setEm(em);
            notificationDao.setEventDao(eventDao());
            notificationDao.setEventGroupDao(eventGroupDao());
        }
        return notificationDao;
    }

    public Find find() {
        if (find == null) {
            find = new Find();
            find.setLuceneDb(AppContext.get().luceneDb());
            find.setCustomPlayerDao(customPlayerDao());
        }
        return find;
    }

    public DebtService debtService() {
        if (debtService == null) {
            debtService = new DebtService();
            debtService.setPlayerSubscriptionDao(playerSubscriptionDao());
            debtService.setPlayerSubscriptionOptionDao(playerSubscriptionOptionDao());
            debtService.setFind(find());
            debtService.setSendMail(AppContext.get().sendMail());
            debtService.setCaches(AppContext.get().caches());
            debtService.setPaymentDao(paymentDao());
        }
        return debtService;
    }

    public RegisterService registerService() {
        if (registerService == null) {
            registerService = new RegisterService();
            registerService.setSendMail(AppContext.get().sendMail());
            registerService.setProperties(AppContext.get().config().getProperties());
            registerService.setPlayerSubscriptionDao(playerSubscriptionDao());
            registerService.setPlayerPendingSubscriptionDao(playerPendingSubscriptionDao());
            registerService.setFind(find());
            registerService.setEventDao(eventDao());
            registerService.setEventOptionDao(eventOptionDao());
            registerService.setEventCollectionOptionDao(eventCollectionOptionDao());
            registerService.setCaches(AppContext.get().caches());
            registerService.setPaymentDao(paymentDao());
            registerService.setConfig(AppContext.get().config());
        }
        return registerService;
    }

    public StateService stateService() {
        if (stateService == null) {
            stateService = new StateService();
            stateService.setRequest(request);
            stateService.setConfig(AppContext.get().config());
        }
        return stateService;
    }

    public Notifications notifications() {
        if (notifications == null) {
            notifications = new Notifications();
            notifications.setCaches(AppContext.get().caches());
            notifications.setNotificationDao(notificationDao());
        }
        return notifications;
    }

    public EventGroupFilter eventGroupFilter() {
        if (eventGroupFilter == null) {
            eventGroupFilter = new EventGroupFilter();
            eventGroupFilter.setCaches(AppContext.get().caches());
            eventGroupFilter.setEventGroupDao(eventGroupDao());
        }
        return eventGroupFilter;
    }

    public PapiService papiService() {
        if (papiService == null) {
            papiService = new PapiService();
            papiService.setLuceneDb(AppContext.get().luceneDb());
        }
        return papiService;
    }

    public PapiUlploadService papiUlploadService() {
        if (papiUlploadService == null) {
            papiUlploadService = new PapiUlploadService();
        }
        return papiUlploadService;
    }

    public ChessEventService chessEventService() {
        if (chessEventService == null) {
            chessEventService = new ChessEventService(
                    eventDao(),
                    eventCollectionDao(),
                    eventCollectionOptionDao(),
                    eventOptionDao(),
                    playerSubscriptionDao(),
                    AppContext.get().luceneDb());
        }
        return chessEventService;
    }
}

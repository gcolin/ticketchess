package com.github.gcolin.registration;

import com.github.gcolin.platform.ServiceUtils;

import com.github.gcolin.platform.Config;
import com.github.gcolin.event.Event;
import com.github.gcolin.event.EventCollection;
import com.github.gcolin.event.EventCollectionOptionType;
import com.github.gcolin.event.EventOptionType;
import com.github.gcolin.payment.Payment;
import com.github.gcolin.payment.PaymentStatus;
import com.github.gcolin.registration.PlayerPendingSubscription;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.player.IPlayer;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import com.github.gcolin.event.EventDao;
import com.github.gcolin.event.EventCollectionOptionDao;
import com.github.gcolin.event.EventOptionDao;
import com.github.gcolin.payment.PaymentDao;
import com.github.gcolin.registration.PlayerPendingSubscriptionDao;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import com.github.gcolin.registration.CancelMail;
import com.github.gcolin.registration.RegistrationMail;
import com.github.gcolin.platform.SendMail;
import io.jsonwebtoken.Jwts;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.gcolin.platform.Caches;
import com.github.gcolin.player.Find;

public class RegisterService {

    private SendMail sendMail;
    private Properties properties;
    private PlayerSubscriptionDao playerSubscriptionService;
    private PlayerPendingSubscriptionDao playerPendingSubscriptionDao;
    private Find find;
    private EventDao eventService;
    private EventOptionDao eventOptionService;
    private EventCollectionOptionDao eventCollectionOptionService;
    private Caches caches;
    private PaymentDao paymentService;
    private Config config;

    private static Logger logger = LoggerFactory.getLogger(RegisterService.class.getName());

    public void setSendMail(SendMail sendMail) {
        this.sendMail = sendMail;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public void setPlayerSubscriptionDao(PlayerSubscriptionDao playerSubscriptionService) {
        this.playerSubscriptionService = playerSubscriptionService;
    }

    public void setPlayerPendingSubscriptionDao(PlayerPendingSubscriptionDao playerPendingSubscriptionDao) {
        this.playerPendingSubscriptionDao = playerPendingSubscriptionDao;
    }

    public void setFind(Find find) {
        this.find = find;
    }

    public void setEventDao(EventDao eventService) {
        this.eventService = eventService;
    }

    public void setEventOptionDao(EventOptionDao eventOptionService) {
        this.eventOptionService = eventOptionService;
    }

    public void setEventCollectionOptionDao(EventCollectionOptionDao eventCollectionOptionService) {
        this.eventCollectionOptionService = eventCollectionOptionService;
    }

    public void setCaches(Caches caches) {
        this.caches = caches;
    }

    public void setPaymentDao(PaymentDao paymentService) {
        this.paymentService = paymentService;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public PlayerSubscription registerPlayerToEvent(Integer eventId, String nrffeString, String email) {
        return registerPlayerToEvent(eventService.find(eventId), find.player(nrffeString, null), email);
    }

    public PlayerSubscription registerPlayerToEvent(Event event, IPlayer player, String email) {
        String playerRef = resolvePlayerReference(player);
        PlayerSubscription existing = playerSubscriptionService.findByEventAndNrffe(event, playerRef);
        if (existing != null) {
            return null;
        }
        if (playerPendingSubscriptionDao.findByEventAndNrffe(event, playerRef) != null) {
            return null;
        }

        EventCollection eventCollection = event.getEventCollection();
        if (eventCollection != null
            && eventCollection.getId() != null
            && playerSubscriptionService.existsActiveByEventCollectionAndNrffe(eventCollection.getId(), playerRef)) {
            logger.info(
                "player {} already registered in event collection {}",
                playerRef,
                eventCollection.getId());
            return null;
        }

        Integer maxSubscriptions = resolveMaxSubscriptions(event);
        if (maxSubscriptions != null
                && maxSubscriptions > 0
                && playerSubscriptionService.countByEvent(event) >= maxSubscriptions) {
            logger.info("max subscriptions reached for event {}", event.getId());
            createPendingSubscription(event, email, playerRef);
            return null;
        }

        if (eventCollection != null && eventCollection.getId() != null) {
            Integer maxCollectionSubscriptions = eventCollectionOptionService.findIntOptionValue(
                eventCollection.getId(), EventCollectionOptionType.MAX_SUBSCRIPTIONS);
            if (maxCollectionSubscriptions != null
                && maxCollectionSubscriptions > 0
                && playerSubscriptionService.countByEventCollection(eventCollection.getId())
                    >= maxCollectionSubscriptions) {
            logger.info("max subscriptions reached for event collection {}", eventCollection.getId());
            createPendingSubscription(event, email, playerRef);
            return null;
            }
        }

        PlayerSubscription subscription = new PlayerSubscription();
        subscription.setEvent(event);
        subscription.setNrFfe(playerRef);
        subscription.setCreationUser(email);

        long priceCents = ServiceUtils.calculatePrice(player, event);

        subscription.setAmountCents(priceCents);
        subscription.setStatus(priceCents == 0 ? PlayerSubscriptionStatus.PAID : PlayerSubscriptionStatus.NOT_PAID);
        playerSubscriptionService.persist(subscription);

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE dd MMMM yyyy", Locale.FRANCE);
        RegistrationMail registrationMail = new RegistrationMail();
        registrationMail.setName(player.getFirstname() + " " + player.getName());
        if (priceCents > 0) {
            registrationMail.setAmount(ServiceUtils.toEuros(priceCents) + " Euros");
        }
        registrationMail.setEvenDate(sdf.format(event.getStartDateAsDate()));
        registrationMail.setEventName(event.getName());
        String baseUrl = properties.getProperty("baseurl");
        registrationMail.setBaseUrl(baseUrl);
        registrationMail.setLoginUrl(buildLoginUrl(email, registrationMail.getName(), baseUrl));

        try {
            sendMail.send(registrationMail, email, "Confirmation inscription " + event.getName());
        } catch (Exception e) {
            LoggerFactory.getLogger(this.getClass().getName()).error("cannot send mail", e);
        }
        caches.getEvent().invalidateAll();
        return playerSubscriptionService.detach(subscription);
    }

    public boolean unregisterPlayerToEvent(Integer eventId, String nrffe) {
        Event event = eventService.find(eventId);
        PlayerSubscription ps = playerSubscriptionService.findByEventAndNrffe(event, nrffe);

        if (ps == null) {
            return false;
        }
        IPlayer player = find.player(nrffe, null);

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE dd MMMM yyyy", Locale.FRANCE);
        CancelMail registrationMail = new CancelMail();
        registrationMail.setName(player.getFirstname() + " " + player.getName());

        long priceCents = ServiceUtils.calculatePrice(player, event);
        if (priceCents > 0) {
            Payment payment = paymentService.findPendingByUser(ps.getCreationUser());
            if (payment != null) {
                payment.setStatus(PaymentStatus.EXPIRED);
                paymentService.persist(payment);
            }
        }
        if (priceCents > 0 && ps.getStatus() != PlayerSubscriptionStatus.NOT_PAID) {
            ps.setStatus(PlayerSubscriptionStatus.CANCELLED);
            playerSubscriptionService.persist(ps);

            registrationMail.setAmount(ServiceUtils.toEuros(priceCents) + " Euros");
        } else {
            playerSubscriptionService.remove(ps);
        }

        registrationMail.setEvenDate(sdf.format(event.getStartDateAsDate()));
        registrationMail.setEventName(event.getName());
        registrationMail.setReference(ps.getCreationUser());
        registrationMail.setLoginUrl(
            buildLoginUrl(ps.getCreationUser(), registrationMail.getName(), properties.getProperty("baseurl")));

        try {
            sendMail.send(registrationMail, ps.getCreationUser(), "Confirmation annulation " + event.getName());
        } catch (Exception e) {
            LoggerFactory.getLogger(this.getClass().getName()).error("cannot send mail", e.toString());
        }

        caches.getEvent().invalidateAll();
        caches.getDebtCache().invalidateAll();
        promoteNextPendingSubscription(event);
        return true;
    }

    private void createPendingSubscription(Event event, String email, String playerRef) {
        PlayerPendingSubscription pending = new PlayerPendingSubscription();
        pending.setEvent(event);
        pending.setNrFfe(playerRef);
        pending.setCreationUser(email);
        playerPendingSubscriptionDao.persist(pending);
    }

    private String resolvePlayerReference(IPlayer player) {
        if (player.getNrffe() == null || player.getNrffe().isEmpty()) {
            return player.getFide();
        }
        return player.getNrffe();
    }

    private Integer resolveMaxSubscriptions(Event event) {
        Integer maxSubscriptions = eventOptionService.findIntOptionValue(event.getId(), EventOptionType.MAX_SUBSCRIPTIONS);
        if (maxSubscriptions != null) {
            return maxSubscriptions;
        }
        return event.getMaxSubscriptions();
    }

    private boolean isEventFull(Event event) {
        Integer maxSubscriptions = resolveMaxSubscriptions(event);
        return maxSubscriptions != null
                && maxSubscriptions > 0
                && playerSubscriptionService.countByEvent(event) >= maxSubscriptions;
    }

    private boolean isEventCollectionFull(EventCollection eventCollection) {
        if (eventCollection == null || eventCollection.getId() == null) {
            return false;
        }
        Integer maxCollectionSubscriptions = eventCollectionOptionService.findIntOptionValue(
                eventCollection.getId(), EventCollectionOptionType.MAX_SUBSCRIPTIONS);
        return maxCollectionSubscriptions != null
                && maxCollectionSubscriptions > 0
                && playerSubscriptionService.countByEventCollection(eventCollection.getId()) >= maxCollectionSubscriptions;
    }

    public void promoteNextPendingSubscription(Event event) {
        EventCollection eventCollection = event.getEventCollection();
        if (eventCollection != null && eventCollection.getId() != null) {
            promoteNextPendingSubscriptionInCollection(eventCollection);
            return;
        }

        while (true) {
            if (isEventFull(event)) {
                return;
            }

            PlayerPendingSubscription pending = playerPendingSubscriptionDao.findOldestByEvent(event);
            if (pending == null) {
                return;
            }

            if (playerSubscriptionService.findByEventAndNrffe(event, pending.getNrFfe()) != null) {
                playerPendingSubscriptionDao.remove(pending);
                continue;
            }

            IPlayer player = find.player(pending.getNrFfe(), null);
            if (player == null) {
                logger.warn("cannot find player {} in pending queue for event {}", pending.getNrFfe(), event.getId());
                playerPendingSubscriptionDao.remove(pending);
                continue;
            }

            promotePending(pending, event, player);
        }
    }

    public void promoteNextPendingSubscriptionInCollection(EventCollection eventCollection) {
        while (true) {
            if (isEventCollectionFull(eventCollection)) {
                return;
            }

            List<PlayerPendingSubscription> pendingSubscriptions =
                    playerPendingSubscriptionDao.findByEventCollection(eventCollection.getId());
            if (pendingSubscriptions.isEmpty()) {
                return;
            }

            boolean removedInvalidPending = false;
            boolean promoted = false;
            for (PlayerPendingSubscription pending : pendingSubscriptions) {
                Event pendingEvent = pending.getEvent();
                if (pendingEvent == null || pendingEvent.getId() == null) {
                    playerPendingSubscriptionDao.remove(pending);
                    removedInvalidPending = true;
                    continue;
                }

                if (isEventFull(pendingEvent)) {
                    continue;
                }

                if (playerSubscriptionService.findByEventAndNrffe(pendingEvent, pending.getNrFfe()) != null) {
                    playerPendingSubscriptionDao.remove(pending);
                    removedInvalidPending = true;
                    continue;
                }

                IPlayer player = find.player(pending.getNrFfe(), null);
                if (player == null) {
                    logger.warn(
                            "cannot find player {} in pending queue for event {}",
                            pending.getNrFfe(),
                            pendingEvent.getId());
                    playerPendingSubscriptionDao.remove(pending);
                    removedInvalidPending = true;
                    continue;
                }

                promotePending(pending, pendingEvent, player);
                promoted = true;
                break;
            }

            if (!promoted && !removedInvalidPending) {
                return;
            }
        }
    }

    private void promotePending(PlayerPendingSubscription pending, Event targetEvent, IPlayer player) {
        PlayerSubscription subscription = new PlayerSubscription();
        subscription.setEvent(targetEvent);
        subscription.setNrFfe(pending.getNrFfe());
        subscription.setCreationUser(pending.getCreationUser());

        long priceCents = ServiceUtils.calculatePrice(player, targetEvent);
        subscription.setAmountCents(priceCents);
        subscription.setStatus(priceCents == 0 ? PlayerSubscriptionStatus.PAID : PlayerSubscriptionStatus.NOT_PAID);
        playerSubscriptionService.persist(subscription);
        playerPendingSubscriptionDao.remove(pending);

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE dd MMMM yyyy", Locale.FRANCE);
        RegistrationMail registrationMail = new RegistrationMail();
        registrationMail.setName(player.getFirstname() + " " + player.getName());
        if (priceCents > 0) {
            registrationMail.setAmount(ServiceUtils.toEuros(priceCents) + " Euros");
        }
        registrationMail.setEvenDate(sdf.format(targetEvent.getStartDateAsDate()));
        registrationMail.setEventName(targetEvent.getName());
        String baseUrl = properties.getProperty("baseurl");
        registrationMail.setBaseUrl(baseUrl);
        registrationMail.setLoginUrl(buildLoginUrl(pending.getCreationUser(), registrationMail.getName(), baseUrl));

        try {
            sendMail.send(
                    registrationMail,
                    pending.getCreationUser(),
                    "Confirmation inscription " + targetEvent.getName());
        } catch (Exception e) {
            LoggerFactory.getLogger(this.getClass().getName()).error("cannot send mail", e);
        }
    }

    private String buildLoginUrl(String email, String issuer, String baseUrl) {
        String safeBaseUrl = baseUrl == null || baseUrl.isBlank() ? "" : baseUrl;
        String safeEmail = email == null ? "" : email.trim();
        String safeIssuer = issuer == null || issuer.isBlank() ? safeEmail : issuer.trim();
        String jwt = Jwts.builder()
                .subject(safeEmail)
                .issuer(safeIssuer)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30))
                .signWith(config.getKeys(), Config.JWT_ALGORITHM)
                .compact();

        String redirect = URLEncoder.encode("/event/my", StandardCharsets.UTF_8);
        String encodedJwt = URLEncoder.encode(jwt, StandardCharsets.UTF_8);
        String loginPath = "/login?jwt=" + encodedJwt + "&redirect_uri=" + redirect;

        if (safeBaseUrl.isBlank()) {
            return loginPath;
        }
        if (safeBaseUrl.endsWith("/")) {
            return safeBaseUrl.substring(0, safeBaseUrl.length() - 1) + loginPath;
        }
        return safeBaseUrl + loginPath;
    }

}

package com.github.gcolin.payment;

import com.github.gcolin.event.Event;
import com.github.gcolin.platform.Caches;
import com.github.gcolin.platform.SendMail;
import com.github.gcolin.platform.ServiceUtils;
import com.github.gcolin.player.Find;
import com.github.gcolin.player.IPlayer;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.registration.PlayerSubscriptionDao;
import com.github.gcolin.registration.PlayerSubscriptionOption;
import com.github.gcolin.registration.PlayerSubscriptionOptionDao;
import com.github.gcolin.registration.PlayerSubscriptionOptionStatus;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebtService {

    private PlayerSubscriptionDao playerSubscriptionService;
    private PlayerSubscriptionOptionDao playerSubscriptionOptionService;
    private Find find;
    private SendMail sendMail;
    private Caches caches;
    private PaymentDao paymentService;

    private static Logger logger = LoggerFactory.getLogger(DebtService.class.getName());

    public void setPlayerSubscriptionDao(PlayerSubscriptionDao playerSubscriptionService) {
        this.playerSubscriptionService = playerSubscriptionService;
    }

    public void setPlayerSubscriptionOptionDao(PlayerSubscriptionOptionDao playerSubscriptionOptionService) {
        this.playerSubscriptionOptionService = playerSubscriptionOptionService;
    }

    public void setFind(Find find) {
        this.find = find;
    }

    public void setSendMail(SendMail sendMail) {
        this.sendMail = sendMail;
    }

    public void setCaches(Caches caches) {
        this.caches = caches;
    }

    public void setPaymentDao(PaymentDao paymentService) {
        this.paymentService = paymentService;
    }

    public double calculateDebt(String email) {
        double out = 0;
        List<PlayerSubscription> subs = playerSubscriptionService.findByCreationUser(email);

        for (PlayerSubscription sub : subs) {
            if (sub.getStatus() == PlayerSubscriptionStatus.NOT_PAID) {
                Event event = sub.getEvent();
                IPlayer player = find.player(sub.getNrFfe(), null);
                out += resolveAmount(sub, player, event);
            }
        }

        for (PlayerSubscriptionOption option : playerSubscriptionOptionService.findNotPaidByCreationUser(email)) {
            out += resolveOptionAmount(option);
        }

        return out;
    }

    public Payment createPayment(String email, double amount) {
        Payment payment = new Payment();
        payment.setUserEmail(email);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setType(PaymentType.CARD);
        payment.setAmount(amount);
        List<PlayerSubscription> subs = new ArrayList<PlayerSubscription>();
        List<PlayerSubscription> allSubs = playerSubscriptionService.findByCreationUser(email);

        for (PlayerSubscription sub : allSubs) {
            if (sub.getStatus() == PlayerSubscriptionStatus.NOT_PAID) {
                IPlayer player = find.player(sub.getNrFfe(), null);
                Event event = sub.getEvent();
                if (sub.getAmountCents() == null) {
                    sub.setAmountCents(toAmountCents(resolveAmount(sub, player, event)));
                }
                subs.add(sub);
                sub.setPayment(payment);
            }
        }

        payment.setSubscriptions(subs);

        paymentService.persist(payment);

        for (PlayerSubscriptionOption option : playerSubscriptionOptionService.findNotPaidByCreationUser(email)) {
            if (option.getAmountCents() == null) {
                option.setAmountCents(0L);
            }
            option.setPayment(payment);
            playerSubscriptionOptionService.merge(option);
        }

        return payment;
    }

    public synchronized void payment(double amount, String email, String sessionId, String intent) {
        List<PlayerSubscription> subs = playerSubscriptionService.findByCreationUserWithEvents(email);
        for (PlayerSubscription sub : subs) {
            if (sub.getStatus() == PlayerSubscriptionStatus.NOT_PAID) {
                Event event = sub.getEvent();
                IPlayer player = find.player(sub.getNrFfe(), null);
                double eventPrice = resolveAmount(sub, player, event);
                if (amount >= eventPrice) {
                    amount -= eventPrice;
                    if (sub.getAmountCents() == null) {
                        sub.setAmountCents(toAmountCents(eventPrice));
                    }
                    sub.setStatus(PlayerSubscriptionStatus.PAID);
                    playerSubscriptionService.persist(sub);
                    logger.info(
                            "[email={}] Payment for event {} for {} - {} euros",
                            email,
                            event.getName(),
                            sub.getNrFfe(),
                            eventPrice);

                    SimpleDateFormat sdf = new SimpleDateFormat("EEEE dd MMMM yyyy", Locale.FRANCE);
                    PaymentMail registrationMail = new PaymentMail();
                    registrationMail.setName(player.getFirstname() + " " + player.getName());
                    if (eventPrice > 0) {
                        registrationMail.setAmount(eventPrice + " Euros");
                    }
                    registrationMail.setEvenDate(sdf.format(event.getStartDateAsDate()));
                    registrationMail.setEventName(event.getName());
                    registrationMail.setReference(email);

                    String eventIdString = String.valueOf(event.getId());
                    if (caches.getEvent().getIfPresent(eventIdString) != null) {
                        caches.getEvent().invalidate(String.valueOf(event.getId()));
                    }
                    try {
                        sendMail.send(registrationMail, email, "Confirmation paiement " + event.getName());
                    } catch (Exception e) {
                        logger.error("[email={}] {}", email, e.toString());
                    }

                } else {
                    logger.error("[email={}] cannot pay event {}", email, event.getName());
                }
            }
        }

        for (PlayerSubscriptionOption option : playerSubscriptionOptionService.findNotPaidByCreationUser(email)) {
            double optionPrice = resolveOptionAmount(option);
            if (amount >= optionPrice) {
                amount -= optionPrice;
                if (option.getAmountCents() == null) {
                    option.setAmountCents(toAmountCents(optionPrice));
                }
                option.setStatus(PlayerSubscriptionOptionStatus.PAID);
                playerSubscriptionOptionService.merge(option);
                PlayerSubscription parent = option.getPlayerSubscription();
                logger.info(
                        "[email={}] Payment for option {} (sub {}) - {} euros",
                        email,
                        option.getId(),
                        parent == null ? null : parent.getId(),
                        optionPrice);
            } else {
                logger.error("[email={}] cannot pay option {}", email, option.getId());
            }
        }

        Payment payment = paymentService.findBySessionId(sessionId);
        if (payment == null) {
            logger.error("cannot find payment for strip id " + sessionId);
        } else {
            payment.setStatus(PaymentStatus.PAID);
            payment.setStripeIntent(intent);
            paymentService.persist(payment);
        }

        caches.getDebtCache().invalidateAll();
        logger.info("[email={}] After event payment, {} euros {}", email, amount, intent);
    }

    private double resolveAmount(PlayerSubscription sub, IPlayer player, Event event) {
        if (sub.getAmountCents() != null) {
            return sub.getAmountCents() / 100d;
        }
        return ServiceUtils.toEuros(ServiceUtils.calculatePrice(player, event));
    }

    private double resolveOptionAmount(PlayerSubscriptionOption option) {
        if (option.getAmountCents() == null) {
            return 0d;
        }
        return option.getAmountCents() / 100d;
    }

    private Long toAmountCents(double amount) {
        return Math.round(amount * 100d);
    }
}

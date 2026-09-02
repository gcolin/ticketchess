package com.github.gcolin.payment;

import com.github.gcolin.club.SeasonScope;
import com.github.gcolin.event.Event;
import com.github.gcolin.event.EventStatus;
import com.github.gcolin.membership.Membership;
import com.github.gcolin.membership.MembershipDao;
import com.github.gcolin.membership.MembershipStatus;
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
    private MembershipDao membershipDao;
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

    public void setMembershipDao(MembershipDao membershipDao) {
        this.membershipDao = membershipDao;
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

    public static boolean isPayableEvent(Event event) {
        return event != null && event.getStatus() == EventStatus.ACTIVE;
    }

    public double calculateDebt(String email) {
        return calculateEventDebt(email) + calculateMembershipDebt(email, SeasonScope.all());
    }

    public double calculateEventDebt(String email) {
        double out = 0;
        List<PlayerSubscription> subs = playerSubscriptionService.findByCreationUser(email);

        for (PlayerSubscription sub : subs) {
            if (sub.getStatus() == PlayerSubscriptionStatus.NOT_PAID && isPayableEvent(sub.getEvent())) {
                Event event = sub.getEvent();
                IPlayer player = find.player(sub.getNrFfe(), null);
                out += resolveAmount(sub, player, event);
            }
        }

        for (PlayerSubscriptionOption option : playerSubscriptionOptionService.findNotPaidByCreationUser(email)) {
            PlayerSubscription parent = option.getPlayerSubscription();
            if (parent != null && isPayableEvent(parent.getEvent())) {
                out += resolveOptionAmount(option);
            }
        }

        return out;
    }

    public double calculateMembershipDebt(String email, SeasonScope scope) {
        double out = 0;
        for (Membership membership : membershipDao.findApprovedUnpaidByUser(email, scope)) {
            out += membership.getAmountCents() / 100d;
        }
        return out;
    }

    public Payment createPayment(String email, double amount) {
        Payment payment = new Payment();
        payment.setUserEmail(email);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setType(PaymentType.CARD);
        payment.setAmount(amount);
        List<PlayerSubscription> subs = new ArrayList<>();
        List<PlayerSubscription> allSubs = playerSubscriptionService.findByCreationUser(email);

        for (PlayerSubscription sub : allSubs) {
            if (sub.getStatus() == PlayerSubscriptionStatus.NOT_PAID && isPayableEvent(sub.getEvent())) {
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
            PlayerSubscription parent = option.getPlayerSubscription();
            if (parent == null || !isPayableEvent(parent.getEvent())) {
                continue;
            }
            if (option.getAmountCents() == null) {
                option.setAmountCents(0L);
            }
            option.setPayment(payment);
            playerSubscriptionOptionService.merge(option);
        }

        return payment;
    }

    public Payment createMembershipPayment(String email, double amount, SeasonScope scope) {
        Payment payment = new Payment();
        payment.setUserEmail(email);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setType(PaymentType.CARD);
        payment.setAmount(amount);

        List<Membership> memberships = new ArrayList<>();
        for (Membership membership : membershipDao.findApprovedUnpaidByUser(email, scope)) {
            memberships.add(membership);
            membership.setPayment(payment);
            membershipDao.merge(membership);
        }
        payment.setMemberships(memberships);
        paymentService.persist(payment);
        return payment;
    }

    public synchronized void payment(double amount, String email, String sessionId, String intent) {
        Payment payment = paymentService.findBySessionId(sessionId);
        if (payment == null) {
            logger.error("cannot find payment for strip id " + sessionId);
            caches.getDebtCache().invalidateAll();
            return;
        }

        List<PlayerSubscription> subs = playerSubscriptionService.findByPaymentId(
                payment.getId().intValue());
        List<Membership> linkedMemberships = membershipDao.findByPaymentId(payment.getId().intValue());
        boolean membershipOnlyPayment = !linkedMemberships.isEmpty() && subs.isEmpty();
        if (subs.isEmpty() && !membershipOnlyPayment) {
            subs = playerSubscriptionService.findByCreationUserWithEvents(email);
        }

        for (PlayerSubscription sub : subs) {
            if (sub.getStatus() != PlayerSubscriptionStatus.NOT_PAID || !isPayableEvent(sub.getEvent())) {
                continue;
            }
            if (sub.getPayment() != null && !sub.getPayment().getId().equals(payment.getId())) {
                continue;
            }
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

        List<PlayerSubscriptionOption> options =
                playerSubscriptionOptionService.findByPaymentId(payment.getId().intValue());
        if (options.isEmpty() && !membershipOnlyPayment) {
            options = playerSubscriptionOptionService.findNotPaidByCreationUser(email);
        }

        for (PlayerSubscriptionOption option : options) {
            PlayerSubscription parent = option.getPlayerSubscription();
            if (parent == null || !isPayableEvent(parent.getEvent())) {
                continue;
            }
            if (option.getPayment() != null && !option.getPayment().getId().equals(payment.getId())) {
                continue;
            }
            double optionPrice = resolveOptionAmount(option);
            if (amount >= optionPrice) {
                amount -= optionPrice;
                if (option.getAmountCents() == null) {
                    option.setAmountCents(toAmountCents(optionPrice));
                }
                option.setStatus(PlayerSubscriptionOptionStatus.PAID);
                playerSubscriptionOptionService.merge(option);
                logger.info(
                        "[email={}] Payment for option {} (sub {}) - {} euros",
                        email,
                        option.getId(),
                        parent.getId(),
                        optionPrice);
            } else {
                logger.error("[email={}] cannot pay option {}", email, option.getId());
            }
        }

        for (Membership membership : linkedMemberships) {
            if (membership.getStatus() != MembershipStatus.APPROVED) {
                continue;
            }
            double membershipPrice = membership.getAmountCents() / 100d;
            if (amount >= membershipPrice) {
                amount -= membershipPrice;
                membership.setStatus(MembershipStatus.PAID);
                membershipDao.merge(membership);
                logger.info(
                        "[email={}] Payment for membership {} ({}) - {} euros",
                        email,
                        membership.getId(),
                        membership.getNrFfe(),
                        membershipPrice);
            } else {
                logger.error("[email={}] cannot pay membership {}", email, membership.getId());
            }
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setStripeIntent(intent);
        paymentService.persist(payment);

        caches.getDebtCache().invalidateAll();
        logger.info("[email={}] After payment, {} euros remaining {}", email, amount, intent);
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

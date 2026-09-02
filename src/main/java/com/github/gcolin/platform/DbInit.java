package com.github.gcolin.platform;

import com.github.gcolin.club.ClubSeason;
import com.github.gcolin.player.CustomPlayer;
import com.github.gcolin.event.Event;
import com.github.gcolin.event.EventGroup;
import com.github.gcolin.event.EventOption;
import com.github.gcolin.event.EventOptionType;
import com.github.gcolin.membership.License;
import com.github.gcolin.membership.LicensePrice;
import com.github.gcolin.membership.MembershipOption;
import com.github.gcolin.membership.MembershipOptionAccessRule;
import com.github.gcolin.membership.MembershipOptionType;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.event.EventStatus;
import com.github.gcolin.membership.LicensePriceCalculator;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.gcolin.event.EventType;

public class DbInit {

    private final EntityManagerFactory emf;

    private Logger logger = LoggerFactory.getLogger(DbInit.class);

    public DbInit(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public void initEvents() {
        EntityManager em = emf.createEntityManager();
        Long count = em.createQuery("select count(e) from Event e", Long.class).getSingleResult();

        if (count == 0) {
            Event evt = null;
            Event evt2 = null;
            Event evt9 = null;
            em.getTransaction().begin();
            EventGroup evg = new EventGroup();
            evg.setName("a super group");
            evg.setShortname("aaa");

            em.persist(evg);

            for (int i = 0; i < 10; i++) {
                Event event1 = new Event();
                event1.setName("Sample Event " + (i + 1));
                event1.setStartDate(LocalDate.now().atStartOfDay().plus(1L, ChronoUnit.DAYS));
                event1.setEndDate(LocalDate.now().atStartOfDay().plus(1L, ChronoUnit.DAYS));
                event1.setPriceCents(1000L);
                event1.setStatus(EventStatus.ACTIVE);
                if (i % 3 == 0) {
                    event1.setEventType(com.github.gcolin.event.EventType.RAPID);
                } else if (i % 3 == 1) {
                    event1.setEventType(com.github.gcolin.event.EventType.BLITZ);
                } else {
                    event1.setEventType(com.github.gcolin.event.EventType.STANDARD);
                }
                if (i == 2) {
                    evt = event1;
                }
                if (i == 3) {
                    evt2 = event1;
                }
                if (i > 5) {
                    event1.setEventGroup(evg);
                }

                if (i == 5) {
                    event1.setStartDate(LocalDate.now().atStartOfDay().plus(1L, ChronoUnit.DAYS));
                }

                if (i == 4) {
                    evt9 = event1;
                }

                em.persist(event1);

                if (i == 1) {
                    EventOption maxSubsOption = new EventOption();
                    maxSubsOption.setEvent(event1);
                    maxSubsOption.setOptionType(EventOptionType.MAX_SUBSCRIPTIONS);
                    maxSubsOption.setValue("0");
                    em.persist(maxSubsOption);
                }
            }
            List<CustomPlayer> players = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                CustomPlayer player = new CustomPlayer();
                player.setBirthDate("2010-10-10");
                player.setCreationUser("test@test.com");
                player.setElo("1000N");
                player.setFirstname("John" + (i + 1));
                player.setGender(true);
                player.setLicence("12345" + i);
                player.setName("Doe");
                em.persist(player);

                PlayerSubscription sub = new PlayerSubscription();
                sub.setCreationUser("test@test.com");
                sub.setEvent(evt);
                sub.setNrFfe("@" + player.getId());
                sub.setAmountCents(1000L);
                sub.setStatus(PlayerSubscriptionStatus.PAID);
                em.persist(sub);
                players.add(player);
            }
            {
                PlayerSubscription sub = new PlayerSubscription();
                sub.setCreationUser("test@test.com");
                sub.setEvent(evt2);
                sub.setNrFfe("X82897");
                sub.setAmountCents(1000L);
                sub.setStatus(PlayerSubscriptionStatus.PAID);
                em.persist(sub);
            }
            for (int i = 0; i < 3; i++) {
                PlayerSubscription sub = new PlayerSubscription();
                sub.setCreationUser("test@test.com");
                sub.setEvent(evt9);
                sub.setNrFfe("@" + players.get(i).getId());
                sub.setAmountCents(1000L);
                sub.setStatus(PlayerSubscriptionStatus.values()[i]);
                em.persist(sub);
            }
            em.getTransaction().commit();
            logger.info("database initialized with default events");
        }
    }

    public void initMembershipOptions() {
        EntityManager em = emf.createEntityManager();
        Long count = em.createQuery("select count(m) from MembershipOption m", Long.class).getSingleResult();

        if (count == 0) {
            List<License> all = em.createQuery("select l from License l", License.class).getResultList();
            License licenseA = all.stream().filter(l -> "A".equals(l.getName())).findFirst().orElse(null);
            em.getTransaction().begin();

            MembershipOption option1 = new MembershipOption();
            option1.setOptionType(MembershipOptionType.COURSES);
            option1.setOptionValue("Cours adulte Mardi 19h00/19h50");
            option1.setAmountCents(12500);
            option1.setAccessRule(MembershipOptionAccessRule.NON_YOUNG_ONLY);
            em.persist(option1);

            MembershipOption option2 = new MembershipOption();
            option2.setOptionType(MembershipOptionType.COURSES);
            option2.setOptionValue("Cours confirmé enfant");
            option2.setAmountCents(12500);
            option2.setAccessRule(MembershipOptionAccessRule.YOUNG_ONLY);
            option2.setLicense(licenseA); // Assuming license A
            em.persist(option2);

            MembershipOption option3 = new MembershipOption();
            option3.setOptionType(MembershipOptionType.COURSES);
            option3.setOptionValue("Cours débutant enfant Mercredi 14h00/14h50 ou 15h00/15h50");
            option3.setAmountCents(5000);
            option3.setAccessRule(MembershipOptionAccessRule.YOUNG_ONLY);
            em.persist(option3);

            /*MembershipOption option4 = new MembershipOption();
            option4.setOptionType(MembershipOptionType.CLUB_CHAMPIONSHIP);
            option4.setOptionValue("Interclub");
            option4.setAmountCents(0);
            option4.setLicense(licenseA); // Assuming license A
            em.persist(option4);

            MembershipOption option5 = new MembershipOption();
            option5.setOptionType(MembershipOptionType.CLUB_CHAMPIONSHIP);
            option5.setOptionValue("Coupe loubatière");
            option5.setAmountCents(0);
            option5.setLicense(licenseA); // Assuming license A
            em.persist(option5);

            MembershipOption option6 = new MembershipOption();
            option6.setOptionType(MembershipOptionType.CLUB_CHAMPIONSHIP);
            option6.setOptionValue("Championnat individuel");
            option6.setAmountCents(0);
            option6.setLicense(licenseA); // Assuming license A
            em.persist(option6);*/

            MembershipOption option7 = new MembershipOption();
            option7.setOptionType(MembershipOptionType.COURSES);
            option7.setOptionValue("Cours Mardi 17h00/17h50");
            option7.setAmountCents(12500);
            option7.setAccessRule(MembershipOptionAccessRule.ADMIN);
            option7.setLicense(licenseA); // Assuming license A
            em.persist(option7);

            MembershipOption option8 = new MembershipOption();
            option8.setOptionType(MembershipOptionType.COURSES);
            option8.setOptionValue("Cours Jeudi 17h00/17h50");
            option8.setAmountCents(12500);
            option8.setAccessRule(MembershipOptionAccessRule.ADMIN);
            option8.setLicense(licenseA); // Assuming license A
            em.persist(option8);

            MembershipOption option9 = new MembershipOption();
            option9.setOptionType(MembershipOptionType.COURSES);
            option9.setOptionValue("Cours Lundi 17h00/17h50");
            option9.setAmountCents(12500);
            option9.setAccessRule(MembershipOptionAccessRule.ADMIN);
            option9.setLicense(licenseA); // Assuming license A
            em.persist(option9);

            MembershipOption option10 = new MembershipOption();
            option10.setOptionType(MembershipOptionType.COURSES);
            option10.setOptionValue("Cours Mercredi 14h00/14h50");
            option10.setAmountCents(5000);
            option10.setAccessRule(MembershipOptionAccessRule.ADMIN);
            em.persist(option10);

            MembershipOption option11 = new MembershipOption();
            option11.setOptionType(MembershipOptionType.COURSES);
            option11.setOptionValue("Cours Mercredi 15h00/15h50");
            option11.setAmountCents(5000);
            option11.setAccessRule(MembershipOptionAccessRule.ADMIN);
            em.persist(option11);

            em.getTransaction().commit();
            logger.info("database initialized with membership options");
        }
    }

    public void initClubSeason() {
        EntityManager em = emf.createEntityManager();
        Long count = em.createQuery("select count(s) from ClubSeason s", Long.class).getSingleResult();
        if (count == 0) {
            em.getTransaction().begin();
            int year = LocalDate.now().getMonthValue() >= 9 ? LocalDate.now().getYear() : LocalDate.now().getYear() - 1;
            ClubSeason season = new ClubSeason();
            season.setName(year + "/" + (year + 1));
            season.setStartDate(LocalDate.of(year, 9, 1));
            season.setEndDate(LocalDate.of(year + 1, 8, 31));
            season.setCurrent(true);
            em.persist(season);
            em.getTransaction().commit();
            logger.info("database initialized with default club season {}", season.getName());
        }
    }

    public void initLicenses() {
        EntityManager em = emf.createEntityManager();
        Long count = em.createQuery("select count(l) from License l", Long.class).getSingleResult();

        if (count == 0) {
            em.getTransaction().begin();

            // Create licenses
            License licenseA = new License("A");
            licenseA.setAccessRule(MembershipOptionAccessRule.ALL);
            License licenseB = new License("B");
            licenseB.setAccessRule(MembershipOptionAccessRule.ALL);
            em.persist(licenseA);
            em.persist(licenseB);
            em.flush(); // Ensure IDs are generated

            // Define all categories for which we need prices
            String[] allCategories = {"PpoM", "PpoF", "PouM", "PouF", "PupM", "PupF",
                                      "MinM", "MinF", "BenM", "BenF", "CadM", "CadF",
                                      "JunM", "JunF", "SenM", "SenF", "SepM", "SepF",
                                      "VetM", "VetF"};

            // Initialize license prices based on LicensePriceCalculator
            int clubPrice = 3000;
            for (String category : allCategories) {
                // License A prices
                int priceA = LicensePriceCalculator.getPriceForLicenseA(category) + clubPrice;
                LicensePrice priceAEntity = new LicensePrice(category, priceA, licenseA);
                em.persist(priceAEntity);

                // License B prices
                int priceB = LicensePriceCalculator.getPriceForLicenseB(category) + clubPrice;
                LicensePrice priceBEntity = new LicensePrice(category, priceB, licenseB);
                em.persist(priceBEntity);
            }

            em.getTransaction().commit();
            logger.info("database initialized with licenses and license prices");
        }
    }
}

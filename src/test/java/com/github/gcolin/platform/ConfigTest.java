package com.github.gcolin.platform;

import static org.mockito.Mockito.mock;

import jakarta.servlet.ServletContext;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigTest {

    @TempDir
    Path tempConfigDir;

    private Config config;
    private String previousConfigDir;

    @BeforeEach
    void setUp() {
        previousConfigDir = System.getProperty("CONFIG_DIR");
        System.setProperty("CONFIG_DIR", tempConfigDir.toAbsolutePath().toString());
        ServletContext servletContext = mock(ServletContext.class);
        config = new Config();
        config.init(servletContext);
    }

    @AfterEach
    void tearDown() {
        if (previousConfigDir == null) {
            System.clearProperty("CONFIG_DIR");
        } else {
            System.setProperty("CONFIG_DIR", previousConfigDir);
        }
    }

    @Test
    void testGetAdmins() {
        Assertions.assertNotNull(config.getAdmins());
        Assertions.assertTrue(config.getAdmins().size() >= 0);
    }

    @Test
    void testGetKeys() {
        Assertions.assertNotNull(config.getKeys());
    }

    @Test
    void testGetLoginUrl() {
        Assertions.assertNotNull(config.getLoginUrl());
        Assertions.assertTrue(config.getLoginUrl().contains("state="));
    }

    @Test
    void testGetPage() {
        Assertions.assertNotNull(config.getPage());
        Assertions.assertNotNull(config.getPage().getTitle());
    }

    @Test
    void isStripeSimulatedRequiresSimuledFlagWithoutPublicKey() {
        Properties props = new Properties();
        props.setProperty("stripe.simuled", "true");
        props.setProperty("stripe.public", "");
        Assertions.assertTrue(Config.isStripeSimulated(props));
        Assertions.assertNull(Config.getStripePublicKey(props));
    }

    @Test
    void isStripeSimulatedIgnoredWhenPublicKeySet() {
        Properties props = new Properties();
        props.setProperty("stripe.simuled", "true");
        props.setProperty("stripe.public", "pk_test_abc");
        Assertions.assertFalse(Config.isStripeSimulated(props));
        Assertions.assertEquals("pk_test_abc", Config.getStripePublicKey(props));
    }

    @Test
    void stripeCardEnabledDefaultsToTrue() {
        Properties props = new Properties();
        Assertions.assertTrue(Config.isStripeCardEnabledForEvents(props));
        Assertions.assertTrue(Config.isStripeCardEnabledForMemberships(props));
    }

    @Test
    void stripeCardEnabledRespectsProperty() {
        Properties props = new Properties();
        props.setProperty("stripe.card.events", "false");
        props.setProperty("stripe.card.memberships", "false");
        Assertions.assertFalse(Config.isStripeCardEnabledForEvents(props));
        Assertions.assertFalse(Config.isStripeCardEnabledForMemberships(props));
    }

    @Test
    void bankTransferEnabledDefaultsToTrue() {
        Properties props = new Properties();
        Assertions.assertTrue(Config.isBankTransferEnabledForEvents(props));
        Assertions.assertTrue(Config.isBankTransferEnabledForMemberships(props));
    }

    @Test
    void bankTransferEnabledRespectsProperty() {
        Properties props = new Properties();
        props.setProperty("stripe.transfer.events", "false");
        props.setProperty("stripe.transfer.memberships", "false");
        Assertions.assertFalse(Config.isBankTransferEnabledForEvents(props));
        Assertions.assertFalse(Config.isBankTransferEnabledForMemberships(props));
    }
}

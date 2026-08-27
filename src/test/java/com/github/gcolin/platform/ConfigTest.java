package com.github.gcolin.platform;

import static org.mockito.Mockito.mock;

import jakarta.servlet.ServletContext;
import java.util.Properties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigTest {

    private Config config;

    @BeforeEach
    void setUp() {
        ServletContext servletContext = mock(ServletContext.class);
        config = new Config();
        config.init(servletContext);
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
}

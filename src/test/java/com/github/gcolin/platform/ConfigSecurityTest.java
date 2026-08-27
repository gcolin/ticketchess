package com.github.gcolin.platform;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class ConfigSecurityTest {

    @Test
    void shouldAllowTestModeWithoutOauth() {
        Config config = new Config();
        config.getProperties().setProperty("testmode", "true");
        config.getProperties().setProperty("baseurl", "http://localhost:8080");
        config.applyRuntime();
        assertDoesNotThrow(config::validateSecurity);
        assertTrue(config.isAuthSimEnabled());
    }

    @Test
    void shouldRejectProductionWithoutOauth() {
        Config config = new Config();
        config.getProperties().setProperty("testmode", "false");
        config.getProperties().setProperty("baseurl", "http://localhost:8080");
        config.getProperties()
                .setProperty("jwt.key", "01234567890123456789012345678901234567890123456789012");
        config.applyRuntime();

        IllegalStateException ex = assertThrows(IllegalStateException.class, config::validateSecurity);
        assertTrue(ex.getMessage().contains("OAuth"));
    }

    @Test
    void shouldRejectProductionWithDefaultJwtKey() {
        Config config = new Config();
        config.getProperties().setProperty("testmode", "false");
        config.getProperties().setProperty("oauth.clientId", "client");
        config.getProperties().setProperty("baseurl", "http://localhost:8080");
        config.getProperties().setProperty("jwt.key", "change-me-in-production-use-a-long-random-string");
        config.applyRuntime();

        IllegalStateException ex = assertThrows(IllegalStateException.class, config::validateSecurity);
        assertTrue(ex.getMessage().contains("jwt.key"));
    }

    @Test
    void shouldAcceptProductionWithOauthAndSecureJwtKey() {
        Config config = new Config();
        config.getProperties().setProperty("testmode", "false");
        config.getProperties().setProperty("oauth.clientId", "client");
        config.getProperties().setProperty("baseurl", "http://localhost:8080");
        config.getProperties()
                .setProperty("jwt.key", "01234567890123456789012345678901234567890123456789012");
        config.applyRuntime();

        assertDoesNotThrow(config::validateSecurity);
        assertFalse(config.isAuthSimEnabled());
    }

    @Test
    void shouldRejectRuntimeJwtWeakeningInProduction() throws Exception {
        Config config = new Config();
        config.getProperties().setProperty("testmode", "false");
        config.getProperties().setProperty("oauth.clientId", "client");
        config.getProperties().setProperty("baseurl", "http://localhost:8080");
        config.getProperties()
                .setProperty("jwt.key", "01234567890123456789012345678901234567890123456789012");
        config.applyRuntime();
        assertDoesNotThrow(config::validateSecurity);

        IOException ex = assertThrows(IOException.class, () -> config.updateProperties(
                java.util.Map.of("jwt.key", "change-me-in-production-use-a-long-random-string")));
        assertTrue(ex.getMessage().contains("jwt.key"));
        assertTrue(config.hasSecureJwtKey());
    }
}

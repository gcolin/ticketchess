package com.github.gcolin.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConfigOauthTest {

    @Test
    void shouldUseOauthKeysWhenPresent() {
        Config config = new Config();
        config.getProperties().setProperty("oauth.clientId", "cid");
        config.getProperties().setProperty("oauth.authorizationUrl", "https://accounts.google.com/o/oauth2/v2/auth");
        config.getProperties().setProperty("oauth.tokenUrl", "https://oauth2.googleapis.com/token");
        config.getProperties().setProperty("baseurl", "http://localhost:8080");
        config.applyRuntime();

        assertTrue(config.isOauthEnabled());
        assertEquals("https://oauth2.googleapis.com/token", config.getOauthTokenUrl());
        assertNull(config.getOauthLogoutUrl());
        assertEquals("/oauth-callback", config.getOauthCallbackPath());
        assertTrue(config.getLoginUrl().contains("accounts.google.com"));
    }

    @Test
    void shouldKeepLegacyKeycloakKeys() {
        Config config = new Config();
        config.getProperties().setProperty("keycloak.CLIENT_ID", "ticket");
        config.getProperties()
                .setProperty("keycloak.url", "https://oauth.example.org/realms/master/protocol/openid-connect/auth");
        config.getProperties().setProperty("baseurl", "http://localhost:8080");
        config.applyRuntime();

        assertTrue(config.isOauthEnabled());
        assertEquals(
                "https://oauth.example.org/realms/master/protocol/openid-connect/token", config.getOauthTokenUrl());
        assertEquals(
                "https://oauth.example.org/realms/master/protocol/openid-connect/logout", config.getOauthLogoutUrl());
        assertEquals("https://oauth.example.org/realms/master/account", config.getOauthAccountUrl());
        assertEquals("/keycloak-callback", config.getOauthCallbackPath());
    }

    @Test
    void invoicePropertyShouldFallBackToOrg() {
        Config config = new Config();
        config.getProperties().setProperty("org.name", "Test Club");
        assertEquals("Test Club", Config.configured(config.getProperties(), "invoice.seller.name", "org.name"));
        config.getProperties().setProperty("invoice.seller.name", "Seller");
        assertEquals("Seller", Config.configured(config.getProperties(), "invoice.seller.name", "org.name"));
    }

    @Test
    void authSimWhenOauthDisabled() {
        Config config = new Config();
        config.getProperties().setProperty("baseurl", "http://localhost:8080");
        config.applyRuntime();
        assertFalse(config.isOauthEnabled());
        assertTrue(config.getLoginUrl().contains("auth-sim?"));
    }

    @Test
    void orgFormValuesShouldPrefillCurrentConfig() {
        Config config = new Config();
        config.getProperties().setProperty("title", "Event Test");
        config.getProperties().setProperty("org.name", "Test Club");
        config.getProperties().setProperty("org.email", "club@example.org");
        config.getProperties().setProperty("keycloak.CLIENT_ID", "ticket");
        config.getProperties()
                .setProperty("keycloak.url", "https://oauth.example.org/realms/master/protocol/openid-connect/auth");

        var form = config.getOrgFormValues();

        assertEquals("Event Test", form.get("title"));
        assertEquals("Test Club", form.get("org.name"));
        assertEquals("club@example.org", form.get("org.email"));
        assertEquals("Test Club", form.get("invoice.seller.name"));
        assertEquals("club@example.org", form.get("invoice.seller.email"));
        assertEquals("ticket", form.get("oauth.clientId"));
        assertEquals(
                "https://oauth.example.org/realms/master/protocol/openid-connect/auth",
                form.get("oauth.authorizationUrl"));
    }
}

package com.github.gcolin.registration;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.github.gcolin.platform.PlaywrightBaseTest;
import com.github.gcolin.player.CustomPlayer;

@Tag("integration")
public class PlayerSubscriptionAdminPendingTest extends PlaywrightBaseTest {

    private static final String EVENT_ID = "1";
    private static final String PENDING_NRFFE = "343428916";

    @Test
    public void adminCanDeletePendingQueuePlayer() {
        Page page = browserContext.newPage();

        page.navigate(BASE_URL + "/event/" + EVENT_ID);

        Locator loginButton = page.locator("#googleauth2");
        assertTrue(loginButton.isVisible());
        loginButton.click();

        navigateToEventEdit(page, EVENT_ID);

        Locator maxSubscriptions = page.locator("#event_maxsubscriptions");
        assertTrue(maxSubscriptions.isVisible());
        maxSubscriptions.fill("1");
        page.selectOption("#event_status", "ACTIVE");

        page.locator("#event_submit").click();
        String savedUrl = getBaseUrl(page.url());
        assertTrue(
                savedUrl.equals(BASE_URL + "/event/" + EVENT_ID)
                        || savedUrl.equals(BASE_URL + "/event/" + EVENT_ID + "/edit"));

        page.navigate(BASE_URL + "/event/" + EVENT_ID + "/register");
        registerPlayerByQuery(page, "Gael colin");
        assertEquals(BASE_URL + "/event/my", getBaseUrl(page.url()));

        page.navigate(BASE_URL + "/event/" + EVENT_ID + "/register");
        registerPlayerByQuery(page, PENDING_NRFFE);
        assertEquals(BASE_URL + "/event/my", getBaseUrl(page.url()));

        page.navigate(BASE_URL + "/playersubscription-admin");
        assertThat(page.locator("h1")).isVisible();

        Locator pendingRow = page.locator("tr:has(input[name='deletePendingId'])")
                .filter(new Locator.FilterOptions().setHasText(PENDING_NRFFE));
        assertThat(pendingRow).hasCount(1);

        page.onDialog(dialog -> {
            assertTrue(dialog.message().contains("sûr") || dialog.message().toLowerCase().contains("sure"));
            dialog.accept();
        });
        pendingRow.locator("button.btn-outline-danger").click();

        page.waitForURL("**/playersubscription-admin/result**");
        assertThat(page.locator("tr:has(input[name='deletePendingId'])")
                .filter(new Locator.FilterOptions().setHasText(PENDING_NRFFE)))
                .hasCount(0);
        assertThat(page.locator(".alert-success")
                .filter(new Locator.FilterOptions().setHasText("CustomPlayer")))
                .containsText("1");

        page.close();
    }

    private void registerPlayerByQuery(Page page, String query) {
        Locator playerInput = page.locator("#player");
        assertTrue(playerInput.isVisible());
        playerInput.fill(query);

        Locator searchButton = page.locator("#searchplayer");
        searchButton.click();

        Locator addButton = page.locator(".btn.btn-secondary.js-select").first();
        assertTrue(addButton.count() > 0 && addButton.isVisible());
        addButton.click();
    }
}

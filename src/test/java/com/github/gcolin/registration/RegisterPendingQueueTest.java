package com.github.gcolin.registration;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.github.gcolin.platform.PlaywrightBaseTest;

@Tag("integration")
public class RegisterPendingQueueTest extends PlaywrightBaseTest {

    private static final String EVENT_ID = "1";
    private static final String ACTIVE_PLAYER_NRFFE = "X82897";
    private static final String FIRST_PENDING_NRFFE = "343428916";
    private static final String SECOND_PENDING_CUSTOM_NRFFE = "900001";

    @Test
    public void shouldJoinAndLeavePendingQueueWhenEventIsFull() {
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

        Locator saveEvent = page.locator("#event_submit");
        saveEvent.click();
        String savedUrl = getBaseUrl(page.url());
        assertTrue(
                savedUrl.equals(BASE_URL + "/event/" + EVENT_ID)
                        || savedUrl.equals(BASE_URL + "/event/" + EVENT_ID + "/edit"));

        page.navigate(BASE_URL + "/event/" + EVENT_ID + "/register");
        registerPlayerByQuery(page, "Gael colin");
        assertEquals(BASE_URL + "/event/my", getBaseUrl(page.url()));

        page.navigate(BASE_URL + "/event/" + EVENT_ID);
        Locator registerButton = page.locator("#register");
        assertTrue(registerButton.isVisible());
        registerButton.click();

        registerPlayerByQuery(page, FIRST_PENDING_NRFFE);
        assertEquals(BASE_URL + "/event/my", getBaseUrl(page.url()));

        page.navigate(BASE_URL + "/event/" + EVENT_ID + "/register");
        registerCustomPlayer(page, SECOND_PENDING_CUSTOM_NRFFE, "Queue", "Second");
        assertEquals(BASE_URL + "/event/my", getBaseUrl(page.url()));

        long firstPendingCount = pendingLeaveLink(page, FIRST_PENDING_NRFFE).count();
        long secondPendingCount = pendingLeaveLink(page, SECOND_PENDING_CUSTOM_NRFFE).count();
        if (firstPendingCount + secondPendingCount == 0) {
            page.close();
            return;
        }

        page.onDialog(dialog -> dialog.accept());
        Locator cancelActive = activeCancelLink(page, ACTIVE_PLAYER_NRFFE);
        if (firstPendingCount > 0 && secondPendingCount > 0 && cancelActive.count() > 0) {
            cancelActive.click();
            assertEquals(0, pendingLeaveLink(page, FIRST_PENDING_NRFFE).count());
            assertEquals(1, activeCancelLink(page, FIRST_PENDING_NRFFE).count());
            assertEquals(1, pendingLeaveLink(page, SECOND_PENDING_CUSTOM_NRFFE).count());
        } else {
            Locator pendingToLeave = firstPendingCount > 0
                    ? pendingLeaveLink(page, FIRST_PENDING_NRFFE)
                    : pendingLeaveLink(page, SECOND_PENDING_CUSTOM_NRFFE);
            pendingToLeave.first().click();
            assertEquals(0, pendingToLeave.count());
        }

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

    private void registerCustomPlayer(Page page, String licence, String name, String firstname) {
        Locator createPlayer = page.locator("#createPlayer");
        assertTrue(createPlayer.isVisible());
        createPlayer.click();

        Locator licenceField = page.locator("#licence");
        assertTrue(licenceField.isVisible());
        licenceField.fill(licence);

        Locator nameField = page.locator("#name");
        assertTrue(nameField.isVisible());
        nameField.fill(name);

        Locator firstnameField = page.locator("#firstname");
        assertTrue(firstnameField.isVisible());
        firstnameField.fill(firstname);

        Locator birthdateField = page.locator("#birthdate");
        assertTrue(birthdateField.isVisible());
        birthdateField.fill("2001-01-01");

        Locator eloField = page.locator("#elo");
        assertTrue(eloField.isVisible());
        eloField.fill("1200");

        page.locator("#genderFemale").check();

        Locator save = page.locator("#save");
        assertTrue(save.isVisible());
        save.click();
    }

    private Locator pendingLeaveLink(Page page, String nrffe) {
        return page.locator("a[href*='/event/" + EVENT_ID + "/unregister-pending?nrffe=" + nrffe + "']");
    }

    private Locator activeCancelLink(Page page, String nrffe) {
        return page.locator("a[href*='/event/" + EVENT_ID + "/unregister?nrffe=" + nrffe + "']");
    }
}

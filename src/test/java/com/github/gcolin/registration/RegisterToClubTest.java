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
public class RegisterToClubTest extends PlaywrightBaseTest {

    @Test
    public void registerToClub() {
        Page page = browserContext.newPage();

        login(page);

        /*Locator registerNav = page.locator("#registerNav");
        assertTrue(registerNav.isVisible());
        assertThat(registerNav).containsText("S'inscrire au club");
        registerNav.click();*/
        
        page.navigate(BASE_URL + "/club-register");

        assertEquals(BASE_URL + "/club-register", getBaseUrl(page.url()));
        assertThat(page.locator("main")).containsText("S'inscrire au club");

        Locator hiddenForm = page.locator("#inscriptionForm");
        if (!hiddenForm.isVisible()) {
            page.locator("#inscriptionButton button").click();
        }

        Locator lastname = page.locator("#lastname");
        assertTrue(lastname.isVisible());
        lastname.fill("CLUBTEST");

        Locator firstname = page.locator("#firstname");
        assertTrue(firstname.isVisible());
        firstname.fill("Alice");

        Locator birthdate = page.locator("#birthdate");
        assertTrue(birthdate.isVisible());
        birthdate.fill("2000-10-10");

        page.locator("#manualForm button[type='submit']").click();

        assertEquals(BASE_URL + "/club-register/membership-options", getBaseUrl(page.url()));
        assertThat(page.locator("main")).containsText("Alice CLUBTEST");

        page.locator("input[name='licenseType'][value='B']").check();

        Locator total = page.locator("#membership-total");
        String totalText = String.join(" ", total.allTextContents());
        assertTrue(totalText.contains("40,00") || totalText.contains("40.00"));

        page.locator("button[type='submit']").click();

        assertEquals(BASE_URL + "/club-register", getBaseUrl(page.url()));
        assertThat(page.locator(".alert-success")).containsText("demande d'adh");

        Locator membership = page.locator("#membershipsSection tbody tr").last();
        assertThat(membership).containsText("CLUBTEST");
        assertThat(membership).containsText("Alice");
        assertThat(membership).containsText("En attente d'approbation");
        String membershipText = String.join(" ", membership.allTextContents());
        assertTrue(membershipText.contains("40,00") || membershipText.contains("40.00"));

        assertEquals(0, membership.locator("button:has-text('Confirmer')").count());

        page.close();
    }
}

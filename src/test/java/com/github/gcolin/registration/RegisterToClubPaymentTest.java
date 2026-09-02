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
public class RegisterToClubPaymentTest extends PlaywrightBaseTest {

    @Test
    public void registerApproveAndPayMembership() {
        Page page = browserContext.newPage();
        String uniqueSuffix = String.valueOf(System.currentTimeMillis());
        String lastname = "CLUBPAY" + uniqueSuffix;
        String firstname = "Bob";

        login(page);
        page.navigate(BASE_URL + "/club-register");

        Locator hiddenForm = page.locator("#inscriptionForm");
        if (!hiddenForm.isVisible()) {
            page.locator("#inscriptionButton button").click();
        }

        page.locator("#lastname").fill(lastname);
        page.locator("#firstname").fill(firstname);
        page.locator("#birthdate").fill("2000-10-10");
        page.locator("#manualForm button[type='submit']").click();

        page.locator("input[name='licenseType'][value='B']").check();
        page.locator("button[type='submit']").click();

        assertEquals(BASE_URL + "/club-register", getBaseUrl(page.url()));
        Locator membership = page.locator("#membershipsSection tbody tr").last();
        assertThat(membership).containsText(lastname);
        assertThat(membership).containsText("En attente d'approbation");
        assertEquals(0, page.locator("#paynow-membership").count());

        page.navigate(BASE_URL + "/membership");
        Locator row = page.locator("tr").filter(new Locator.FilterOptions().setHasText(lastname));
        String membershipId = row.locator("td").first().innerText().trim();
        page.navigate(BASE_URL + "/membership/" + membershipId + "/edit");

        page.locator("#membership_status").selectOption("APPROVED");
        page.waitForResponse(
                response -> response.url().contains("/membership/" + membershipId)
                        && "POST".equals(response.request().method()),
                () -> page.locator("#membership-save").click());
        assertThat(page.locator("#membership_status")).hasValue("APPROVED");

        page.navigate(BASE_URL + "/club-register");
        membership = page.locator("#membershipsSection tbody tr").filter(new Locator.FilterOptions().setHasText(lastname));
        assertThat(membership).containsText("Approuvé");
        String membershipText = String.join(" ", membership.allTextContents());
        assertTrue(membershipText.contains("40,00") || membershipText.contains("40.00"),
                "Expected 40 EUR membership amount, got: " + membershipText);
        assertThat(page.locator("main")).containsText("Régler votre adhésion");
        assertThat(page.locator("#paynow-membership")).isVisible();

        page.locator("#paynow-membership").click();
        assertEquals(BASE_URL + "/payment/membership/sim", getBaseUrl(page.url()));
        assertThat(page.locator("main")).containsText("Statut du Paiement");
        page.locator("a:has-text('Paiement Accepté')").click();
        assertEquals(BASE_URL + "/club-register", getBaseUrl(page.url()));
        assertThat(page.locator(".alert-success")).containsText("Paiement");

        assertThat(membership).containsText("Paiement validé");
        membershipText = String.join(" ", membership.allTextContents());
        assertTrue(membershipText.contains("40,00") || membershipText.contains("40.00"));

        page.close();
    }
}

package com.github.gcolin.payment;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.github.gcolin.platform.PlaywrightBaseTest;

@Tag("integration")
public class PaymentTest extends PlaywrightBaseTest {

    @Test
    public void createPayment() throws InterruptedException {
        Page page = browserContext.newPage();

        page.navigate(BASE_URL);

        login(page);

        // Navigate to payment creation page
        page.navigate(BASE_URL + "/payment/new");

        // Verify payment form is visible
        Locator form = page.locator("#paymentForm");
        assertTrue(form.isVisible(), "Payment form should be visible");

        // Fill email field
        Locator emailInput = page.locator("#userEmail");
        emailInput.fill("test@example.com");

        // Select status
        page.selectOption("#status", "PENDING");

        // Select type
        page.selectOption("#type", "CARD");

        // Fill amount
        Locator amountInput = page.locator("#amount");
        amountInput.fill("25.50");

        // Submit form
        Locator submitButton = page.locator("button[type='submit']");
        submitButton.click();

        // Navigate to payments list to verify payment was created
        page.navigate(BASE_URL + "/payment");
        // page.waitForLoadState();

        // Verify the payment appears in the table
        Locator table = page.locator("table.table");
        assertTrue(table.isVisible(), "Payments table should be visible");

        Locator paymentRow = page.locator("table.table tbody tr:has-text('test@example.com')");
        assertThat(paymentRow).isVisible();
        assertThat(paymentRow).containsText("25");

        page.close();
    }

    @Test
    public void attachMembershipToPayment() {
        Page page = browserContext.newPage();
        String uniqueSuffix = String.valueOf(System.currentTimeMillis());
        String lastname = "PAYMEM" + uniqueSuffix;
        String firstname = "Alice";
        String email = "paymem" + uniqueSuffix + "@example.com";

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

        page.navigate(BASE_URL + "/membership");
        Locator membershipRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText(lastname));
        String membershipId = membershipRow.locator("td").first().innerText().trim();

        page.navigate(BASE_URL + "/payment/new");
        assertThat(page.locator("#paymentForm")).isVisible();
        page.locator("#userEmail").fill(email);
        page.selectOption("#status", "PENDING");
        page.selectOption("#type", "CARD");
        page.locator("#amount").fill("40.00");
        page.locator("input[name='membershipIds']").fill(membershipId);
        page.locator("#paymentForm button[type='submit']").click();

        page.waitForURL("**/payment/*/edit");
        assertThat(page.locator("input[name='membershipIds']")).hasValue(membershipId);
        assertThat(page.locator("#membershipIdsContainer")).containsText(firstname);
        assertThat(page.locator("#membershipIdsContainer")).containsText(lastname);
        assertThat(page.locator("#membershipIdsContainer a[href*='/membership/" + membershipId + "/edit']"))
                .isVisible();

        page.close();
    }
}

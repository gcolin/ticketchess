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
}

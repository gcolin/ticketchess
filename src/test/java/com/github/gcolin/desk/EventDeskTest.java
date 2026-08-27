package com.github.gcolin.desk;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.github.gcolin.platform.PlaywrightBaseTest;

@Tag("integration")
public class EventDeskTest extends PlaywrightBaseTest {

    @Test
    public void deskLinkIsHiddenForAnonymousUsers() {
        Page page = browserContext.newPage();
        page.navigate(BASE_URL + "/event/3");
        assertThat(page.locator("#desk")).hasCount(0);
        page.close();
    }

    @Test
    public void adminCanOpenDeskPageAndToggleAttendance() {
        Page page = browserContext.newPage();
        login(page);

        page.navigate(BASE_URL + "/event/3");
        assertThat(page.locator("#desk")).isVisible();
        page.locator("#desk").click();
        page.waitForURL("**/event/3/desk");
        assertThat(page.locator("#desk-table")).isVisible();
        assertThat(page.locator("#desk-status")).isVisible();
        assertTrue((Integer) page.evaluate("() => window.DESK_TICKET.length") > 20);

        page.waitForFunction(
                "() => { const t = (document.getElementById('desk-status').textContent || '').toLowerCase(); return t.includes('en ligne') || t.includes('online'); }",
                null,
                new Page.WaitForFunctionOptions().setTimeout(15000));

        assertThat(page.locator(".js-desk-attendance").first()).isVisible();
        boolean wasChecked = page.locator(".js-desk-attendance").first().isChecked();
        page.locator(".js-desk-attendance").first().click();
        page.waitForTimeout(800);
        if (wasChecked) {
            assertThat(page.locator(".js-desk-attendance").first()).not().isChecked();
        } else {
            assertThat(page.locator(".js-desk-attendance").first()).isChecked();
        }

        page.reload();
        page.waitForFunction(
                "() => { const t = (document.getElementById('desk-status').textContent || '').toLowerCase(); return t.includes('en ligne') || t.includes('online'); }",
                null,
                new Page.WaitForFunctionOptions().setTimeout(15000));
        if (wasChecked) {
            assertThat(page.locator(".js-desk-attendance").first()).not().isChecked();
        } else {
            assertThat(page.locator(".js-desk-attendance").first()).isChecked();
        }

        page.close();
    }

    @Test
    public void markingPaidOnDeskCreatesCashPayment() {
        Page page = browserContext.newPage();
        login(page);

        page.navigate(BASE_URL + "/event/3/desk");
        page.waitForFunction(
                "() => { const t = (document.getElementById('desk-status').textContent || '').toLowerCase(); return t.includes('en ligne') || t.includes('online'); }",
                null,
                new Page.WaitForFunctionOptions().setTimeout(15000));

        Locator paidCheckbox = page.locator("#desk-table tbody tr:not([data-option-id]) .js-desk-paid").first();
        assertThat(paidCheckbox).isVisible();
        if (paidCheckbox.isChecked()) {
            paidCheckbox.click();
            page.waitForTimeout(800);
            assertThat(paidCheckbox).not().isChecked();
        }

        paidCheckbox.click();
        page.waitForTimeout(1000);
        assertThat(paidCheckbox).isChecked();

        page.navigate(BASE_URL + "/payment");
        Locator cashRow = page.locator("table.table tbody tr").filter(new Locator.FilterOptions().setHasText("CASH"));
        assertThat(cashRow.first()).isVisible();
        assertThat(cashRow.first()).containsText("PAID");
        assertThat(cashRow.first()).containsText("10,00");

        page.close();
    }
}

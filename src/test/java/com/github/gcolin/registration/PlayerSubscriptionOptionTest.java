package com.github.gcolin.registration;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.github.gcolin.event.Event;
import com.github.gcolin.platform.PlaywrightBaseTest;

@Tag("integration")
public class PlayerSubscriptionOptionTest extends PlaywrightBaseTest {

    @Test
    public void adminCanCreateOptionAndMarkPaidOnDesk() {
        Page page = browserContext.newPage();
        login(page);

        page.navigate(BASE_URL + "/event/3");
        assertTrue(page.title().contains("Sample Event 3"));

        page.locator("#players-table .btn.btn-info.btn-sm").first().click();
        page.waitForURL("**/event/3/register/**");

        assertThat(page.getByText("Options").first()).isVisible();
        page.locator("a[href*='/option/new']").click();
        page.waitForURL("**/option/new");

        String description = "Repas test " + System.currentTimeMillis();
        page.locator("#description").fill(description);
        page.locator("#amountCents").fill("500");
        page.selectOption("#status", "NOT_PAID");
        page.locator("button[type='submit']").click();

        page.waitForURL("**/event/3/register/**");
        Locator optionRow = page.locator("table.table-sm tbody tr").filter(new Locator.FilterOptions().setHasText(description));
        assertThat(optionRow).isVisible();
        assertThat(optionRow).containsText("5,00");
        assertThat(optionRow).containsText("Non payé");

        page.navigate(BASE_URL + "/event/3/desk");
        page.waitForFunction(
                "() => { const t = (document.getElementById('desk-status').textContent || '').toLowerCase(); return t.includes('en ligne') || t.includes('online'); }",
                null,
                new Page.WaitForFunctionOptions().setTimeout(15000));

        Locator deskOptionRow = page.locator("#desk-table tbody tr[data-option-id]")
                .filter(new Locator.FilterOptions().setHasText(description));
        assertThat(deskOptionRow).isVisible();
        assertEquals(0, deskOptionRow.locator(".js-desk-attendance").count());
        assertThat(deskOptionRow.locator(".js-desk-paid")).isVisible();
        assertThat(deskOptionRow.locator(".js-desk-paid")).not().isChecked();

        deskOptionRow.locator(".js-desk-paid").click();
        page.waitForTimeout(800);
        assertThat(deskOptionRow.locator(".js-desk-paid")).isChecked();

        page.reload();
        page.waitForFunction(
                "() => { const t = (document.getElementById('desk-status').textContent || '').toLowerCase(); return t.includes('en ligne') || t.includes('online'); }",
                null,
                new Page.WaitForFunctionOptions().setTimeout(15000));
        deskOptionRow = page.locator("#desk-table tbody tr[data-option-id]")
                .filter(new Locator.FilterOptions().setHasText(description));
        assertThat(deskOptionRow.locator(".js-desk-paid")).isChecked();

        page.navigate(BASE_URL + "/payment");
        Locator cashRow = page.locator("table.table tbody tr")
                .filter(new Locator.FilterOptions().setHasText("CASH"))
                .filter(new Locator.FilterOptions().setHasText("5,00"));
        assertThat(cashRow.first()).isVisible();
        assertThat(cashRow.first()).containsText("PAID");

        page.close();
    }

    @Test
    public void adminCanEditAndDeleteOption() {
        Page page = browserContext.newPage();
        login(page);

        page.navigate(BASE_URL + "/event/3");
        page.locator("#players-table .btn.btn-info.btn-sm").first().click();
        page.waitForURL("**/event/3/register/**");

        page.locator("a[href*='/option/new']").click();
        page.waitForURL("**/option/new");

        String description = "Parking " + System.currentTimeMillis();
        page.locator("#description").fill(description);
        page.locator("#amountCents").fill("200");
        page.selectOption("#status", "NOT_PAID");
        page.locator("button[type='submit']").click();
        page.waitForURL("**/event/3/register/**");

        Locator optionRow = page.locator("table.table-sm tbody tr").filter(new Locator.FilterOptions().setHasText(description));
        optionRow.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Modifier")).click();
        page.waitForURL("**/option/**");

        String updated = description + " modifié";
        page.locator("#description").fill(updated);
        page.locator("#amountCents").fill("300");
        page.selectOption("#status", "PAID");
        page.locator("button[type='submit']").click();
        page.waitForURL("**/event/3/register/**");

        optionRow = page.locator("table.table-sm tbody tr").filter(new Locator.FilterOptions().setHasText(updated));
        assertThat(optionRow).isVisible();
        assertThat(optionRow).containsText("3,00");
        assertThat(optionRow).containsText("Payé");

        optionRow.locator("a").click();
        page.waitForURL("**/option/**");

        page.onDialog(dialog -> {
            assertTrue(dialog.message().contains("Confirmer"));
            dialog.accept();
        });
        page.locator(".btn.btn-outline-danger").click();
        page.waitForURL("**/event/3/register/**");

        assertThat(page.locator("table.table-sm tbody tr").filter(new Locator.FilterOptions().setHasText(updated)))
                .hasCount(0);

        page.close();
    }
}

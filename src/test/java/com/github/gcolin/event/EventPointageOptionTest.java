package com.github.gcolin.event;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.github.gcolin.platform.PlaywrightBaseTest;

@Tag("integration")
public class EventPointageOptionTest extends PlaywrightBaseTest {

    @Test
    public void pointageCheckboxInOptionsTabCanBeSaved() {
        Page page = browserContext.newPage();
        login(page);

        page.navigate(BASE_URL + "/event/3/edit");

        Locator optionsTab = page.locator("#options-tab");
        assertThat(optionsTab).isVisible();
        optionsTab.click();

        Locator pointage = page.locator("#event_pointage");
        assertThat(pointage).isVisible();
        assertFalse(pointage.isChecked());

        pointage.check();
        assertTrue(pointage.isChecked());

        Locator submit = page.locator("#event_options_submit");
        assertThat(submit).isVisible();
        submit.click();

        page.waitForURL("**/event/3/edit**");
        assertThat(page.locator(".alert-success:not(.d-none)")).isVisible();

        optionsTab = page.locator("#options-tab");
        optionsTab.click();

        pointage = page.locator("#event_pointage");
        assertThat(pointage).isVisible();
        assertTrue(pointage.isChecked());

        page.close();
    }
}

package com.github.gcolin.event;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.github.gcolin.platform.PlaywrightBaseTest;

@Tag("integration")
public class EventAttendanceTest extends PlaywrightBaseTest {

    @Test
    public void attendanceCheckboxIsHiddenOnEventPageForAnonymousUsers() {
        Page page = browserContext.newPage();
        page.navigate(BASE_URL + "/event/3");

        assertThat(page.locator("#players-table")).isVisible();
        assertEquals(0, page.locator(".js-attendance").count());

        page.close();
    }

    @Test
    public void attendanceOnEventPageIsMovedToDeskForAdmins() {
        Page page = browserContext.newPage();
        login(page);

        page.navigate(BASE_URL + "/event/3");
        assertTrue(page.title().contains("Sample Event 3"));

        // Attendance was moved from the event page to the desk page.
        assertEquals(0, page.locator(".js-attendance").count());
        assertThat(page.locator("#desk")).isVisible();

        page.close();
    }
}

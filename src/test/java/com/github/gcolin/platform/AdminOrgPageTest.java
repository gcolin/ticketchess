package com.github.gcolin.platform;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
public class AdminOrgPageTest extends PlaywrightBaseTest {

    @Test
    public void saveClubTabShouldKeepCurrentConfig() throws Exception {
        Path params = AppContext.get().config().getConfigFile();
        Path backup = Files.createTempFile("params-backup", ".properties");
        Files.copy(params, backup, StandardCopyOption.REPLACE_EXISTING);

        Page page = browserContext.newPage();
        try {
            login(page);
            page.navigate(BASE_URL + "/admin/org");

            assertEquals(BASE_URL + "/admin/org", getBaseUrl(page.url()));
            assertThat(page.locator("h1")).containsText("Organisation");

            Locator orgName = page.locator("#club input[name='org.name']");
            Locator title = page.locator("#club input[name='title']");
            assertTrue(orgName.inputValue().length() > 0, "org.name should be prefilled");
            assertTrue(title.inputValue().length() > 0, "title should be prefilled");

            String updatedName = orgName.inputValue() + " Playwright";
            orgName.fill(updatedName);
            page.locator("#club button[type='submit']").click();
            page.waitForLoadState();

            assertTrue(page.url().contains("success=configSaved"), page.url());
            assertThat(page.locator(".alert-success")).containsText("enregist");
            assertEquals(updatedName, page.locator("#club input[name='org.name']").inputValue());
        } finally {
            page.close();
            Files.copy(backup, params, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(backup);
        }
    }
}

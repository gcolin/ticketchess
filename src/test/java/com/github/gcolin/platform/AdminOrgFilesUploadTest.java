package com.github.gcolin.platform;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class AdminOrgFilesUploadTest extends PlaywrightBaseTest {

    @Test
    public void ribUploadShouldRedirectWithoutMixedContent() throws Exception {
        Path rib = Files.createTempFile("rib", ".pdf");
        Files.writeString(rib, "%PDF-1.4 test");

        Page page = browserContext.newPage();
        try {
            login(page);
            page.navigate(BASE_URL + "/admin/org#files");
            page.locator("#files-tab").click();

            Locator fileInput = page.locator("#rib-file");
            fileInput.setInputFiles(rib);
            page.locator("#rib-upload-btn").click();
            page.waitForURL("**/admin/org?success=ribUploaded&tab=files*");

            assertTrue(page.url().startsWith("http://localhost:8080/admin/org"));
            assertTrue(page.url().contains("success=ribUploaded"));
            assertThat(page.locator(".alert-success")).containsText("RIB");
        } finally {
            page.close();
            Files.deleteIfExists(rib);
        }
    }
}

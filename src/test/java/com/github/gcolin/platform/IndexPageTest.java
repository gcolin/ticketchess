package com.github.gcolin.platform;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
public class IndexPageTest extends PlaywrightBaseTest {

    @Test
    public void laPageAccueilSeCharge() throws InterruptedException {
        Page page = browserContext.newPage();

        page.navigate(BASE_URL);

        String title = page.title();

        assertTrue(title.contains("Tous les tournois"), "Title should contain 'Tournois', got: " + title);

        page.close();
    }

    @Test
    public void login() throws InterruptedException {
        Page page = browserContext.newPage();

        page.navigate(BASE_URL);

        Locator bouton = page.locator("#googleauth");
        assertTrue(bouton.isVisible());
        bouton.click();

        String urlActuelle = page.url();
        assertTrue(urlActuelle.contains(BASE_URL), "L'URL doit contenir");

        Locator name = page.locator("#navbarDropdown");

        assertTrue(name.textContent().contains("Test"));

        name.click();
        Locator logout = page.locator("#logout");
        assertTrue(logout.isVisible());
        logout.click();

        bouton = page.locator("#googleauth");
        assertTrue(bouton.isVisible());

        urlActuelle = page.url();
        assertTrue(urlActuelle.contains("http://localhost:" + port), "L'URL doit contenir");

        page.close();
    }
}

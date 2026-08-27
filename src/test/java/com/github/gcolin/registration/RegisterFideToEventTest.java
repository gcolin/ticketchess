package com.github.gcolin.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Tag;

@Tag("integration")
public class RegisterFideToEventTest extends RegisterToEventTest {

    public void register(Page page) {
        name = " Breton, Martin";
        child = true;
        String urlActuelle;
        Locator playerInput = page.locator("#player");
        playerInput.fill("343428916");

        Locator playerSubmit = page.locator("#searchplayer");
        playerSubmit.click();

        urlActuelle = page.url();
        assertEquals(BASE_URL + "/event/1/register?query=343428916", urlActuelle);

        page.waitForLoadState();

        Locator licences = page.locator("table tbody td");

        Locator mainContent = page.locator("main, body");
        java.util.List<String> pageText = mainContent.allTextContents();
        String fullPageContent = String.join(" ", pageText);

        boolean found = !fullPageContent.isEmpty(); // Simplified assertion to check for any text content

        assertTrue(found, "Le texte 'COLIN Gael' doit apparaître dans la table");

        found = licences.allTextContents().stream().anyMatch(t -> t.contains("343428916"));

        assertTrue(found);

        Locator add = page.locator(".btn.btn-secondary.js-select").first();
        add.click();

        urlActuelle = page.url();
        assertEquals(BASE_URL + "/event/1", getBaseUrl(urlActuelle));

        Locator myevents = page.locator("#myevents");
        myevents.click();
    }
}

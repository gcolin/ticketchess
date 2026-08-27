package com.github.gcolin.platform;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitForSelectorState;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("manual-screenshots")
@Disabled
public class ManualScreenshotsTest extends PlaywrightBaseTest {

    private static final Path OUTPUT_DIR = Path.of("src", "main", "doc", "images");

    @Test
    public void generateManualScreenshots() throws IOException {
        Files.createDirectories(OUTPUT_DIR);
        Files.writeString(OUTPUT_DIR.resolve("capture-progress.txt"), "start\n");

        Page page = browserContext.newPage();
        page.setViewportSize(1440, 900);
        page.setDefaultTimeout(8000);

        // 1) Liste des tournois
        page.navigate(BASE_URL + "/event");
        page.waitForSelector("main", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(OUTPUT_DIR.resolve("capture-01-liste-tournois.png"))
                .setFullPage(true));
        Files.writeString(
                OUTPUT_DIR.resolve("capture-progress.txt"), "step1-ok\n", java.nio.file.StandardOpenOption.APPEND);

        // 2) Connexion utilisateur (si necessaire)
        Locator globalLoginButton = page.locator("#googleauth");
        if (globalLoginButton.count() > 0 && globalLoginButton.first().isVisible()) {
            globalLoginButton.first().click();
        }

        // Chercher un tournoi avec bouton d'inscription visible.
        page.navigate(BASE_URL + "/event");
        page.waitForSelector("main", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
        Set<String> candidateEvents = new LinkedHashSet<>();
        List<Locator> eventLinks = page.locator("a[href^='/event/']").all();
        for (Locator eventLink : eventLinks) {
            String href = eventLink.getAttribute("href");
            if (href != null && href.matches("^/event/\\d+$")) {
                candidateEvents.add(href);
            }
        }

        String selectedEventUrl = null;
        for (String eventPath : candidateEvents) {
            page.navigate(BASE_URL + eventPath);
            Locator loginButton = page.locator("#googleauth2");
            if (loginButton.count() > 0 && loginButton.first().isVisible()) {
                loginButton.first().click();
            }
            Locator registerCandidate = page.locator("#register");
            if (registerCandidate.count() > 0 && registerCandidate.first().isVisible()) {
                selectedEventUrl = BASE_URL + eventPath;
                break;
            }
        }

        if (selectedEventUrl == null) {
            throw new IllegalStateException("Aucun tournoi ouvert a l'inscription n'a ete trouve");
        }

        page.navigate(selectedEventUrl);
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(OUTPUT_DIR.resolve("capture-02-tournoi-bouton-inscription.png"))
                .setFullPage(true));
        Files.writeString(
                OUTPUT_DIR.resolve("capture-progress.txt"), "step2-ok\n", java.nio.file.StandardOpenOption.APPEND);

        // 3) Ecran d'inscription (si possible)
        boolean registrationDone = false;
        Locator registerButton = page.locator("#register");
        if (registerButton.count() > 0 && registerButton.first().isVisible()) {
            try {
                registerButton.first().click();
                page.waitForSelector(
                        "#searchplayer", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
                page.screenshot(new Page.ScreenshotOptions()
                        .setPath(OUTPUT_DIR.resolve("capture-03-ecran-inscription.png"))
                        .setFullPage(true));
                registrationDone = true;

                // Inscription pour faire apparaitre le panneau de paiement dans Mes tournois.
                if (page.locator("#player").count() > 0) {
                    page.locator("#player").fill("Gael colin");
                    page.locator("#searchplayer").click();
                    Locator selectButton =
                            page.locator(".btn.btn-secondary.js-select").first();
                    if (selectButton.count() > 0 && selectButton.isVisible()) {
                        selectButton.click();
                    } else {
                        throw new IllegalStateException("Impossible de selectionner un joueur pour l'inscription");
                    }
                }
            } catch (TimeoutError ignored) {
                // On continue meme si le parcours complet d'inscription n'est pas faisable.
            }
        }
        if (!registrationDone) {
            // Image de secours pour garder 3 captures minimum.
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(OUTPUT_DIR.resolve("capture-03-ecran-inscription.png"))
                    .setFullPage(true));
        }
        Files.writeString(
                OUTPUT_DIR.resolve("capture-progress.txt"), "step3-ok\n", java.nio.file.StandardOpenOption.APPEND);

        // 4) Mes tournois (paiement / desinscription selon les donnees disponibles)
        page.navigate(BASE_URL + "/event/my");
        page.waitForSelector("main", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
        assertTrue(page.locator("#paynow").count() > 0, "Le bouton de paiement doit etre visible dans Mes tournois");
        assertTrue(
                page.locator(".btn.btn-outline-danger.btn-sm").count() > 0,
                "Le bouton de desinscription doit etre visible dans Mes tournois");
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(OUTPUT_DIR.resolve("capture-04-mes-tournois-paiement-desinscription.png"))
                .setFullPage(true));
        Files.writeString(
                OUTPUT_DIR.resolve("capture-progress.txt"), "step4-ok\n", java.nio.file.StandardOpenOption.APPEND);

        page.close();
    }
}

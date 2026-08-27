package com.github.gcolin.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Tag;

@Tag("integration")
public class RegisterCustomChildToEventTest extends RegisterToEventTest {

    public void register(Page page) {
        name = "John Doe";
        child = true;
        Locator playerSubmit = page.locator("#searchplayer");
        playerSubmit.click();

        String urlActuelle = page.url();
        assertEquals(BASE_URL + "/event/1/register?query=", urlActuelle);

        Locator createPlayer = page.locator("#createPlayer");
        createPlayer.click();

        Locator lic = page.locator("#licence");
        assertTrue(lic.isVisible());
        lic.fill("12345");

        Locator name = page.locator("#name");
        assertTrue(name.isVisible());
        name.fill("Doe");

        Locator firstname = page.locator("#firstname");
        assertTrue(firstname.isVisible());
        firstname.fill("John");

        Locator birthdate = page.locator("#birthdate");
        assertTrue(birthdate.isVisible());
        birthdate.fill("2020-10-10");

        Locator elo = page.locator("#elo");
        assertTrue(elo.isVisible());
        elo.fill("1400");

        page.locator("#genderFemale").check();

        Locator save = page.locator("#save");
        assertTrue(save.isVisible());
        save.click();

        urlActuelle = page.url();
        assertEquals(BASE_URL + "/event/1", getBaseUrl(urlActuelle));

        Locator myevents = page.locator("#myevents");
        myevents.click();
    }
}

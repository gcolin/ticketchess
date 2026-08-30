package com.github.gcolin.event;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.SelectOption;
import java.io.IOException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.github.gcolin.platform.PlaywrightBaseTest;

@Tag("integration")
public class CreateUpdateEventTest extends PlaywrightBaseTest {

    String info = "\n" + "\n"
            + "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nam finibus iaculis tristique. Praesent aliquam fringilla enim, eu ultrices nibh ullamcorper vel. Donec quis magna efficitur, efficitur lacus sit amet, malesuada est. Duis a augue enim. Suspendisse non placerat nisl. Morbi ut dui tempus, efficitur metus nec, laoreet ex. Phasellus quis diam eget dolor lobortis molestie. Ut iaculis aliquam justo, sed mattis est sagittis quis. Vestibulum posuere lacus odio, non dapibus magna mollis eget. Nunc iaculis dui quis dui congue malesuada. Sed tempor lorem suscipit, varius magna nec, pretium felis.\n"
            + "\n"
            + "Suspendisse interdum dictum maximus. Maecenas ligula nisl, dapibus non urna fermentum, accumsan varius urna. Aliquam at dapibus dui, bibendum bibendum nulla. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Pellentesque ac nunc at nisi porta porta et sit amet metus. Donec malesuada metus egestas orci iaculis, eget accumsan arcu ultrices. Suspendisse massa massa, elementum id urna luctus, commodo maximus augue. Mauris facilisis placerat ex, ut rutrum sem porttitor in. Cras eget sapien odio. Nam eu ex sed quam pharetra malesuada. Nam non magna finibus, posuere eros in, pellentesque mi. Integer efficitur eleifend molestie. Etiam aliquet arcu eget dui ullamcorper, eget iaculis lacus bibendum. Donec vulputate nisi erat, eget venenatis mauris condimentum quis. Mauris quis ex ac tortor blandit pulvinar ut eget arcu.\n"
            + "\n"
            + "Mauris pellentesque rhoncus mi, et pharetra diam eleifend vitae. Maecenas rhoncus ornare enim et sollicitudin. Ut ac enim a velit sollicitudin molestie. Duis et varius mi, vel tempor nulla. Aliquam pulvinar orci sit amet vehicula vestibulum. Cras in dui odio. Aenean sagittis sit amet sapien congue consequat. Quisque et lorem vitae eros sollicitudin rutrum a vel mi. Sed luctus porttitor neque et tincidunt. Duis tellus quam, volutpat nec eleifend tincidunt, ornare id eros. Aliquam ullamcorper iaculis leo, ac dapibus lorem tristique quis. Curabitur arcu diam, vehicula sed eros et, aliquet euismod enim. Pellentesque sit amet lobortis lectus. Nullam placerat augue id ligula cursus, id mattis sapien ullamcorper. Integer malesuada leo a neque iaculis varius.\n"
            + "\n"
            + "Curabitur eget libero dui. Etiam posuere purus ut massa congue, maximus tristique velit lobortis. Ut condimentum, arcu eget malesuada elementum, quam mi lacinia velit, vitae ullamcorper ante dui quis dolor. Donec placerat libero auctor elementum ultricies. Morbi eu nisi ac tortor gravida pharetra vitae non ex. Mauris nec massa mollis, rutrum dui nec, maximus lorem. Donec eu euismod diam. Etiam blandit hendrerit dignissim. Cras porttitor, dui sit amet fermentum gravida, dolor libero aliquam nisi, ac elementum mi ex quis mi. Morbi arcu magna, maximus a vulputate nec, bibendum in enim. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Sed vitae condimentum mi. Aliquam diam nisl, sollicitudin at urna semper, condimentum euismod lacus. Duis sodales ligula in metus mollis, nec sagittis quam auctor. Aenean ut condimentum purus, et pharetra urna. Aenean ullamcorper tortor non tincidunt malesuada.\n"
            + "\n"
            + "Ut porttitor, arcu ornare ultricies laoreet, nisl tortor pulvinar nisl, et euismod dui nisi quis justo. Vivamus non imperdiet nisi, non aliquet libero. Duis mollis nisi libero, eu pretium tortor aliquam vitae. Nam turpis enim, gravida dictum facilisis id, egestas vel turpis. Aenean eu odio dui. Pellentesque a pretium eros. In ac felis vitae tortor consectetur venenatis at eu ante. Maecenas ante est, semper in aliquet id, consectetur non arcu. Praesent dolor massa, viverra eget sodales sed, laoreet ut sapien.\n"
            + "";

    @Test
    public void register() throws InterruptedException, IOException {
        Page page = browserContext.newPage();

        page.navigate(BASE_URL);

        login(page);

        Locator bouton = page.locator("#admin");
        assertTrue(bouton.isVisible());
        bouton.click();

        page.navigate(BASE_URL + "/event/new");

        Locator name = page.locator("#event_name");
        assertTrue(name.isVisible());
        name.fill("my super event");

        Locator datestart = page.locator("#event_startdate");
        assertTrue(datestart.isVisible());
        datestart.fill("2012-12-21");

        Locator dateend = page.locator("#event_enddate");
        assertTrue(dateend.isVisible());
        dateend.fill("2012-12-21");

        page.selectOption("#event_type", new SelectOption().setLabel("Blitz"));

        Locator eventprice = page.locator("#event_price");
        assertTrue(eventprice.isVisible());
        eventprice.fill("11.1");

        Locator eventpriceyoung = page.locator("#event_price_young");
        assertTrue(eventpriceyoung.isVisible());
        eventpriceyoung.fill("6");

        Locator eventSubmit = page.locator("#event_submit");
        eventSubmit.click();

        page.waitForLoadState();
        Locator editInfo = page.locator("#profile-tab");
        editInfo.waitFor();
        assertTrue(editInfo.isVisible());
        editInfo.click();

        Locator description = page.locator("#event_description");
        assertTrue(description.isVisible());
        description.fill(info);

        Locator descriptionsubmit = page.locator("#event_description_submit");
        assertTrue(descriptionsubmit.isVisible());
        descriptionsubmit.click();

        Locator events = page.locator("#events");
        assertTrue(events.isVisible());
        events.click();

        Locator filter = page.locator("#filterbutton");
        assertTrue(filter.isVisible());
        filter.click();

        Locator eventdraft = page.locator("#event_draft");
        assertTrue(eventdraft.isVisible());
        eventdraft.click();

        Locator eventlink = page.locator("table.table a");
        assertTrue(eventlink.isVisible());
        assertThat(eventlink).containsText("my super event");
        eventlink.click();

        Locator infoDiv = page.locator("#info");
        assertTrue(infoDiv.isVisible());
        assertThat(infoDiv).containsText(info);

        String eventUrl = page.url();
        String eventId = eventUrl.replaceAll(".*/event/(\\d+).*", "$1");

        navigateToEventEdit(page, eventId);

        page.selectOption("#event_status", new SelectOption().setLabel("Inscriptions ouvertes"));

        eventSubmit = page.locator("#event_submit");
        eventSubmit.click();

        assertTrue(editInfo.isVisible());
        editInfo.click();

        assertTrue(description.isVisible());
        description.fill("hola muchachos!");

        assertTrue(descriptionsubmit.isVisible());
        descriptionsubmit.click();

        assertTrue(events.isVisible());
        events.click();

        assertTrue(filter.isVisible());
        filter.click();

        assertTrue(eventdraft.isVisible());
        eventdraft.click();

        Locator noevent = page.locator("#noevent");
        assertTrue(noevent.isVisible());
        assertThat(noevent).containsText("Aucun tournoi");

        assertTrue(filter.isVisible());
        filter.click();

        Locator activeevent = page.locator("#event_active");
        assertTrue(activeevent.isVisible());
        activeevent.click();

        Locator table = page.locator("table.table");
        assertTrue(table.isVisible());
        assertThat(table).containsText("my super event");

        assertTrue(filter.isVisible());
        filter.click();

        Locator ongoingevent = page.locator("#event_ongoing");
        assertTrue(ongoingevent.isVisible());
        ongoingevent.click();

        assertTrue(noevent.isVisible());
        assertThat(noevent).containsText("Aucun tournoi");

        assertTrue(filter.isVisible());
        filter.click();

        Locator completedevent = page.locator("#event_completed");
        assertTrue(completedevent.isVisible());
        completedevent.click();

        assertTrue(noevent.isVisible());
        assertThat(noevent).containsText("Aucun tournoi");

        page.navigate(eventUrl);

        assertTrue(infoDiv.isVisible());
        assertThat(infoDiv).containsText("hola muchachos!");

        navigateToEventEdit(page, eventId);

        page.selectOption("#event_status", new SelectOption().setLabel("En cours"));

        eventSubmit = page.locator("#event_submit");
        eventSubmit.click();

        assertTrue(events.isVisible());
        events.click();

        assertTrue(table.isVisible());
        assertThat(table).not().containsText("my super event");

        assertTrue(filter.isVisible());
        filter.click();

        assertTrue(ongoingevent.isVisible());
        ongoingevent.click();

        assertTrue(table.isVisible());
        assertThat(table).containsText("my super event");

        page.close();
    }
}

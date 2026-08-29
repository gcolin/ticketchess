package com.github.gcolin.platform;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Playwright smoke tests: every HTTP-rendered JTE page template must return HTML without a 500 error.
 * Email templates ({@code tmpl/}) and partials ({@code head}, {@code menubar}, {@code footer}) are covered
 * by {@link MailTemplateTest} and {@link JteOrphanTemplateTest} respectively.
 * <p>
 * Templates needing runtime-created entities (membership edit, payment edit, event-collection variant/mail/stats)
 * reuse the same {@code .jte} file as their {@code /new} counterparts and are covered once data exists in CI.
 */
@Tag("integration")
public class JteTemplateCoverageTest extends PlaywrightBaseTest {

    record TemplatePage(String template, String path) {}

    @Test
    void allPageTemplatesRender() {
        Page page = browserContext.newPage();
        page.setDefaultTimeout(15_000);

        page.navigate(BASE_URL + "/auth-sim");
        page.waitForLoadState();

        List<TemplatePage> failures = new ArrayList<>();
        for (TemplatePage tp : pageTemplates()) {
            if (!tryRender(page, tp)) {
                failures.add(tp);
            }
        }

        Page guestPage = browser.newContext(
                        new Browser.NewContextOptions().setLocale("fr-FR").setTimezoneId("Europe/Paris"))
                .newPage();
        guestPage.setDefaultTimeout(15_000);
        TemplatePage notLogged = new TemplatePage("auth/notlogged.jte", "/event/my");
        if (!tryRender(guestPage, notLogged)) {
            failures.add(notLogged);
        }
        guestPage.context().close();
        page.close();

        assertTrue(
                failures.isEmpty(),
                "JTE templates that failed to render: "
                        + failures.stream().map(tp -> tp.template() + " (" + tp.path() + ")").toList());
    }

    private List<TemplatePage> pageTemplates() {
        List<TemplatePage> pages = new ArrayList<>();
        Map<String, String> map = new LinkedHashMap<>();

        map.put("event/events.jte", "/event");
        map.put("event/events.jte (group)", "/event/aaa");
        map.put("event/event.jte", "/event/3");
        map.put("event/eventEdit.jte", "/event/3/edit");
        map.put("event/eventEdit.jte (new)", "/event/new");
        map.put("event/myevents.jte", "/event/my");
        map.put("event/eventStatistics.jte", "/event/3/statistics");
        map.put("event/statistics.jte", "/statistics");
        map.put("event/eventMail.jte", "/event/3/mail");
        map.put("event/eventgroup.jte", "/eventgroup");
        map.put("event/eventgroupEdit.jte", "/eventgroup/1");
        map.put("event/eventcollection.jte", "/eventcollection");
        map.put("event/eventcollectionEdit.jte", "/eventcollection/new");
        map.put("event/eventAdminTree.jte", "/admin/events");

        map.put("registration/register.jte", "/event/1/register");
        map.put("registration/subEdit.jte", "/event/3/register/1");
        map.put("registration/subOptionEdit.jte", "/event/3/register/1/option/new");
        map.put("registration/playersubscriptionAdmin.jte", "/playersubscription-admin");
        map.put("registration/clubRegister.jte", "/club-register");
        map.put("registration/clubMembershipOptions.jte",
                "/club-register/membership-options?lastname=Doe&firstname=John&birthdate=2010-10-10");

        map.put("membership/membership.jte", "/membership");
        map.put("membership/membershipNew.jte", "/membership/new");
        map.put("membership/membershipOption.jte", "/membership-option-admin");
        map.put("membership/membershipOptionEdit.jte", "/membership-option-admin/1/edit");
        map.put("membership/membershipOptionEdit.jte (new)", "/membership-option-admin/new");
        map.put("membership/licenseAdmin.jte", "/license-admin");
        map.put("membership/licenseEdit.jte", "/license-admin/1");
        map.put("membership/licenseEdit.jte (new)", "/license-admin/new");
        map.put("membership/licensePriceAdmin.jte", "/license-admin/1/price");
        map.put("membership/licensePriceEdit.jte", "/license-admin/1/price/new");

        map.put("payment/payments.jte", "/payment");
        map.put("payment/paymentEdit.jte", "/payment/new");
        map.put("payment/paymentStatus.jte", "/payment/sim");
        map.put("payment/paymentAudit.jte", "/payment/audit");

        map.put("platform/admin.jte", "/admin");
        map.put("platform/adminOrg.jte", "/admin/org");
        map.put("club/seasonAdmin.jte", "/admin/seasons");
        map.put("club/seasonEdit.jte", "/admin/seasons/new");
        map.put("club/seasonEdit.jte (edit)", "/admin/seasons/1/edit");
        map.put("platform/adminMail.jte", "/admin/mail");
        map.put("platform/dashboard.jte", "/dashboard");
        map.put("platform/mentions.jte", "/mentions");
        map.put("platform/error.jte", "/admin/seasons/999999/edit");

        map.put("auth/loggedusers.jte", "/logged-users");
        map.put("auth/logas.jte", "/logas");
        map.put("auth/userauthorization.jte", "/user-authorization");

        map.put("notification/notification.jte", "/notification");
        map.put("notification/notificationEdit.jte", "/notification/new");

        map.put("player/database.jte", "/database");
        map.put("player/databasePlayers.jte", "/database/manual-players");
        map.put("player/players.jte", "/customplayer");
        map.put("player/customplayer.jte", "/customplayer/1");

        map.put("desk/eventDesk.jte", "/event/3/desk");

        for (Map.Entry<String, String> entry : map.entrySet()) {
            pages.add(new TemplatePage(entry.getKey(), entry.getValue()));
        }
        return pages;
    }

    private boolean tryRender(Page page, TemplatePage tp) {
        try {
            assertTemplateRenders(page, tp.path(), tp.template());
            return true;
        } catch (AssertionError e) {
            return false;
        }
    }

    private void assertTemplateRenders(Page page, String path, String templateName) {
        page.navigate(BASE_URL + path);
        page.waitForSelector("body", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.ATTACHED));

        String title = page.title();
        String html = page.content();

        assertFalse(
                title.contains("500") || title.contains("Error 500"),
                templateName + " (" + path + ") – title indicates server error: " + title);
        assertFalse(
                html.contains("HTTP ERROR 500") || html.contains("Failed to render template"),
                templateName + " (" + path + ") – body indicates server error");

        if ("auth/notlogged.jte".equals(templateName)) {
            assertTrue(
                    html.contains("connect") || html.contains("Connexion") || html.contains("login"),
                    templateName + " should show login prompt");
        } else if ("platform/error.jte".equals(templateName)) {
            assertTrue(
                    html.contains("error") || html.contains("Error") || html.contains("erreur"),
                    templateName + " should show error content");
        } else {
            assertTrue(
                    page.locator("main").count() > 0 || page.locator("nav").count() > 0,
                    templateName + " (" + path + ") – expected main or nav element");
            assertFalse(html.isBlank(), templateName + " (" + path + ") – empty body");
        }
    }
}

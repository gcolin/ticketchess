package com.github.gcolin.platform;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.gcolin.auth.LoggedUser;
import com.github.gcolin.notification.Notifications;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Renders JTE templates that are not exposed via a dedicated HTTP route
 * ({@code platform/index.jte} and layout partials included via {@code @template}).
 */
class JteOrphanTemplateTest {

    private static TemplateEngine engine;
    private static Messages msg;
    private static Page pageConfig;
    private static LoggedUser user;
    private static Notifications notifications;
    private static StateService state;

    @BeforeAll
    static void initEngine() {
        engine = TemplateEngine.createPrecompiled(ContentType.Html);
        msg = new Messages(Locale.FRENCH);
        pageConfig = new Page();
        pageConfig.setTitle("Test Club");
        pageConfig.setOrgName("Test Club");
        pageConfig.setContactUrl("https://example.org/contact");
        user = Mockito.mock(LoggedUser.class);
        Mockito.when(user.isLogged()).thenReturn(true);
        Mockito.when(user.getEmail()).thenReturn("admin@test.com");
        Mockito.when(user.getUsername()).thenReturn("Admin");
        Mockito.when(user.isAdmin()).thenReturn(true);
        Mockito.when(user.getDebt()).thenReturn(0.0);
        Mockito.when(user.canSeeAdminMenu()).thenReturn(true);
        notifications = Mockito.mock(Notifications.class);
        Mockito.when(notifications.getGlobal()).thenReturn(Collections.emptyList());
        state = Mockito.mock(StateService.class);
        Mockito.when(state.getLogin()).thenReturn("/auth-sim");
    }

    @Test
    void shouldRenderIndexTemplate() throws IOException {
        String html = render(
                "platform/index.jte",
                model(
                        "contextPath", "",
                        "i18ntitle", msg.get("home.title"),
                        "msg", msg,
                        "notifications", notifications,
                        "page", "home",
                        "pageConfig", pageConfig,
                        "state", state,
                        "title", msg.get("home.title"),
                        "user", user,
                        "event", null,
                        "p", null));

        assertFalse(html.contains("${"));
        assertTrue(html.contains(msg.get("home.title")));
        assertTrue(html.contains("<main"));
    }

    @Test
    void shouldRenderLayoutPartials() throws IOException {
        String head = render(
                "platform/head.jte",
                model("title", "Test", "contextPath", "", "pageConfig", pageConfig));
        assertTrue(head.contains("<head>"));
        assertTrue(head.contains("Test"));

        String footer = render(
                "platform/footer.jte", model("contextPath", "", "pageConfig", pageConfig, "msg", msg));
        assertTrue(footer.contains("footer") || footer.contains("Test Club"));
    }

    private String render(String template, Map<String, Object> model) throws IOException {
        StringOutput output = new StringOutput();
        engine.render(template, model, output);
        return output.toString();
    }

    private static Map<String, Object> model(Object... keyValues) {
        Map<String, Object> model = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            model.put((String) keyValues[i], keyValues[i + 1]);
        }
        return model;
    }
}

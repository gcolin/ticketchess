package com.github.gcolin.platform;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.io.File;
import java.nio.file.Path;
import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class PlaywrightBaseTest {

    protected Playwright playwright;
    protected Browser browser;
    private Server server;
    protected BrowserContext browserContext;
    protected static final String BASE_URL = "http://localhost:8080";
    protected int port;
    private String previousConfigDir;
    private String previousTestProperty;
    private String previousLoadtestProperty;

    @BeforeEach
    public void setup() throws Exception {
        previousTestProperty = System.getProperty("test");
        previousLoadtestProperty = System.getProperty("loadtest");
        System.setProperty("test", "true");
        System.setProperty("loadtest", "true");
        previousConfigDir = System.getProperty("CONFIG_DIR");
        System.setProperty(
                "CONFIG_DIR",
                Path.of("src/test/resources").toAbsolutePath().normalize().toString());
        server = new Server(8080);
        WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setBaseResourceAsString("src/main/resources/webapp");
        context.setParentLoaderPriority(true);
        server.setHandler(context);
        server.start();
        port = 8080;
        boolean headless = Boolean.parseBoolean(System.getProperty("playwright.headless", "true"));
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));

        browserContext = browser.newContext(
                new Browser.NewContextOptions().setLocale("fr-FR").setTimezoneId("Europe/Paris"));
        File emails = new File("emails");
        if (emails.exists()) {
            for (File f : emails.listFiles()) {
                f.delete();
            }
        }
    }

    public void login(Page page) {
        page.navigate(BASE_URL);
        Locator bouton = page.locator("#googleauth");
        assertTrue(bouton.isVisible());
        bouton.click();
        page.waitForSelector("#navbarDropdown");
    }

    protected String getBaseUrl(String url) {
        return url.split("\\?")[0];
    }

    @AfterEach
    public void cleanupContext() throws Exception {
        if (browserContext != null) {
            browserContext.close();
        }
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
        if (server != null) {
            server.stop();
            server.destroy();
        }
        AppContext.shutdown();
        restoreProperty("CONFIG_DIR", previousConfigDir);
        restoreProperty("test", previousTestProperty);
        restoreProperty("loadtest", previousLoadtestProperty);
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}

package com.github.gcolin.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.gcolin.payment.RibService;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AdminApiRibTest {

    @TempDir
    Path tempDir;

    @Test
    void ribPageShouldExposeMissingFile() throws Exception {
        AdminApi api = new AdminApi();
        RibService ribService = mock(RibService.class);
        when(ribService.exists()).thenReturn(false);
        inject(api, "ribService", ribService);
        inject(api, "logoService", missingLogo());
        inject(api, "backgroundService", missingBackground());
        inject(api, "config", configWithProps());

        JteHtml html = api.orgPage("", "", "club");

        assertEquals("platform/adminOrg.jte", html.getTemplate());
        assertEquals(false, html.getModel().get("ribAvailable"));
        assertEquals("Event Test", ((Map<?, ?>) html.getModel().get("cfg")).get("title"));
    }

    @Test
    void ribPageShouldExposeExistingFileSize() throws Exception {
        Path ribFile = tempDir.resolve("rib.pdf");
        Files.writeString(ribFile, "%PDF-1.4 test");

        AdminApi api = new AdminApi();
        RibService ribService = mock(RibService.class);
        when(ribService.exists()).thenReturn(true);
        when(ribService.getRibFile()).thenReturn(ribFile);
        inject(api, "ribService", ribService);
        inject(api, "logoService", missingLogo());
        inject(api, "backgroundService", missingBackground());
        inject(api, "config", configWithProps());

        JteHtml html = api.orgPage("ribUploaded", "", "files");

        assertEquals(true, html.getModel().get("ribAvailable"));
        assertEquals("ribUploaded", html.getModel().get("success"));
        assertNotNull(html.getModel().get("ribSize"));
    }

    @Test
    void uploadRibShouldRedirectAfterSave() throws Exception {
        AdminApi api = new AdminApi();
        RibService ribService = mock(RibService.class);
        inject(api, "ribService", ribService);
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/admin/org?success=ribUploaded&tab=files#files")));

        InputStream file = new ByteArrayInputStream("%PDF-1.4".getBytes(StandardCharsets.US_ASCII));
        Response response = api.uploadRib(file);

        verify(ribService).save(file);
        assertEquals(200, response.getStatus());
        assertEquals("/admin/org?success=ribUploaded&tab=files#files", readRedirect(response));
    }

    @Test
    void uploadRibShouldRedirectWhenFileIsNotPdf() throws Exception {
        AdminApi api = new AdminApi();
        RibService ribService = mock(RibService.class);
        doThrow(new jakarta.ws.rs.WebApplicationException("RIB file must be a PDF", 400))
                .when(ribService)
                .save(org.mockito.ArgumentMatchers.any());
        inject(api, "ribService", ribService);
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/admin/org?error=invalidRib&tab=files#files")));

        Response response = api.uploadRib(new ByteArrayInputStream("hello".getBytes(StandardCharsets.US_ASCII)));

        assertEquals(200, response.getStatus());
        assertEquals("/admin/org?error=invalidRib&tab=files#files", readRedirect(response));
    }

    @Test
    void uploadRibShouldRedirectOnIoFailure() throws Exception {
        AdminApi api = new AdminApi();
        RibService ribService = mock(RibService.class);
        doThrow(new IOException("disk")).when(ribService).save(org.mockito.ArgumentMatchers.any());
        inject(api, "ribService", ribService);
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/admin/org?error=ribUploadFailed&tab=files#files")));

        Response response = api.uploadRib(new ByteArrayInputStream("%PDF-1.4".getBytes(StandardCharsets.US_ASCII)));

        assertEquals(200, response.getStatus());
        assertEquals("/admin/org?error=ribUploadFailed&tab=files#files", readRedirect(response));
    }

    @Test
    void saveConfigShouldPersistClubTab() throws Exception {
        AdminApi api = new AdminApi();
        Config config = mock(Config.class);
        inject(api, "config", config);
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/admin/org?success=configSaved&tab=club")));

        MultivaluedMap<String, String> form = new MultivaluedHashMap<>();
        form.putSingle("tab", "club");
        form.putSingle("title", "New Title");
        form.putSingle("org.name", "Club");
        form.putSingle("membership.notif.emails", "admin@club.fr");

        Response response = api.saveConfig(form);

        assertEquals(303, response.getStatus());
        verify(config).updateProperties(org.mockito.ArgumentMatchers.argThat(updates ->
                "New Title".equals(updates.get("title"))
                        && "Club".equals(updates.get("org.name"))
                        && "admin@club.fr".equals(updates.get("membership.notif.emails"))));
    }

    @Test
    void deleteLogoShouldRedirect() throws Exception {
        AdminApi api = new AdminApi();
        LogoService logoService = mock(LogoService.class);
        Config config = mock(Config.class);
        inject(api, "logoService", logoService);
        inject(api, "config", config);
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/admin/org?success=logoDeleted&tab=files#files")));

        Response response = api.deleteLogo();

        verify(logoService).delete();
        verify(config).applyRuntime();
        assertEquals(200, response.getStatus());
        assertEquals("/admin/org?success=logoDeleted&tab=files#files", readRedirect(response));
    }

    private static LogoService missingLogo() {
        LogoService logoService = mock(LogoService.class);
        when(logoService.exists()).thenReturn(false);
        return logoService;
    }

    private static BackgroundService missingBackground() {
        BackgroundService backgroundService = mock(BackgroundService.class);
        when(backgroundService.exists()).thenReturn(false);
        return backgroundService;
    }

    private static Config configWithProps() {
        Config config = mock(Config.class);
        Page page = new Page();
        page.setClubRegisterEnabled(true);
        when(config.getOrgFormValues()).thenReturn(Map.of("title", "Event Test", "org.name", "Club"));
        when(config.getPage()).thenReturn(page);
        when(config.isSecretConfigured(anyString())).thenReturn(false);
        return config;
    }

    private static String readRedirect(Response response) {
        Object entity = response.getEntity();
        if (!(entity instanceof Map<?, ?> map)) {
            return null;
        }
        Object redirect = map.get("redirect");
        return redirect == null ? null : redirect.toString();
    }

    private static UriInfo mockUriInfo(URI target) {
        UriInfo uriInfo = mock(UriInfo.class);
        UriBuilder uriBuilder = mock(UriBuilder.class);
        when(uriInfo.getBaseUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        when(uriBuilder.queryParam(anyString(), anyString())).thenReturn(uriBuilder);
        when(uriBuilder.fragment(anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(target);
        return uriInfo;
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

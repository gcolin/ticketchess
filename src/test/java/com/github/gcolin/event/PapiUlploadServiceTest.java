package com.github.gcolin.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class PapiUlploadServiceTest {

    @Test
    void testUploadRejectsMissingFile() {
        PapiUlploadService service = new PapiUlploadService();

        WebApplicationException ex = Assertions.assertThrows(
                WebApplicationException.class,
                () -> service.upload("login", "password", Path.of("does-not-exist.papi")));

        Assertions.assertEquals(
                Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    void testUploadRejectsNullFile() {
        PapiUlploadService service = new PapiUlploadService();

        WebApplicationException ex =
                Assertions.assertThrows(WebApplicationException.class, () -> service.upload("login", "password", null));

        Assertions.assertEquals(
                Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    void testToFormEncodesKeysAndValues() throws Exception {
        Method toForm = PapiUlploadService.class.getDeclaredMethod("toForm", Map.class);
        toForm.setAccessible(true);

        Map<String, String> params = new HashMap<>();
        params.put("a b", "x+y");
        params.put("mail", "alice@example.com");

        String encoded = (String) toForm.invoke(null, params);

        Assertions.assertTrue(encoded.contains("a+b=x%2By"));
        Assertions.assertTrue(encoded.contains("mail=alice%40example.com"));
        Assertions.assertTrue(encoded.contains("&"));
    }

    @Test
    void testParseUploadErrorScenarios() throws Exception {
        Method parseUploadError = PapiUlploadService.class.getDeclaredMethod("parseUploadError", String.class);
        parseUploadError.setAccessible(true);

        String noLabel = (String) parseUploadError.invoke(null, "<html><body>ok</body></html>");
        Assertions.assertNull(noLabel);

        String success = (String) parseUploadError.invoke(
                null,
                "<span id=\"ctl00_ContentPlaceHolderMain_LabelError\">"
                        + "Transfert du fichier : tournoi.papi (123 octets) achevé"
                        + "</span>");
        Assertions.assertNull(success);

        String failure = (String) parseUploadError.invoke(
                null, "<span id=\"ctl00_ContentPlaceHolderMain_LabelError\">Erreur serveur</span>");
        Assertions.assertEquals("Erreur serveur", failure);
    }

    @Test
    void testParseDocExtractsViewFields() throws Exception {
        PapiUlploadService service = new PapiUlploadService();
        Method parseDoc = PapiUlploadService.class.getDeclaredMethod("parseDoc", Map.class, String.class);
        parseDoc.setAccessible(true);

        Map<String, String> params = new HashMap<>();
        params.put("old", "value");

        String html = "<html><body>"
                + "<input id=\"__VIEWSTATE\" value=\"vs\"/>"
                + "<input id=\"__VIEWSTATEGENERATOR\" value=\"vsg\"/>"
                + "<input id=\"__EVENTVALIDATION\" value=\"ev\"/>"
                + "</body></html>";

        parseDoc.invoke(service, params, html);

        Assertions.assertEquals(3, params.size());
        Assertions.assertEquals("vs", params.get("__VIEWSTATE"));
        Assertions.assertEquals("vsg", params.get("__VIEWSTATEGENERATOR"));
        Assertions.assertEquals("ev", params.get("__EVENTVALIDATION"));
        Assertions.assertFalse(params.containsKey("old"));
    }

    @Test
    void testToMultipartBuildsPayload() throws Exception {
        Method toMultipart = PapiUlploadService.class.getDeclaredMethod(
                "toMultipart", Map.class, String.class, Path.class, String.class);
        toMultipart.setAccessible(true);

        Path tempFile = Files.createTempFile("papi-upload-test", ".txt");
        Files.writeString(tempFile, "sample-content");

        Map<String, String> params = Map.of("k1", "v1", "k2", "v2");

        Object publisher = toMultipart.invoke(null, params, "boundary-123", tempFile, "uploadField");

        Assertions.assertNotNull(publisher);
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testGetPageAndPostFormUseHttpClientResponseBody() throws Exception {
        Method getPage = PapiUlploadService.class.getDeclaredMethod("getPage", HttpClient.class, String.class);
        getPage.setAccessible(true);
        Method postForm =
                PapiUlploadService.class.getDeclaredMethod("postForm", HttpClient.class, String.class, Map.class);
        postForm.setAccessible(true);

        HttpClient client = mock(HttpClient.class);
        HttpResponse<String> response1 = response("<html>GET</html>");
        HttpResponse<String> response2 = response("<html>POST</html>");

        when(client.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response1, response2);

        String getBody = (String) getPage.invoke(null, client, "http://localhost");
        String postBody = (String) postForm.invoke(null, client, "http://localhost", Map.of("a", "b"));

        Assertions.assertEquals("<html>GET</html>", getBody);
        Assertions.assertEquals("<html>POST</html>", postBody);
    }

    @Test
    @Disabled
    void testUploadSuccessWithMockedHttpClient() throws Exception {
        PapiUlploadService service = new PapiUlploadService();
        Path tempFile = Files.createTempFile("papi-upload-ok", ".papi");
        Files.writeString(tempFile, "dummy");

        HttpClient.Builder builder = mock(HttpClient.Builder.class);
        HttpClient client = mock(HttpClient.class);

        String firstPage = "<input id=\"__VIEWSTATE\" value=\"vs1\"/>"
                + "<input id=\"__VIEWSTATEGENERATOR\" value=\"vsg1\"/>"
                + "<input id=\"__EVENTVALIDATION\" value=\"ev1\"/>";
        String loginOk = "<input id=\"__VIEWSTATE\" value=\"vs2\"/>"
                + "<input id=\"__VIEWSTATEGENERATOR\" value=\"vsg2\"/>"
                + "<input id=\"__EVENTVALIDATION\" value=\"ev2\"/>"
                + "<a id=\"ctl00_ContentPlaceHolderMain_LinkViewTournoi\">view</a>"
                + "<a id=\"ctl00_ContentPlaceHolderMain_CmdUploadPapi\">upload</a>";

        when(builder.cookieHandler(any())).thenReturn(builder);
        when(builder.build()).thenReturn(client);
        HttpResponse<String> firstResponse = response(firstPage);
        HttpResponse<String> loginPostResponse = response("<html>login posted</html>");
        HttpResponse<String> tournamentPageResponse = response(loginOk);
        HttpResponse<String> uploadResponse = response("<html>done</html>");
        when(client.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenReturn(firstResponse, loginPostResponse, tournamentPageResponse, uploadResponse);

        try (MockedStatic<HttpClient> httpClientStatic = Mockito.mockStatic(HttpClient.class)) {
            httpClientStatic.when(HttpClient::newBuilder).thenReturn(builder);

            boolean ok = service.upload("login", "password", tempFile);
            Assertions.assertTrue(ok);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void testUploadUnauthorizedWhenLoginPageHasNoViewLink() throws Exception {
        PapiUlploadService service = new PapiUlploadService();
        Path tempFile = Files.createTempFile("papi-upload-ko", ".papi");
        Files.writeString(tempFile, "dummy");

        HttpClient.Builder builder = mock(HttpClient.Builder.class);
        HttpClient client = mock(HttpClient.class);

        String firstPage = "<input id=\"__VIEWSTATE\" value=\"vs1\"/>"
                + "<input id=\"__VIEWSTATEGENERATOR\" value=\"vsg1\"/>"
                + "<input id=\"__EVENTVALIDATION\" value=\"ev1\"/>";
        String loginKo = "<input id=\"__VIEWSTATE\" value=\"vs2\"/>"
                + "<input id=\"__VIEWSTATEGENERATOR\" value=\"vsg2\"/>"
                + "<input id=\"__EVENTVALIDATION\" value=\"ev2\"/>";

        when(builder.cookieHandler(any())).thenReturn(builder);
        when(builder.build()).thenReturn(client);
        HttpResponse<String> firstResponse = response(firstPage);
        HttpResponse<String> loginPostResponse = response("<html>login posted</html>");
        HttpResponse<String> loginKoResponse = response(loginKo);
        when(client.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenReturn(firstResponse, loginPostResponse, loginKoResponse);

        try (MockedStatic<HttpClient> httpClientStatic = Mockito.mockStatic(HttpClient.class)) {
            httpClientStatic.when(HttpClient::newBuilder).thenReturn(builder);

            WebApplicationException ex = Assertions.assertThrows(
                    WebApplicationException.class, () -> service.upload("login", "password", tempFile));

            Assertions.assertEquals(
                    Response.Status.UNAUTHORIZED.getStatusCode(),
                    ex.getResponse().getStatus());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private static HttpResponse<String> response(String body) throws Exception {
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.body()).thenReturn(body);
        return response;
    }
}

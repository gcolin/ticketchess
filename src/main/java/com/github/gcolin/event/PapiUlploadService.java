package com.github.gcolin.event;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class PapiUlploadService {

    private static final String VIEW_STATE = "__VIEWSTATE";
    private static final String VIEW_STATE_GENERATOR = "__VIEWSTATEGENERATOR";
    private static final String EVENT_VALIDATION = "__EVENTVALIDATION";
    private static final String BASE_URL = "http://admin.echecs.asso.fr";
    private static final String UPLOAD_EVENT = "ctl00$ContentPlaceHolderMain$CmdUploadPapi";
    private static final String UPLOAD_FILE_FIELD = "ctl00$ContentPlaceHolderMain$UploadPapi";
    private static final String UPLOAD_LINK_ID = "ctl00_ContentPlaceHolderMain_CmdUploadPapi";
    private static final String VIEW_LINK_ID = "ctl00_ContentPlaceHolderMain_LinkViewTournoi";
    private static final String ERROR_LABEL_ID = "ctl00_ContentPlaceHolderMain_LabelError";
    private static final Pattern UPLOAD_SUCCESS =
            Pattern.compile("^Transfert du fichier : .* \\(\\d+ octets\\) achevé$");

    public boolean upload(String login, String password, Path papiFile) throws IOException, InterruptedException {
        if (papiFile == null || !Files.isRegularFile(papiFile)) {
            throw new WebApplicationException("PAPI file is required", Response.Status.BAD_REQUEST);
        }

        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .build();

        Map<String, String> formParams = new HashMap<>();
        parseDoc(formParams, getPage(client, BASE_URL));

        formParams.put("ctl00$TextLogin", login);
        formParams.put("ctl00$TextPassword", password);
        formParams.put("ctl00$CmdLogin.x", "12");
        formParams.put("ctl00$CmdLogin.y", "6");

        String page = postForm(client, BASE_URL + "/Default.aspx", formParams);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/MonTournoi.aspx"))
                .build();
        
        page = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
        Document doc = parseDoc(formParams, page);

        if (doc.select("#" + VIEW_LINK_ID).isEmpty()) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        if (doc.select("#" + UPLOAD_LINK_ID).isEmpty()) {
            throw new WebApplicationException(
                    "Upload link not found, tournament may be marked as finished on the FFE website.",
                    Response.Status.CONFLICT);
        }

        formParams.put("__EVENTTARGET", UPLOAD_EVENT);
        formParams.put("__EVENTARGUMENT", "");

        String boundary = "----Boundary" + UUID.randomUUID();
        HttpRequest uploadRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/MonTournoi.aspx"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(toMultipart(formParams, boundary, papiFile, UPLOAD_FILE_FIELD))
                .build();

        String uploadPage = client.send(uploadRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .body();
        String error = parseUploadError(uploadPage);
        if (error != null) {
            throw new WebApplicationException(error, Response.Status.BAD_GATEWAY);
        }
        return true;
    }

    private static String getPage(HttpClient client, String url) throws IOException, InterruptedException {
        HttpRequest request =
                HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .body();
    }

    private static String postForm(HttpClient client, String url, Map<String, String> params)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(BodyPublishers.ofString(toForm(params)))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .body();
    }

    private Document parseDoc(Map<String, String> params, String html) {
        Document doc = Jsoup.parse(html);
        params.clear();
        params.put(VIEW_STATE, inputValue(doc, VIEW_STATE));
        params.put(VIEW_STATE_GENERATOR, inputValue(doc, VIEW_STATE_GENERATOR));
        params.put(EVENT_VALIDATION, inputValue(doc, EVENT_VALIDATION));
        return doc;
    }

    private static String inputValue(Document doc, String id) {
        Element input = doc.select("#" + id).first();
        return input != null && input.hasAttr("value") ? input.attr("value") : "";
    }

    private static String parseUploadError(String html) {
        Element label = Jsoup.parse(html).select("#" + ERROR_LABEL_ID).first();
        if (label == null) {
            return null;
        }
        String text = label.text().trim();
        if (text.isEmpty() || UPLOAD_SUCCESS.matcher(text).matches()) {
            return null;
        }
        return text;
    }

    private static String toForm(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private static HttpRequest.BodyPublisher toMultipart(
            Map<String, String> data, String boundary, Path file, String fileField) throws IOException {
        List<byte[]> byteArrays = new ArrayList<>();

        for (Map.Entry<String, String> entry : data.entrySet()) {
            byteArrays.add(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            byteArrays.add(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            byteArrays.add(entry.getValue().getBytes(StandardCharsets.UTF_8));
            byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
        }

        String mimeType = Files.probeContentType(file);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        byteArrays.add(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        byteArrays.add(("Content-Disposition: form-data; name=\"" + fileField + "\"; filename=\"" + file.getFileName()
                        + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        byteArrays.add(("Content-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        byteArrays.add(Files.readAllBytes(file));
        byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
        byteArrays.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        return BodyPublishers.ofByteArrays(byteArrays);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 3) {
            System.err.println("Usage: PapiUlploadService <login> <password> <papi-file>");
            System.exit(1);
        }
        boolean ok = new PapiUlploadService().upload(args[0], args[1], Path.of(args[2]));
        System.out.println(ok ? "OK" : "FAILED");
    }
}

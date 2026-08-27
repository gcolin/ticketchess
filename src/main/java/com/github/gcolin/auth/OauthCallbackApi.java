package com.github.gcolin.auth;

import com.github.gcolin.platform.Caches;
import com.github.gcolin.platform.Config;
import io.jsonwebtoken.Jwts;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import org.json.JSONObject;

@Path("/{callback: oauth-callback|keycloak-callback}")
public class OauthCallbackApi {

    @Inject
    private LoggedUser loggedUser;

    @Inject
    private Caches caches;

    @Inject
    private Config config;

    @Context
    UriInfo uriInfo;

    protected HttpResponse<String> requestToken(String form) throws IOException, InterruptedException {
        String tokenUrl = config.getOauthTokenUrl();
        if (tokenUrl == null || tokenUrl.isBlank()) {
            throw new IOException("OAuth token URL is not configured");
        }
        HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        return HttpClient.newHttpClient().send(tokenRequest, HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> requestUserinfo(String accessToken) throws IOException, InterruptedException {
        String userinfoUrl = config.getOauthUserinfoUrl();
        if (userinfoUrl == null || userinfoUrl.isBlank()) {
            throw new IOException("OAuth userinfo URL is not configured");
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(userinfoUrl))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    @GET
    public Response callback(@QueryParam("code") String code, @QueryParam("state") String state) {
        if (code == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing code")
                    .build();
        }

        String clientId = nullToEmpty(config.getOauthClientId());
        String clientSecret = nullToEmpty(config.getOauthClientSecret());
        String form = "grant_type=authorization_code"
                + "&code="
                + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&client_id="
                + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&client_secret="
                + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                + "&redirect_uri="
                + URLEncoder.encode(config.getOauthRedirectUri(), StandardCharsets.UTF_8);

        try {
            HttpResponse<String> tokenResponse = requestToken(form);
            if (tokenResponse.statusCode() != 200) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Token exchange failed")
                        .build();
            }

            JSONObject json = new JSONObject(tokenResponse.body());
            JSONObject profile = profileFromTokenResponse(json);
            if (profile == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Unable to read user profile")
                        .build();
            }

            String username = profile.optString("preferred_username");
            if (!profile.isNull("name") && !profile.optString("name").isBlank()) {
                username = profile.optString("name");
            }
            if (username == null || username.isBlank()) {
                username = profile.optString("email");
            }
            String email = profile.optString("email");

            loggedUser.setEmail(email);
            loggedUser.setUsername(username);
            loggedUser.setLogged(true);
            loggedUser.setAdmin(config.getAdmins().contains(email));
            caches.getDebtCache().invalidateAll();

            String idToken = json.optString("id_token", null);
            String jwt = Jwts.builder()
                    .subject(email)
                    .issuer(username)
                    .id(idToken)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30))
                    .signWith(config.getKeys())
                    .compact();

            NewCookie newCookie = new NewCookie.Builder("remember_me")
                    .path("/")
                    .value(jwt)
                    .maxAge(30 * 24 * 60 * 60)
                    .httpOnly(true)
                    .secure(false)
                    .build();

            URI redirectUri;
            if (state != null) {
                String decodedState = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
                redirectUri = uriInfo.getBaseUriBuilder().path(decodedState).build();
            } else {
                redirectUri = uriInfo.getBaseUri();
            }

            return Response.seeOther(redirectUri).cookie(newCookie).build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Response.serverError().entity("Token exchange interrupted").build();
        } catch (IOException e) {
            return Response.serverError()
                    .entity("I/O error during token exchange")
                    .build();
        }
    }

    private JSONObject profileFromTokenResponse(JSONObject json) throws IOException, InterruptedException {
        String idToken = json.optString("id_token", null);
        if (idToken != null && !idToken.isBlank()) {
            String[] parts = idToken.split("\\.");
            if (parts.length >= 2) {
                String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                return new JSONObject(payload);
            }
        }
        String accessToken = json.optString("access_token", null);
        if (accessToken != null && !accessToken.isBlank() && config.getOauthUserinfoUrl() != null) {
            HttpResponse<String> userinfo = requestUserinfo(accessToken);
            if (userinfo.statusCode() == 200) {
                return new JSONObject(userinfo.body());
            }
        }
        return null;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

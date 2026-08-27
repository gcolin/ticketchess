package com.github.gcolin.auth;

import com.github.gcolin.platform.Config;
import com.github.gcolin.auth.ActiveLoggedUsers;
import com.github.gcolin.auth.LoggedUser;
import io.jsonwebtoken.Claims;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;

@Path("logout")
public class LogoutApi {

    @Inject
    private Properties properties;

    @Inject
    private Config config;

    @Context
    HttpServletRequest request;

    @Inject
    private LoggedUser loggerUser;

    @Inject
    private ActiveLoggedUsers activeLoggedUsers;

    @Context
    UriInfo uriInfo;

    @GET
    public Response logout() {
        var session = request.getSession(false);
        if (session != null) {
            activeLoggedUsers.remove(session.getId());
            session.invalidate();
        }

        NewCookie cookie = new NewCookie.Builder("remember_me")
                .value("")
                .path("/")
                .maxAge(0)
                .build();

        String logoutUrl = config.getOauthLogoutUrl();
        if (logoutUrl != null && !logoutUrl.isBlank()) {
            Claims claims = loggerUser.getClaims();
            if (claims != null) {
                Date nowPlus5Min = new Date(new Date().getTime() + 5 * 60 * 1000);
                if (claims.getIssuedAt().before(nowPlus5Min)) {
                    String id = claims.getId();
                    if (id != null) {
                        return Response.seeOther(URI.create(logoutUrl
                                        + "?id_token_hint="
                                        + id
                                        + "&post_logout_redirect_uri="
                                        + URLEncoder.encode(
                                                properties.getProperty("baseurl", "http://localhost:8080"),
                                                StandardCharsets.UTF_8)))
                                .cookie(cookie)
                                .build();
                    }
                }
            }
        }
        return Response.seeOther(uriInfo.getBaseUri()).cookie(cookie).build();
    }
}

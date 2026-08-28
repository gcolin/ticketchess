package com.github.gcolin.auth;

import com.github.gcolin.auth.RequirePermission;
import com.github.gcolin.platform.Config;
import com.github.gcolin.auth.PermissionCode;
import com.github.gcolin.platform.Caches;
import com.github.gcolin.auth.LoggedUser;
import io.jsonwebtoken.Jwts;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Properties;

@Path("auth-sim")
public class AuthSim {

    @Inject
    private Properties properties;

    @Inject
    private Caches caches;

    @Inject
    private LoggedUser loggerUser;

    @Inject
    private Config config;

    @Context
    UriInfo uriInfo;

    @Context
    HttpServletRequest request;

    @GET
    @Path("logAs")
    @RequirePermission(PermissionCode.USER_IMPERSONATE)
    public Response logAs(
            @QueryParam("email") String email,
            @QueryParam("name") String name,
            @QueryParam("admin") @DefaultValue("false") boolean admin) {
        requireImpersonationAllowed();
        loggerUser.setEmail(email);
        loggerUser.setUsername(name);
        loggerUser.setLogged(true);
        boolean effectiveAdmin = admin || config.getAdmins().contains(email);
        loggerUser.setAdmin(effectiveAdmin);
        caches.getDebtCache().invalidateAll();
        caches.getPermissionCache().invalidateAll();
        rememberAuthInSession(email, effectiveAdmin);

        String jwt = Jwts.builder()
                .subject(email)
                .issuer(name)
                .claim("admin", effectiveAdmin)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30))
                .signWith(config.getKeys(), Config.JWT_ALGORITHM)
                .compact();

        NewCookie newCookie = new NewCookie.Builder("remember_me")
                .httpOnly(true)
                .maxAge(60 * 60 * 24 * 30)
                .path("/")
                .value(jwt)
                .build();

        return Response.seeOther(uriInfo.getBaseUri()).cookie(newCookie).build();
    }

    @GET
    public Response doGet(@QueryParam("state") String state) throws ServletException, IOException {
        requireDevAuthSim();

        loggerUser.setEmail(properties.getProperty("auth.USER_EMAIL"));
        loggerUser.setUsername(properties.getProperty("auth.USER_NAME"));
        loggerUser.setLogged(true);
        loggerUser.setAdmin(true);
        caches.getDebtCache().invalidateAll();
        caches.getPermissionCache().invalidateAll();
        rememberAuthInSession(loggerUser.getEmail(), true);

        String jwt = Jwts.builder()
                .subject(loggerUser.getEmail())
                .issuer(loggerUser.getUsername())
                .claim("admin", true)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30)) // 30 jours
                .signWith(config.getKeys(), Config.JWT_ALGORITHM)
                .compact();

        NewCookie newCookie = new NewCookie.Builder("remember_me")
                .httpOnly(true)
                .maxAge(60 * 60 * 24 * 30)
                .path("/")
                .value(jwt)
                .build();

        if (state != null) {
            String decodedState = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
            return Response.seeOther(
                            uriInfo.getBaseUriBuilder().path(decodedState).build())
                    .cookie(newCookie)
                    .build();
        } else {
            return Response.seeOther(uriInfo.getBaseUri()).cookie(newCookie).build();
        }
    }

    private void rememberAuthInSession(String email, boolean admin) {
        if (request != null) {
            request.getSession(true).setAttribute("auth.email", email);
            request.getSession(true).setAttribute("auth.admin", admin);
        }
    }

    private void requireDevAuthSim() {
        if (config.isOauthEnabled() || !config.isAuthSimEnabled()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
    }

    private void requireImpersonationAllowed() {
        if (!config.isOauthEnabled() && !config.isTestMode()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
    }
}

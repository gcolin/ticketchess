package com.github.gcolin.auth;

import com.github.gcolin.platform.RateLimit;
import com.github.gcolin.platform.Config;
import com.github.gcolin.platform.Caches;
import com.github.gcolin.platform.Redirects;
import com.github.gcolin.auth.LoggedUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;

@Path("login")
public class LoginApi {

    @Inject
    private Config config;

    @Inject
    private LoggedUser loggedUser;

    @Inject
    private Caches caches;

    @Context
    private HttpServletRequest request;

    @Context
    private UriInfo uriInfo;

    @GET
    @RateLimit(maxAttempts = 3, windowSeconds = 60)
    public Response loginByMail(@QueryParam("jwt") String jwt, @QueryParam("redirect_uri") String redirectUri) {
        if (jwt == null || jwt.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing jwt query parameter")
                    .build();
        }

        final Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(config.getKeys())
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();
        } catch (JwtException ex) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid or expired token")
                    .build();
        }

        String email = claims.getSubject();
        if (email == null || email.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid token payload")
                    .build();
        }

        request.getSession(true);
        loggedUser.setEmail(email);
        loggedUser.setUsername(claims.getIssuer() == null || claims.getIssuer().isBlank() ? email : claims.getIssuer());
        loggedUser.setLogged(true);
        loggedUser.setAdmin(config.getAdmins().contains(email));
        caches.getDebtCache().invalidateAll();

        NewCookie newCookie = new NewCookie.Builder("remember_me")
                .httpOnly(true)
                .maxAge(60 * 60 * 24 * 30)
                .path("/")
                .value(jwt)
                .build();

        URI redirectTo = Redirects.safeRedirect(redirectUri, uriInfo.getBaseUri(), uriInfo);

        return Response.seeOther(redirectTo).cookie(newCookie).build();
    }
}

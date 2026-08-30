package com.github.gcolin.auth;

import com.github.gcolin.auth.RequireRole;
import com.github.gcolin.auth.AuthorizationScopeType;
import com.github.gcolin.auth.RoleCode;
import com.github.gcolin.platform.Caches;
import com.github.gcolin.auth.LoggedUser;
import com.github.gcolin.auth.UserAuthorizationDao;
import jakarta.inject.Inject;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import com.github.gcolin.platform.JteHtml;

@Path("user-authorization")
@RequireRole(RoleCode.ADMIN)
public class UserAuthorizationApi {

    @Inject
    private UserAuthorizationDao userAuthorizationDao;

    @Inject
    private LoggedUser user;

    @Inject
    private Caches caches;

    @Context
    UriInfo uriInfo;

    @GET
    public JteHtml page() {
        Map<String, Object> model = new HashMap<>();
        model.put("authorizations", userAuthorizationDao.allOrdered());
        model.put("roles", Arrays.asList(RoleCode.values()));
        model.put("scopeTypes", Arrays.asList(AuthorizationScopeType.values()));
        return new JteHtml(model, "auth/userauthorization.jte");
    }

    @POST
    public Response save(
            @FormParam("toRemove") String toRemove,
            @FormParam("id") Integer id,
            @FormParam("email") String email,
            @FormParam("role") String role,
            @FormParam("scopeType") String scopeType,
            @FormParam("scopeId") String scopeId,
            @FormParam("validUntil") String validUntil) {
        if ("true".equals(toRemove)) {
            if (id != null) {
                userAuthorizationDao.remove(id);
                caches.getRoleCache().invalidateAll();
            }
            return redirectToPage();
        }

        RoleCode roleCode = RoleCode.valueOf(role);
        AuthorizationScopeType authorizationScopeType = AuthorizationScopeType.valueOf(scopeType);
        Integer parsedScopeId = parseNullableInteger(scopeId);

        if (authorizationScopeType != AuthorizationScopeType.GLOBAL && parsedScopeId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("scopeId is required when scopeType is not GLOBAL")
                    .build();
        }

        userAuthorizationDao.upsert(
                email,
                roleCode,
                authorizationScopeType,
                parsedScopeId,
                parseNullableDateTime(validUntil),
                user == null ? null : user.getEmail());
        caches.getRoleCache().invalidateAll();

        return redirectToPage();
    }

    private Response redirectToPage() {
        return Response.seeOther(
                        uriInfo.getBaseUriBuilder().path("user-authorization").build())
                .build();
    }

    private Integer parseNullableInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return Integer.parseInt(value.trim());
    }

    private LocalDateTime parseNullableDateTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}

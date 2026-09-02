package com.github.gcolin.auth;

import com.github.gcolin.platform.RequestContext;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import com.github.gcolin.platform.Redirects;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class RequireRoleFilter implements ContainerRequestFilter {

    @Context
    private ResourceInfo resourceInfo;

    @Context
    private UriInfo uriInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        RequireRole requiredRole = findRequiredRole();
        if (requiredRole == null) {
            return;
        }

        var user = RequestContext.require().loggedUser();

        if (user == null || user.getEmail() == null) {
            if (shouldRedirectToAdmin(requestContext)) {
                URI adminUri = uriInfo.getBaseUriBuilder().path("admin").build();
                requestContext.abortWith(
                        Response.seeOther(Redirects.toSameOriginRelative(adminUri)).build());
            } else {
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                        .entity("you must be logged")
                        .type(MediaType.TEXT_PLAIN)
                        .build());
            }
            return;
        }

        RoleCode role = requiredRole.value();
        RoleCode[] alternatives = requiredRole.or();
        boolean allowed = user.hasRole(role)
                || (alternatives.length > 0 && Arrays.stream(alternatives).anyMatch(user::hasRole));
        if (!allowed) {
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity("missing role: " + role.name())
                    .type(MediaType.TEXT_PLAIN)
                    .build());
        }
    }

    private RequireRole findRequiredRole() {
        if (resourceInfo == null) {
            return null;
        }

        Method method = resourceInfo.getResourceMethod();
        if (method != null) {
            RequireRole methodAnnotation = method.getAnnotation(RequireRole.class);
            if (methodAnnotation != null) {
                return methodAnnotation;
            }
        }

        Class<?> resourceClass = resourceInfo.getResourceClass();
        if (resourceClass == null) {
            return null;
        }
        return resourceClass.getAnnotation(RequireRole.class);
    }

    private static boolean shouldRedirectToAdmin(ContainerRequestContext requestContext) {
        if (!"GET".equalsIgnoreCase(requestContext.getMethod())) {
            return false;
        }
        String path = requestContext.getUriInfo().getPath();
        if (path != null && path.contains("/export")) {
            return false;
        }
        return acceptsHtml(requestContext.getAcceptableMediaTypes());
    }

    private static boolean acceptsHtml(List<MediaType> acceptableMediaTypes) {
        if (acceptableMediaTypes == null || acceptableMediaTypes.isEmpty()) {
            return true;
        }
        for (MediaType mediaType : acceptableMediaTypes) {
            if (MediaType.TEXT_HTML_TYPE.isCompatible(mediaType) || MediaType.WILDCARD_TYPE.isCompatible(mediaType)) {
                return true;
            }
        }
        return false;
    }
}

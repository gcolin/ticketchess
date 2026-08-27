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
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Method;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class RequirePermissionFilter implements ContainerRequestFilter {

    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        RequirePermission requiredPermission = findRequiredPermission();
        if (requiredPermission == null) {
            return;
        }

        var user = RequestContext.require().loggedUser();

        if (user == null || user.getEmail() == null) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("you must be logged")
                    .type(MediaType.TEXT_PLAIN)
                    .build());
            return;
        }

        PermissionCode permission = requiredPermission.value();
        if (!user.hasPermission(permission)) {
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity("missing permission: " + permission.name())
                    .type(MediaType.TEXT_PLAIN)
                    .build());
        }
    }

    private RequirePermission findRequiredPermission() {
        if (resourceInfo == null) {
            return null;
        }

        Method method = resourceInfo.getResourceMethod();
        if (method != null) {
            RequirePermission methodAnnotation = method.getAnnotation(RequirePermission.class);
            if (methodAnnotation != null) {
                return methodAnnotation;
            }
        }

        Class<?> resourceClass = resourceInfo.getResourceClass();
        if (resourceClass == null) {
            return null;
        }
        return resourceClass.getAnnotation(RequirePermission.class);
    }
}

package com.github.gcolin.auth;

import com.github.gcolin.platform.JteHtml;
import com.github.gcolin.platform.RequestContext;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Collections;

@Provider
@LoggedOnly
@Priority(Priorities.AUTHORIZATION)
public class LoggedOnlyFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        var user = RequestContext.require().loggedUser();

        if (user == null || user.getEmail() == null) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new JteHtml(Collections.emptyMap(), "auth/notlogged.jte"))
                    .build());
        }
    }
}

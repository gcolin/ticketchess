package com.github.gcolin.auth;

import com.github.gcolin.platform.AppContext;
import com.github.gcolin.platform.RequestContext;
import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class LoggedUserTrackerFilter implements ContainerRequestFilter {

    @Context
    private HttpServletRequest request;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        var user = RequestContext.require().loggedUser();
        var activeLoggedUsers = AppContext.get().activeLoggedUsers();

        if (user == null || !user.isLogged() || user.getEmail() == null) {
            return;
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            activeLoggedUsers.touch(session.getId(), user.getEmail(), user.getUsername(), user.isAdmin());
        }
    }
}

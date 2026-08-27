package com.github.gcolin.auth;

import com.github.gcolin.platform.AppContext;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

public class HttpSessionDestroyedListener implements HttpSessionListener {

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        try {
            AppContext.get().activeLoggedUsers().remove(event.getSession().getId());
        } catch (Exception ignored) {
            // Application context may be unavailable during shutdown
        }
    }
}

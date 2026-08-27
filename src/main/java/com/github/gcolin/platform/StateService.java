package com.github.gcolin.platform;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class StateService {

    private HttpServletRequest request;
    private Config config;

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public String getValue() {
        HttpServletRequest httpRequest = request;
        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && requestURI.startsWith(contextPath)) {
            requestURI = requestURI.substring(contextPath.length());
        }
        if (requestURI.startsWith("/")) {
            requestURI = requestURI.substring(1);
        }
        String queryString = httpRequest.getQueryString();
        if (queryString != null) {
            requestURI += "?" + queryString;
        }
        return Base64.getUrlEncoder().encodeToString(requestURI.getBytes(StandardCharsets.UTF_8));
    }

    public String getLogin() {
        return config.getLoginUrl() + getValue();
    }
}

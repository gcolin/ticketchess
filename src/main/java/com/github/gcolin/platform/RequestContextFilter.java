package com.github.gcolin.platform;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

public class RequestContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest)) {
            chain.doFilter(request, response);
            return;
        }
        if (isReadOnlyMethod(httpRequest)) {
            RequestContext.openReadOnly(httpRequest);
        } else {
            RequestContext.open(httpRequest);
        }
        try {
            chain.doFilter(request, response);
            RequestContext.commit();
        } catch (RuntimeException | IOException | ServletException e) {
            RequestContext.rollback();
            throw e;
        } finally {
            RequestContext.close();
        }
    }

    private static boolean isReadOnlyMethod(HttpServletRequest request) {
        String method = request.getMethod();
        return "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method);
    }
}

package com.github.gcolin.desk;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * Jersey filter that lets WebSocket upgrade requests continue down the chain
 * so the container WebSocket upgrade filter (Jetty or Tomcat) can handle them.
 */
public class JerseyWebSocketAwareFilter extends ServletContainer {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest http
                && "websocket".equalsIgnoreCase(http.getHeader("Upgrade"))) {
            chain.doFilter(request, response);
            return;
        }
        super.doFilter(request, response, chain);
    }
}

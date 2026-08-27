package com.github.gcolin.platform;

import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class RateLimitFilter implements ContainerResponseFilter {

    // Map<ip, List<Long timestamps>>
    private final Map<String, List<Long>> attemptsByIp = new HashMap<>();

    @Context
    private ResourceInfo resourceInfo;

    @Context
    private HttpServletRequest servletRequest;

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        RateLimit rateLimit = findRateLimit();
        if (rateLimit == null || responseContext.getStatus() != Response.Status.UNAUTHORIZED.getStatusCode()) {
            return;
        }

        String clientIp = getClientIp(requestContext);
        int maxAttempts = rateLimit.maxAttempts();
        int windowSeconds = rateLimit.windowSeconds();

        synchronized (attemptsByIp) {
            long now = System.currentTimeMillis();
            long windowMs = windowSeconds * 1000L;

            List<Long> attempts = attemptsByIp.computeIfAbsent(clientIp, k -> new ArrayList<>());

            // Remove old attempts outside the window
            attempts.removeIf(timestamp -> (now - timestamp) > windowMs);

            // Record this failed attempt
            attempts.add(now);

            // Check if we exceeded the limit
            if (attempts.size() >= maxAttempts) {
                responseContext.setStatus(Response.Status.TOO_MANY_REQUESTS.getStatusCode());
                responseContext.setEntity("Too many failed attempts. Please try again later.");
            }

            // Clean up empty entries to avoid memory leak
            if (attempts.isEmpty()) {
                attemptsByIp.remove(clientIp);
            }
        }
    }

    private RateLimit findRateLimit() {
        if (resourceInfo == null) {
            return null;
        }

        Method method = resourceInfo.getResourceMethod();
        if (method != null) {
            RateLimit methodAnnotation = method.getAnnotation(RateLimit.class);
            if (methodAnnotation != null) {
                return methodAnnotation;
            }
        }

        Class<?> resourceClass = resourceInfo.getResourceClass();
        if (resourceClass == null) {
            return null;
        }
        return resourceClass.getAnnotation(RateLimit.class);
    }

    private String getClientIp(ContainerRequestContext requestContext) {
        // Try X-Forwarded-For first (for proxied requests)
        String xForwardedFor = requestContext.getHeaderString("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Get the first IP if there are multiple
            return xForwardedFor.split(",")[0].trim();
        }

        // Try X-Real-IP (another common proxy header)
        String xRealIp = requestContext.getHeaderString("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        // Fall back to servlet remote address
        if (servletRequest != null) {
            String remoteAddr = servletRequest.getRemoteAddr();
            if (remoteAddr != null && !remoteAddr.isBlank()) {
                return remoteAddr;
            }
        }

        return "unknown";
    }
}

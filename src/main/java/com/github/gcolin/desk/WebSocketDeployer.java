package com.github.gcolin.desk;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketDeployer implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketDeployer.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServerContainer container =
                (ServerContainer) sce.getServletContext().getAttribute(ServerContainer.class.getName());
        if (container == null) {
            logger.error("Jakarta WebSocket ServerContainer is not available");
            return;
        }
        try {
            container.setDefaultMaxSessionIdleTimeout(EventDeskWebSocket.IDLE_TIMEOUT_MS);
            container.addEndpoint(EventDeskWebSocket.class);
            logger.info("Registered EventDeskWebSocket endpoint (idleTimeout={}ms)", EventDeskWebSocket.IDLE_TIMEOUT_MS);
        } catch (DeploymentException e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate WebSocket mapping")) {
                logger.info(
                        "EventDeskWebSocket already registered by the container (idleTimeout={}ms)",
                        EventDeskWebSocket.IDLE_TIMEOUT_MS);
                return;
            }
            throw new IllegalStateException("cannot deploy EventDeskWebSocket", e);
        }
    }
}

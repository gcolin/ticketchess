package com.github.gcolin.app;

import java.util.logging.Logger;
import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class Main {
    public static void main(String[] args) throws Exception {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        int port = 8080;
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isBlank()) {
            port = Integer.parseInt(portEnv);
        }

        Server server = new Server();
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.addCustomizer(new ForwardedRequestCustomizer());
        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
        connector.setPort(port);
        server.addConnector(connector);

        WebAppContext context = new WebAppContext();
        context.setContextPath("/");

        var resource = ResourceFactory.of(context).newResource(Main.class.getResource("/webapp"));

        context.setBaseResource(resource);

        context.setParentLoaderPriority(true);

        server.setHandler(context);

        server.start();
        Logger.getGlobal().info("Server started on http://localhost:" + port);
        server.join();
    }
}

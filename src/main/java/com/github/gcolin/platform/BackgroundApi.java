package com.github.gcolin.platform;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

@Path("background")
public class BackgroundApi {

    @Inject
    private BackgroundService backgroundService;

    @GET
    public Response download() {
        if (!backgroundService.exists()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        StreamingOutput stream = ClientDisconnect.copyFile(backgroundService.getBackgroundFile());
        return Response.ok(stream)
                .type(backgroundService.getContentType())
                .header("Cache-Control", "no-cache")
                .build();
    }
}

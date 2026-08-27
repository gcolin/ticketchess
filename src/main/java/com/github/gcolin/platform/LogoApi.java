package com.github.gcolin.platform;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

@Path("logo")
public class LogoApi {

    @Inject
    private LogoService logoService;

    @GET
    public Response download() {
        if (!logoService.exists()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        StreamingOutput stream = ClientDisconnect.copyFile(logoService.getLogoFile());
        return Response.ok(stream)
                .type(logoService.getContentType())
                .header("Cache-Control", "no-cache")
                .build();
    }
}

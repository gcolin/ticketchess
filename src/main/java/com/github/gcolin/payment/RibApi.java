package com.github.gcolin.payment;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import com.github.gcolin.platform.ClientDisconnect;

@Path("rib")
public class RibApi {

    @Inject
    private RibService ribService;

    @GET
    public Response download() {
        if (!ribService.exists()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        StreamingOutput stream = ClientDisconnect.copyFile(ribService.getRibFile());
        return Response.ok(stream)
                .type("application/pdf")
                .header("Content-Disposition", "inline; filename=\"" + RibService.FILE_NAME + "\"")
                .build();
    }
}

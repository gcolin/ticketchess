package com.github.gcolin.platform;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

@Path("signature")
public class SignatureApi {

    @Inject
    private SignatureService signatureService;

    @GET
    public Response download() {
        if (!signatureService.exists()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        StreamingOutput stream = ClientDisconnect.copyFile(signatureService.getSignatureFile());
        return Response.ok(stream)
                .type(signatureService.getContentType())
                .header("Cache-Control", "no-cache")
                .build();
    }
}

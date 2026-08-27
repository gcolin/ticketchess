package com.github.gcolin.platform;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;

@Path("/")
public class HomeApi {

    @Context
    UriInfo uriInfo;

    @GET
    public Response home() throws IOException {
        URI uri = uriInfo.getBaseUriBuilder().path("event").build();

        return Response.seeOther(uri).build();
    }
}

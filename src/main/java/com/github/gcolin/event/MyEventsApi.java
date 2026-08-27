package com.github.gcolin.event;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;

@Path("myevents")
public class MyEventsApi {

    @Context
    UriInfo uriInfo;

    @GET
    public Response events() {
        URI uri = uriInfo.getBaseUriBuilder().path("event").path("my").build();

        return Response.seeOther(uri).build();
    }
}

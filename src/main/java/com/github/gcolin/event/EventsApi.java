package com.github.gcolin.event;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;

@Path("events")
public class EventsApi {

    @Context
    UriInfo uriInfo;

    @GET
    public Response events() {
        URI uri = uriInfo.getBaseUriBuilder().path("event").build();

        return Response.seeOther(uri).build();
    }

    @GET
    @Path("{category}")
    public Response events(@PathParam("category") String category) {
        URI uri = uriInfo.getBaseUriBuilder().path("event").path(category).build();

        return Response.seeOther(uri).build();
    }
}

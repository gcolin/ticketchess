package com.github.gcolin.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Field;
import java.net.URI;
import org.junit.jupiter.api.Test;

class EventsApiTest {

    @Test
    void eventsShouldRedirectToEvent() throws Exception {
        EventsApi api = new EventsApi();

        UriInfo uriInfo = mock(UriInfo.class);
        UriBuilder uriBuilder = mock(UriBuilder.class);
        URI target = URI.create("http://localhost:8080/event");

        when(uriInfo.getBaseUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(target);

        injectUriInfo(api, uriInfo);

        Response response = api.events();

        assertEquals(303, response.getStatus());
        assertEquals(target, response.getLocation());
    }

    @Test
    void eventsCategoryShouldRedirectToEventCategory() throws Exception {
        EventsApi api = new EventsApi();

        UriInfo uriInfo = mock(UriInfo.class);
        UriBuilder uriBuilder = mock(UriBuilder.class);
        URI target = URI.create("http://localhost:8080/event/rapid");

        when(uriInfo.getBaseUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(target);

        injectUriInfo(api, uriInfo);

        Response response = api.events("rapid");

        assertEquals(303, response.getStatus());
        assertEquals(target, response.getLocation());
    }

    private static void injectUriInfo(EventsApi api, UriInfo uriInfo) throws Exception {
        Field field = EventsApi.class.getDeclaredField("uriInfo");
        field.setAccessible(true);
        field.set(api, uriInfo);
    }
}

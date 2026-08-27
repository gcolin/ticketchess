package com.github.gcolin.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Field;
import java.net.URI;
import org.junit.jupiter.api.Test;

class HomeApiTest {

    @Test
    void homeShouldRedirectToEvent() throws Exception {
        HomeApi api = new HomeApi();

        UriInfo uriInfo = mock(UriInfo.class);
        UriBuilder uriBuilder = mock(UriBuilder.class);
        URI target = URI.create("http://localhost:8080/event");

        when(uriInfo.getBaseUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(target);

        Field field = HomeApi.class.getDeclaredField("uriInfo");
        field.setAccessible(true);
        field.set(api, uriInfo);

        Response response = api.home();

        assertNotNull(response);
        assertEquals(303, response.getStatus());
        assertEquals(target, response.getLocation());
    }
}

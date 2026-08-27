package com.github.gcolin.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.gcolin.event.Event;
import com.github.gcolin.player.DisplayPlayer;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import com.github.gcolin.event.PapiService;
import com.github.gcolin.event.EventDao;
import com.github.gcolin.event.EventCache;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EventPapiApiTest {

    @Test
    void papiShouldReturnFileAsStreamingResponse() throws Exception {
        EventPapiApi api = new EventPapiApi();
        EventDao eventDao = mock(EventDao.class);
        PapiService papiService = mock(PapiService.class);

        Event event = new Event();
        event.setId(1);
        event.setName("Rapid 2024");

        EventCache cache = new EventCache();
        cache.event = event;
        cache.players = List.of();

        File tempFile = File.createTempFile("test", ".papi");
        tempFile.deleteOnExit();

        when(eventDao.buildCache(1)).thenReturn(cache);
        when(papiService.generatePapiFile(event, cache.players)).thenReturn(tempFile);

        inject(api, "eventService", eventDao);
        inject(api, "papiService", papiService);

        Response response = api.papi(1);

        assertEquals(200, response.getStatus());
        assertEquals("application/x-msaccess", response.getMediaType().toString());
    }

    @Test
    void papiShouldEncodeEventNameInFilename() throws Exception {
        EventPapiApi api = new EventPapiApi();
        EventDao eventDao = mock(EventDao.class);
        PapiService papiService = mock(PapiService.class);

        Event event = new Event();
        event.setId(1);
        event.setName("Tournoi 2024-05 Blitz!");

        EventCache cache = new EventCache();
        cache.event = event;
        cache.players = List.of();

        File tempFile = File.createTempFile("test", ".papi");
        tempFile.deleteOnExit();

        when(eventDao.buildCache(1)).thenReturn(cache);
        when(papiService.generatePapiFile(event, cache.players)).thenReturn(tempFile);

        inject(api, "eventService", eventDao);
        inject(api, "papiService", papiService);

        Response response = api.papi(1);

        assertEquals(200, response.getStatus());
    }

    @Test
    void papiShouldExcludeCancelledPlayers() throws Exception {
        EventPapiApi api = new EventPapiApi();
        EventDao eventDao = mock(EventDao.class);
        PapiService papiService = mock(PapiService.class);

        Event event = new Event();
        event.setId(1);
        event.setName("Rapid 2024");

        DisplayPlayer active = new DisplayPlayer();
        active.setStatus(PlayerSubscriptionStatus.PAID);
        DisplayPlayer cancelled = new DisplayPlayer();
        cancelled.setStatus(PlayerSubscriptionStatus.CANCELLED);

        EventCache cache = new EventCache();
        cache.event = event;
        cache.players = List.of(active, cancelled);

        File tempFile = File.createTempFile("test", ".papi");
        tempFile.deleteOnExit();

        when(eventDao.buildCache(1)).thenReturn(cache);
        when(papiService.generatePapiFile(event, List.of(active))).thenReturn(tempFile);

        inject(api, "eventService", eventDao);
        inject(api, "papiService", papiService);

        Response response = api.papi(1);

        assertEquals(200, response.getStatus());
        ArgumentCaptor<List<DisplayPlayer>> captor = ArgumentCaptor.forClass(List.class);
        verify(papiService).generatePapiFile(eq(event), captor.capture());
        assertEquals(1, captor.getValue().size());
        assertTrue(captor.getValue().contains(active));
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

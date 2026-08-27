package com.github.gcolin.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.gcolin.event.EventDao;
import jakarta.ws.rs.core.Response;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class EventCsvApiTest {

    @Test
    void papiShouldReturnCsvResponseWithCorrectHeaders() throws Exception {
        EventCsvApi api = new EventCsvApi();
        EventDao eventDao = mock(EventDao.class);

        String csvContent = "id,name\n1,Alice\n";
        when(eventDao.buildCsv(42)).thenReturn(csvContent);

        inject(api, "eventService", eventDao);

        Response response = api.papi(42);

        assertEquals(200, response.getStatus());
        assertEquals("text/csv", response.getMediaType().toString());
        assertEquals("attachment; filename=\"42.csv\"", response.getHeaderString("Content-Disposition"));
        assertEquals(csvContent, response.getEntity());
    }

    @Test
    void papiShouldUseEventIdInFilename() throws Exception {
        EventCsvApi api = new EventCsvApi();
        EventDao eventDao = mock(EventDao.class);

        when(eventDao.buildCsv(7)).thenReturn("data");

        inject(api, "eventService", eventDao);

        Response response = api.papi(7);

        assertEquals("attachment; filename=\"7.csv\"", response.getHeaderString("Content-Disposition"));
    }

    @Test
    void papiShouldReturnEmptyCsvBodyWhenDaoReturnsEmptyString() throws Exception {
        EventCsvApi api = new EventCsvApi();
        EventDao eventDao = mock(EventDao.class);

        when(eventDao.buildCsv(99)).thenReturn("");

        inject(api, "eventService", eventDao);

        Response response = api.papi(99);

        assertEquals(200, response.getStatus());
        assertTrue(((String) response.getEntity()).isEmpty());
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

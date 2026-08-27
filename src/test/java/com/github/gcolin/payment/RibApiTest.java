package com.github.gcolin.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RibApiTest {

    @TempDir
    Path tempDir;

    @Test
    void downloadShouldReturnNotFoundWhenMissing() throws Exception {
        RibApi api = new RibApi();
        RibService ribService = mock(RibService.class);
        when(ribService.exists()).thenReturn(false);
        inject(api, "ribService", ribService);

        Response response = api.download();

        assertEquals(404, response.getStatus());
    }

    @Test
    void downloadShouldReturnPdfWhenFileExists() throws Exception {
        Path ribFile = tempDir.resolve("rib.pdf");
        Files.writeString(ribFile, "%PDF-1.4 test");

        RibApi api = new RibApi();
        RibService ribService = mock(RibService.class);
        when(ribService.exists()).thenReturn(true);
        when(ribService.getRibFile()).thenReturn(ribFile);
        inject(api, "ribService", ribService);

        Response response = api.download();

        assertEquals(200, response.getStatus());
        assertEquals("application/pdf", response.getMediaType().toString());
        assertNotNull(response.getHeaderString("Content-Disposition"));
        assertEquals("inline; filename=\"rib.pdf\"", response.getHeaderString("Content-Disposition"));
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

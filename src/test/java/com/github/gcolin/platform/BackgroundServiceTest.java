package com.github.gcolin.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BackgroundServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void saveShouldStoreJpegAndReplaceOtherFormats() throws Exception {
        BackgroundService service = service();
        byte[] jpeg = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 1, 2, 3, 4, 5, 6, 7, 8};

        service.save(new ByteArrayInputStream(jpeg));

        assertTrue(service.exists());
        assertEquals("image/jpeg", service.getContentType());
        assertTrue(Files.isRegularFile(tempDir.resolve("background.jpg")));

        service.delete();

        assertFalse(service.exists());
        assertFalse(Files.exists(tempDir.resolve("background.jpg")));
    }

    @Test
    void saveShouldRejectUnknownType() throws Exception {
        BackgroundService service = service();

        WebApplicationException ex = assertThrows(
                WebApplicationException.class,
                () -> service.save(new ByteArrayInputStream("hello world!!!".getBytes())));

        assertEquals(400, ex.getResponse().getStatus());
        assertFalse(service.exists());
    }

    private BackgroundService service() throws Exception {
        BackgroundService service = new BackgroundService();
        Config config = mock(Config.class);
        when(config.getConfigDir()).thenReturn(tempDir.toString());
        Field field = BackgroundService.class.getDeclaredField("config");
        field.setAccessible(true);
        field.set(service, config);
        return service;
    }
}

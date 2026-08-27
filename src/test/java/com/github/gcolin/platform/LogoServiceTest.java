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

class LogoServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void saveShouldStorePngAndReplaceOtherFormats() throws Exception {
        LogoService service = service();
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 1, 2, 3};

        service.save(new ByteArrayInputStream(png));

        assertTrue(service.exists());
        assertEquals("image/png", service.getContentType());
        assertTrue(Files.isRegularFile(tempDir.resolve("logo.png")));

        service.delete();

        assertFalse(service.exists());
        assertFalse(Files.exists(tempDir.resolve("logo.png")));
    }

    @Test
    void saveShouldRejectUnknownType() throws Exception {
        LogoService service = service();

        WebApplicationException ex = assertThrows(
                WebApplicationException.class,
                () -> service.save(new ByteArrayInputStream("hello world!!!".getBytes())));

        assertEquals(400, ex.getResponse().getStatus());
        assertFalse(service.exists());
    }

    private LogoService service() throws Exception {
        LogoService service = new LogoService();
        Config config = mock(Config.class);
        when(config.getConfigDir()).thenReturn(tempDir.toString());
        Field field = LogoService.class.getDeclaredField("config");
        field.setAccessible(true);
        field.set(service, config);
        return service;
    }
}

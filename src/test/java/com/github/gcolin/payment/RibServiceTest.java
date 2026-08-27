package com.github.gcolin.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.gcolin.platform.Config;
import jakarta.ws.rs.WebApplicationException;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RibServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void existsShouldBeFalseWhenFileIsMissing() throws Exception {
        RibService service = service();

        assertFalse(service.exists());
    }

    @Test
    void saveShouldStorePdfInConfigDir() throws Exception {
        RibService service = service();
        byte[] pdf = "%PDF-1.4 test".getBytes(StandardCharsets.US_ASCII);

        service.save(new ByteArrayInputStream(pdf));

        assertTrue(service.exists());
        assertEquals(pdf.length, Files.size(service.getRibFile()));
        assertTrue(Files.readAllBytes(service.getRibFile())[0] == '%');
    }

    @Test
    void deleteShouldRemovePdf() throws Exception {
        RibService service = service();
        service.save(new ByteArrayInputStream("%PDF-1.4".getBytes(StandardCharsets.US_ASCII)));
        assertTrue(service.exists());

        service.delete();

        assertFalse(service.exists());
    }

    @Test
    void saveShouldAcceptPdfWithLeadingWhitespace() throws Exception {
        RibService service = service();
        byte[] pdf = "\n\r  %PDF-1.7 bank rib".getBytes(StandardCharsets.US_ASCII);

        service.save(new ByteArrayInputStream(pdf));

        assertTrue(service.exists());
        assertEquals("%PDF-1.7 bank rib", new String(Files.readAllBytes(service.getRibFile()), StandardCharsets.US_ASCII));
    }

    @Test
    void saveShouldRejectNonPdf() throws Exception {
        RibService service = service();

        WebApplicationException ex = assertThrows(
                WebApplicationException.class,
                () -> service.save(new ByteArrayInputStream("hello".getBytes(StandardCharsets.US_ASCII))));

        assertEquals(400, ex.getResponse().getStatus());
        assertFalse(service.exists());
    }

    @Test
    void saveShouldRejectMissingFile() throws Exception {
        RibService service = service();

        WebApplicationException ex =
                assertThrows(WebApplicationException.class, () -> service.save(null));

        assertEquals(400, ex.getResponse().getStatus());
    }

    @Test
    void saveShouldRejectFileAboveMaxSize() throws Exception {
        RibService service = service();
        byte[] tooLarge = new byte[RibService.MAX_SIZE_BYTES + 1];
        tooLarge[0] = '%';
        tooLarge[1] = 'P';
        tooLarge[2] = 'D';
        tooLarge[3] = 'F';

        WebApplicationException ex = assertThrows(
                WebApplicationException.class, () -> service.save(new ByteArrayInputStream(tooLarge)));

        assertEquals(400, ex.getResponse().getStatus());
        assertFalse(service.exists());
    }

    @Test
    void saveShouldReplaceExistingFile() throws Exception {
        RibService service = service();
        service.save(new ByteArrayInputStream("%PDF-1.4 first".getBytes(StandardCharsets.US_ASCII)));
        byte[] second = "%PDF-1.4 second".getBytes(StandardCharsets.US_ASCII);

        service.save(new ByteArrayInputStream(second));

        assertEquals("PDF-1.4 second", new String(Files.readAllBytes(service.getRibFile()), StandardCharsets.US_ASCII)
                .substring(1));
    }

    private RibService service() throws Exception {
        RibService service = new RibService();
        Config config = mock(Config.class);
        when(config.getConfigDir()).thenReturn(tempDir.toString());
        Field field = RibService.class.getDeclaredField("config");
        field.setAccessible(true);
        field.set(service, config);
        return service;
    }
}

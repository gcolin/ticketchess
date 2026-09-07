package com.github.gcolin.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.gcolin.platform.Config;
import com.github.gcolin.platform.JteHtml;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DatabaseApiTest {

    @Test
    void pageShouldReturnFfeSettingsInModel() throws Exception {
        DatabaseApi api = new DatabaseApi();
        Config config = mock(Config.class);
        when(config.getProperty("ffe.excludeAffTypeN", "false")).thenReturn("true");

        inject(api, "luceneDb", mock(LuceneDb.class));
        inject(api, "config", config);

        JteHtml html = api.page(null);

        assertEquals("player/database.jte", html.getTemplate());
        Map<String, Object> model = html.getModel();
        assertEquals(true, model.get("excludeAffTypeN"));
        assertEquals(null, model.get("status"));
    }

    @Test
    void getFfeInfoShouldReturnFileInfo() throws Exception {
        DatabaseApi api = new DatabaseApi();
        LuceneDb luceneDb = mock(LuceneDb.class);

        File mockFile = mock(File.class);
        when(mockFile.exists()).thenReturn(true);
        when(mockFile.getAbsolutePath()).thenReturn("/path/to/Data.mdb");
        when(mockFile.lastModified()).thenReturn(1000000L);
        when(mockFile.length()).thenReturn(1024L);

        when(luceneDb.getMdbFile()).thenReturn(mockFile);

        inject(api, "luceneDb", luceneDb);

        String result = api.getFfeInfo();

        assertTrue(result.contains("/path/to/Data.mdb"));
        assertTrue(result.contains("1970-01-01"));
    }

    @Test
    void getFfeInfoShouldReturnNotFoundWhenFileNull() throws Exception {
        DatabaseApi api = new DatabaseApi();
        LuceneDb luceneDb = mock(LuceneDb.class);

        when(luceneDb.getMdbFile()).thenReturn(null);

        inject(api, "luceneDb", luceneDb);

        String result = api.getFfeInfo();

        assertEquals("Data.mdb not found", result);
    }

    @Test
    void getFideInfoShouldReturnFileInfo() throws Exception {
        DatabaseApi api = new DatabaseApi();
        LuceneDb luceneDb = mock(LuceneDb.class);

        File mockFile = mock(File.class);
        when(mockFile.exists()).thenReturn(true);
        when(mockFile.getAbsolutePath()).thenReturn("/path/to/fide.mdb");
        when(mockFile.lastModified()).thenReturn(2000000L);
        when(mockFile.length()).thenReturn(2048L);

        when(luceneDb.getFideFile()).thenReturn(mockFile);

        inject(api, "luceneDb", luceneDb);

        String result = api.getFideInfo();

        assertTrue(result.contains("/path/to/fide.mdb"));
        assertTrue(result.contains("1970-01-01"));
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

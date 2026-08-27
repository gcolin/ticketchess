package com.github.gcolin.event;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.gcolin.platform.Caches;
import com.github.gcolin.platform.SelectItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class EventGroupFilterTest {

    private EventGroupFilter filter;
    private Caches caches;
    private EventGroupDao eventGroupService;

    @BeforeEach
    void setUp() {
        caches = Mockito.mock(Caches.class);
        eventGroupService = Mockito.mock(EventGroupDao.class);
        filter = new EventGroupFilter();
        filter.setCaches(caches);
        filter.setEventGroupDao(eventGroupService);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetAllBuildsSelectItemsAndCachesResults() {
        Locale previousDefault = Locale.getDefault();
        Locale.setDefault(Locale.FRANCE);
        try {
            Cache<String, List<SelectItem>> cache = Mockito.mock(Cache.class);

            Mockito.when(caches.getEventGroups()).thenReturn(cache);
            Mockito.when(cache.getIfPresent("alpha")).thenReturn(null);

            EventGroup first = new EventGroup();
            first.setName("Beta");
            first.setShortname("beta");

            EventGroup second = new EventGroup();
            second.setName("Alpha");
            second.setShortname("alpha");

            Mockito.when(eventGroupService.all()).thenReturn(new ArrayList<>(List.of(first, second)));

            List<SelectItem> result = filter.getAll("alpha");

            Assertions.assertEquals(3, result.size());
            Assertions.assertEquals("Tournois principaux", result.get(0).getLabel());
            Assertions.assertEquals("", result.get(0).getValue());
            Assertions.assertFalse(result.get(0).isSelected());
            Assertions.assertEquals("Alpha", result.get(1).getLabel());
            Assertions.assertEquals("alpha", result.get(1).getValue());
            Assertions.assertTrue(result.get(1).isSelected());
            Assertions.assertEquals("Beta", result.get(2).getLabel());
            Assertions.assertEquals("beta", result.get(2).getValue());
            Assertions.assertFalse(result.get(2).isSelected());

            Mockito.verify(cache).put("alpha", result);
        } finally {
            Locale.setDefault(previousDefault);
        }
    }
}

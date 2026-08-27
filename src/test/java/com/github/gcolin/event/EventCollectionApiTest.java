package com.github.gcolin.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.gcolin.event.EventCollection;
import com.github.gcolin.event.EventCollectionOptionType;
import com.github.gcolin.platform.Caches;
import com.github.gcolin.registration.RegisterService;
import com.github.gcolin.event.EventCollectionDao;
import com.github.gcolin.event.EventCollectionOptionDao;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.github.gcolin.platform.JteHtml;

class EventCollectionApiTest {

    @Test
    void pageShouldReturnTemplateAndModel() throws Exception {
        EventCollectionApi api = new EventCollectionApi();
        EventCollectionDao dao = mock(EventCollectionDao.class);

        EventCollection one = new EventCollection();
        one.setId(1);
        one.setName("Festival");

        when(dao.allOrdered()).thenReturn(List.of(one));
        inject(api, "eventCollectionService", dao);

        JteHtml html = api.page("1", null, null, null);

        assertEquals("event/eventcollection.jte", html.getTemplate());
        assertTrue((Boolean) html.getModel().get("created"));
        @SuppressWarnings("unchecked")
        List<EventCollection> events = (List<EventCollection>) html.getModel().get("eventCollections");
        assertEquals(1, events.size());
    }

    @Test
    void createShouldReturnTemplateAndCreateMode() throws Exception {
        EventCollectionApi api = new EventCollectionApi();

        JteHtml html = api.create(null);

        assertEquals("event/eventcollectionEdit.jte", html.getTemplate());
        assertTrue((Boolean) html.getModel().get("createMode"));
        assertTrue(html.getModel().get("eventCollection") instanceof EventCollection);
    }

    @Test
    void postCreateShouldPersistAndRedirect() throws Exception {
        EventCollectionApi api = new EventCollectionApi();
        EventCollectionDao dao = mock(EventCollectionDao.class);
        EventCollectionOptionDao optionDao = mock(EventCollectionOptionDao.class);

        doAnswer(invocation -> {
                    EventCollection event = invocation.getArgument(0);
                    event.setId(11);
                    return null;
                })
                .when(dao)
                .persist(any(EventCollection.class));

        inject(api, "eventCollectionService", dao);
        inject(api, "eventCollectionOptionService", optionDao);
        inject(api, "caches", new Caches());
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/eventcollection/11?success=save")));

        Response response = api.post("create", null, " Open Series ", "12");

        assertEquals(303, response.getStatus());
        verify(dao).persist(any(EventCollection.class));
        verify(optionDao).setOption(11, EventCollectionOptionType.MAX_SUBSCRIPTIONS, "12");
    }

    @Test
    void postRemoveShouldReturnErrorWhenLinked() throws Exception {
        EventCollectionApi api = new EventCollectionApi();
        EventCollectionDao dao = mock(EventCollectionDao.class);
        EventCollectionOptionDao optionDao = mock(EventCollectionOptionDao.class);

        EventCollection event = new EventCollection();
        event.setId(8);
        when(dao.find(8)).thenReturn(event);
        when(dao.countLinkedEvents(8)).thenReturn(2L);

        inject(api, "eventCollectionService", dao);
    inject(api, "eventCollectionOptionService", optionDao);
        inject(api, "caches", new Caches());
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/eventcollection?error=linkedEvents")));

    Response response = api.post("remove", 8, null, null);

        assertEquals(303, response.getStatus());
    }

    @Test
    void postRemoveShouldDeleteWhenNoLink() throws Exception {
        EventCollectionApi api = new EventCollectionApi();
        EventCollectionDao dao = mock(EventCollectionDao.class);
        EventCollectionOptionDao optionDao = mock(EventCollectionOptionDao.class);

        EventCollection event = new EventCollection();
        event.setId(9);
        when(dao.find(9)).thenReturn(event);
        when(dao.countLinkedEvents(9)).thenReturn(0L);

        inject(api, "eventCollectionService", dao);
    inject(api, "eventCollectionOptionService", optionDao);
        inject(api, "caches", new Caches());
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/eventcollection?removed=1")));

    Response response = api.post("remove", 9, null, null);

        assertEquals(303, response.getStatus());
        verify(dao).remove(event);
    }

    @Test
    void postUpdateShouldMergeAndRedirect() throws Exception {
        EventCollectionApi api = new EventCollectionApi();
        EventCollectionDao dao = mock(EventCollectionDao.class);
        EventCollectionOptionDao optionDao = mock(EventCollectionOptionDao.class);
        RegisterService registerService = mock(RegisterService.class);

        EventCollection event = new EventCollection();
        event.setId(10);
        event.setName("Old Name");
        when(dao.find(10)).thenReturn(event);

        inject(api, "eventCollectionService", dao);
        inject(api, "eventCollectionOptionService", optionDao);
        inject(api, "registerService", registerService);
        inject(api, "caches", new Caches());
        inject(api, "uriInfo", mockUriInfo(URI.create("http://localhost:8080/eventcollection/10?success=save")));

        Response response = api.post("update", 10, " New Name ", "24");

        assertEquals(303, response.getStatus());
        assertEquals("New Name", event.getName());
        verify(dao).merge(event);
        verify(optionDao).setOption(10, EventCollectionOptionType.MAX_SUBSCRIPTIONS, "24");
        verify(registerService).promoteNextPendingSubscriptionInCollection(event);
    }

    @Test
    void editShouldReturnTemplate() throws Exception {
        EventCollectionApi api = new EventCollectionApi();
        EventCollectionDao dao = mock(EventCollectionDao.class);

        EventCollection event = new EventCollection();
        event.setId(6);
        event.setName("Spring Open");
        when(dao.find(6)).thenReturn(event);

        inject(api, "eventCollectionService", dao);

        JteHtml html = api.edit(6, "save", null);

        assertEquals("event/eventcollectionEdit.jte", html.getTemplate());
        assertEquals(event, html.getModel().get("eventCollection"));
        assertEquals("save", html.getModel().get("success"));
    }

    @Test
    void editShouldThrowNotFoundWhenMissing() throws Exception {
        EventCollectionApi api = new EventCollectionApi();
        EventCollectionDao dao = mock(EventCollectionDao.class);
        when(dao.find(999)).thenReturn(null);

        inject(api, "eventCollectionService", dao);

        assertThrows(NotFoundException.class, () -> api.edit(999, null, null));
    }

    private static UriInfo mockUriInfo(URI target) {
        UriInfo uriInfo = mock(UriInfo.class);
        UriBuilder uriBuilder = mock(UriBuilder.class);

        when(uriInfo.getBaseUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        when(uriBuilder.queryParam(anyString(), any())).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(target);

        return uriInfo;
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

package com.github.gcolin.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.gcolin.platform.TestContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RequireRoleFilterTest {

    @Test
    void filterShouldRedirectGetHtmlRequestsToAdminWhenNotLogged() throws Exception {
        RequireRoleFilter filter = new RequireRoleFilter();
        ContainerRequestContext ctx = mock(ContainerRequestContext.class);
        ResourceInfo resourceInfo = mock(ResourceInfo.class);
        UriInfo uriInfo = mock(UriInfo.class);
        UriBuilder uriBuilder = mock(UriBuilder.class);

        when(ctx.getMethod()).thenReturn("GET");
        when(ctx.getAcceptableMediaTypes()).thenReturn(List.of(MediaType.TEXT_HTML_TYPE));
        when(ctx.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("membership");
        when(uriInfo.getBaseUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path("admin")).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(URI.create("http://localhost:8080/admin"));

        Method method = SampleResource.class.getMethod("page");
        when(resourceInfo.getResourceMethod()).thenReturn(method);
        when(resourceInfo.getResourceClass()).thenReturn((Class) SampleResource.class);

        inject(filter, "resourceInfo", resourceInfo);
        inject(filter, "uriInfo", uriInfo);

        LoggedUser user = mock(LoggedUser.class);
        try (TestContext ignored = TestContext.open(user)) {
            when(user.getEmail()).thenReturn(null);
            filter.filter(ctx);
        }

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).abortWith(captor.capture());
        assertEquals(303, captor.getValue().getStatus());
        assertEquals("/admin", captor.getValue().getLocation().toString());
    }

    @Test
    void filterShouldReturnUnauthorizedForPostWhenNotLogged() throws Exception {
        RequireRoleFilter filter = new RequireRoleFilter();
        ContainerRequestContext ctx = mock(ContainerRequestContext.class);
        ResourceInfo resourceInfo = mock(ResourceInfo.class);

        when(ctx.getMethod()).thenReturn("POST");

        Method method = SampleResource.class.getMethod("page");
        when(resourceInfo.getResourceMethod()).thenReturn(method);
        when(resourceInfo.getResourceClass()).thenReturn((Class) SampleResource.class);

        inject(filter, "resourceInfo", resourceInfo);

        LoggedUser user = mock(LoggedUser.class);
        try (TestContext ignored = TestContext.open(user)) {
            when(user.getEmail()).thenReturn(null);
            filter.filter(ctx);
        }

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).abortWith(captor.capture());
        assertEquals(401, captor.getValue().getStatus());
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                var field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    @RequireRole(RoleCode.TRESORIER)
    static class SampleResource {
        public void page() {}
    }
}

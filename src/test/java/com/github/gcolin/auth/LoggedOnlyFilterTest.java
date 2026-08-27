package com.github.gcolin.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.github.gcolin.platform.TestContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LoggedOnlyFilterTest {

    @Test
    void filterShouldAbortWhenEmailMissing() throws Exception {
        LoggedOnlyFilter filter = new LoggedOnlyFilter();
        LoggedUser user = mock(LoggedUser.class);
        ContainerRequestContext ctx = mock(ContainerRequestContext.class);

        try (TestContext ignored = TestContext.open(user)) {
            filter.filter(ctx);
        }

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).abortWith(captor.capture());
        assertEquals(401, captor.getValue().getStatus());
    }
}

package com.github.gcolin.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConstraintViolationExceptionMapperTest {

    @Test
    void toResponseShouldBuildErrorModel() {
        ConstraintViolationExceptionMapper mapper = new ConstraintViolationExceptionMapper();

        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> cv = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("obj.field");
        when(cv.getPropertyPath()).thenReturn(path);
        when(cv.getMessage()).thenReturn("must not be null");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(cv));
        Response response = mapper.toResponse(ex);

        assertEquals(400, response.getStatus());
        JteHtml html = (JteHtml) response.getEntity();
        assertEquals("platform/error.jte", html.getTemplate());
        @SuppressWarnings("unchecked")
        java.util.List<String> errors = (java.util.List<String>) html.getModel().get("errors");
        assertTrue(errors.get(0).contains("field : must not be null"));
    }
}

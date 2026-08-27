package com.github.gcolin.platform;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<String> messages = exception.getConstraintViolations().stream()
                .map(cv -> {
                    String path = cv.getPropertyPath().toString();
                    String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                    return field + " : " + cv.getMessage();
                })
                .collect(Collectors.toList());

        Map<String, Object> model = new HashMap<>();
        model.put("statusCode", 400);
        model.put("statusMessage", "Requête invalide");
        model.put("errors", messages);

        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.TEXT_HTML + ";charset=UTF-8")
                .entity(new JteHtml(model, "platform/error.jte"))
                .build();
    }
}

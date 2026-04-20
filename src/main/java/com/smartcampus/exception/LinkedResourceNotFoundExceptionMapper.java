package com.smartcampus.exception;

import com.smartcampus.model.ErrorResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Logger;

/**
 * Part 5.2 — Maps LinkedResourceNotFoundException to HTTP 422 Unprocessable Entity.
 *
 * Why 422 over 404?
 * A 404 means the requested resource (the URL itself) was not found.
 * Here, the URL /api/v1/sensors is perfectly valid — the issue is that a
 * *reference inside the JSON payload* (the roomId field) points to a
 * non-existent resource. HTTP 422 precisely communicates that the request
 * was syntactically valid but semantically unprocessable due to a broken
 * dependency reference within the payload.
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {
    private static final Logger LOGGER = Logger.getLogger(LinkedResourceNotFoundExceptionMapper.class.getName());

    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        LOGGER.warning("Dependency validation failed: " + ex.getMessage());
        ErrorResponse error = new ErrorResponse(
            422,
            "Unprocessable Entity",
            ex.getMessage()
        );
        return Response.status(422)
                       .entity(error)
                       .type(MediaType.APPLICATION_JSON)
                       .build();
    }
}

package com.smartcampus.exception;

import com.smartcampus.model.ErrorResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Part 5.4 — Global safety net ExceptionMapper.
 *
 * Catches ALL uncaught Throwable instances (including RuntimeExceptions,
 * NullPointerExceptions, IndexOutOfBoundsExceptions, etc.) and returns a
 * clean HTTP 500 response with a generic message.
 *
 * Cybersecurity importance: Exposing raw Java stack traces reveals:
 *  - Internal class names and package structure (attack surface mapping)
 *  - Library/framework versions (known CVE lookup)
 *  - Business logic flow and data paths (logic exploitation)
 *  - File system paths (directory traversal intel)
 * This mapper ensures NONE of that is visible to external consumers.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {
        // Log the full trace server-side for debugging — never send it to the client
        LOGGER.log(Level.SEVERE, "Unhandled exception caught by global safety net", ex);
        ErrorResponse error = new ErrorResponse(
            500,
            "Internal Server Error",
            "An unexpected error occurred on the server. Please contact the system administrator."
        );
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                       .entity(error)
                       .type(MediaType.APPLICATION_JSON)
                       .build();
    }
}

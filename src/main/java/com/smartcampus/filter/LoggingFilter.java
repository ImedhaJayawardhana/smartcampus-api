package com.smartcampus.filter;

import javax.ws.rs.container.*;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Part 5.5 — API Request & Response Logging Filter.
 *
 * Implements both ContainerRequestFilter and ContainerResponseFilter
 * to log every incoming request and every outgoing response.
 *
 * Why filters over manual Logger.info() in every method?
 * Filters implement the "cross-cutting concern" principle. Adding logging
 * inside every resource method violates DRY (Don't Repeat Yourself), increases
 * maintenance burden, and risks forgetting to log certain endpoints. A filter
 * intercepts ALL requests/responses at the framework level automatically,
 * ensuring consistent, centralised observability without cluttering business logic.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    /**
     * Logs the HTTP method and URI for every incoming request.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info(String.format("[REQUEST]  Method: %-7s | URI: %s",
            requestContext.getMethod(),
            requestContext.getUriInfo().getRequestUri()));
    }

    /**
     * Logs the HTTP status code for every outgoing response.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOGGER.info(String.format("[RESPONSE] Method: %-7s | URI: %s | Status: %d %s",
            requestContext.getMethod(),
            requestContext.getUriInfo().getRequestUri(),
            responseContext.getStatus(),
            responseContext.getStatusInfo().getReasonPhrase()));
    }
}

package com.smartcampus;

import com.smartcampus.filter.LoggingFilter;
import com.smartcampus.exception.*;
import com.smartcampus.resource.DiscoveryResource;
import com.smartcampus.resource.RoomResource;
import com.smartcampus.resource.SensorResource;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

/**
 * JAX-RS Application Configuration class.
 *
 * Lifecycle Note: By default, JAX-RS creates a new instance of each
 * resource class per HTTP request (request-scoped). This class
 * registers all resources, filters, and exception mappers.
 *
 * The @ApplicationPath annotation sets the base URI for all REST endpoints.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends ResourceConfig {

    public SmartCampusApplication() {
        // Register resource classes
        register(DiscoveryResource.class);
        register(RoomResource.class);
        register(SensorResource.class);

        // Register exception mappers
        register(RoomNotEmptyExceptionMapper.class);
        register(LinkedResourceNotFoundExceptionMapper.class);
        register(SensorUnavailableExceptionMapper.class);
        register(GlobalExceptionMapper.class);

        // Register filters
        register(LoggingFilter.class);

        // Enable Jackson JSON support
        register(JacksonFeature.class);
    }
}

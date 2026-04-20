package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Part 1.2 — Discovery Endpoint.
 *
 * GET /api/v1
 * Returns API metadata including version info, admin contact, and a HATEOAS-style
 * resource map linking to all primary collections.
 *
 * HATEOAS (Hypermedia As The Engine Of Application State) is considered a hallmark
 * of mature RESTful APIs because it makes the API self-documenting: clients receive
 * links to related resources directly in responses, eliminating the need to hardcode
 * URIs. This reduces coupling between client and server — if URLs change, only the
 * server needs to update them. Client developers can explore the API dynamically
 * rather than relying on external (often outdated) documentation.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> response = new LinkedHashMap<>();

        // API versioning info
        response.put("api", "Smart Campus Sensor & Room Management API");
        response.put("version", "1.0.0");
        response.put("status", "operational");

        // Administrative contact
        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("team", "Smart Campus Infrastructure Team");
        contact.put("email", "smartcampus-admin@university.ac.uk");
        contact.put("documentation", "https://github.com/your-username/smartcampus-api");
        response.put("contact", contact);

        // HATEOAS-style resource links (hypermedia navigation)
        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms",    "/api/v1/rooms");
        resources.put("sensors",  "/api/v1/sensors");
        response.put("resources", resources);

        // Endpoint catalogue for client discovery
        Map<String, Object> endpoints = new LinkedHashMap<>();

        Map<String, String> roomEndpoints = new LinkedHashMap<>();
        roomEndpoints.put("list_all",  "GET    /api/v1/rooms");
        roomEndpoints.put("create",    "POST   /api/v1/rooms");
        roomEndpoints.put("get_by_id", "GET    /api/v1/rooms/{roomId}");
        roomEndpoints.put("delete",    "DELETE /api/v1/rooms/{roomId}");
        endpoints.put("rooms", roomEndpoints);

        Map<String, String> sensorEndpoints = new LinkedHashMap<>();
        sensorEndpoints.put("list_all",        "GET    /api/v1/sensors");
        sensorEndpoints.put("filter_by_type",  "GET    /api/v1/sensors?type={type}");
        sensorEndpoints.put("create",          "POST   /api/v1/sensors");
        sensorEndpoints.put("get_by_id",       "GET    /api/v1/sensors/{sensorId}");
        sensorEndpoints.put("readings_list",   "GET    /api/v1/sensors/{sensorId}/readings");
        sensorEndpoints.put("readings_post",   "POST   /api/v1/sensors/{sensorId}/readings");
        endpoints.put("sensors", sensorEndpoints);

        response.put("endpoints", endpoints);

        return Response.ok(response).build();
    }
}

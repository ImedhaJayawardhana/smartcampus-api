package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.util.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Part 3 — Sensor Resource.
 *
 * Manages the /api/v1/sensors collection.
 * Supports: GET all (with optional ?type filter), POST with roomId validation,
 * GET by ID, and sub-resource locator for /sensors/{id}/readings.
 *
 * Part 3.1 — @Consumes(APPLICATION_JSON):
 * If a client sends a request with Content-Type: text/plain or application/xml,
 * JAX-RS will reject it before the method body is even reached and automatically
 * return HTTP 415 Unsupported Media Type. This ensures strict contract enforcement
 * at the framework level without any manual checking needed in business logic.
 *
 * Part 3.2 — @QueryParam vs @PathParam for filtering:
 * Using GET /sensors?type=CO2 (query param) is superior to GET /sensors/type/CO2
 * (path param) for filtering because:
 *  - Query params are optional by nature — the base resource /sensors still works
 *  - Multiple filters compose naturally: ?type=CO2&status=ACTIVE
 *  - Path params imply a distinct sub-resource, which misleads clients
 *  - HTTP caching and standard tools treat query-filtered URLs correctly
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private static final Logger LOGGER = Logger.getLogger(SensorResource.class.getName());
    private final DataStore store = DataStore.getInstance();

    /**
     * GET /api/v1/sensors
     * GET /api/v1/sensors?type=CO2
     *
     * Returns all sensors, optionally filtered by type using a query parameter.
     */
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        Collection<Sensor> all = store.getSensors().values();

        List<Sensor> result;
        if (type != null && !type.isBlank()) {
            result = all.stream()
                        .filter(s -> s.getType().equalsIgnoreCase(type))
                        .collect(Collectors.toList());
            LOGGER.info("Fetching sensors filtered by type='" + type + "'. Count: " + result.size());
        } else {
            result = new ArrayList<>(all);
            LOGGER.info("Fetching all sensors. Count: " + result.size());
        }

        return Response.ok(result).build();
    }

    /**
     * GET /api/v1/sensors/{sensorId}
     * Returns a single sensor by its ID.
     */
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensorById(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("error", "Not Found",
                                          "message", "Sensor '" + sensorId + "' does not exist."))
                           .build();
        }
        return Response.ok(sensor).build();
    }

    /**
     * POST /api/v1/sensors
     *
     * Registers a new sensor. Validates that the referenced roomId exists
     * before saving. If the room is not found, throws LinkedResourceNotFoundException
     * which maps to HTTP 422 Unprocessable Entity.
     */
    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", "Bad Request", "message", "Sensor ID is required."))
                           .build();
        }
        if (store.getSensorById(sensor.getId()) != null) {
            return Response.status(Response.Status.CONFLICT)
                           .entity(Map.of("error", "Conflict",
                                          "message", "A sensor with ID '" + sensor.getId() + "' already exists."))
                           .build();
        }

        // Validate that the referenced roomId actually exists
        String roomId = sensor.getRoomId();
        if (roomId == null || roomId.isBlank()) {
            throw new LinkedResourceNotFoundException(
                "Field 'roomId' is required. A sensor must be assigned to a valid room.");
        }
        Room room = store.getRoomById(roomId);
        if (room == null) {
            throw new LinkedResourceNotFoundException(
                "Referenced roomId '" + roomId + "' does not exist. " +
                "Please create the room first before registering sensors in it.");
        }

        // Default status to ACTIVE if not provided
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }

        // Persist sensor and update the room's sensorIds list
        store.addSensor(sensor);
        room.getSensorIds().add(sensor.getId());

        LOGGER.info("Registered new sensor: " + sensor.getId() + " in room: " + roomId);
        URI location = URI.create("/api/v1/sensors/" + sensor.getId());
        return Response.created(location).entity(sensor).build();
    }

    /**
     * DELETE /api/v1/sensors/{sensorId}
     * Removes a sensor and unlinks it from its room.
     */
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensorById(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("error", "Not Found",
                                          "message", "Sensor '" + sensorId + "' does not exist."))
                           .build();
        }
        // Unlink from room
        Room room = store.getRoomById(sensor.getRoomId());
        if (room != null) {
            room.getSensorIds().remove(sensorId);
        }
        store.deleteSensor(sensorId);
        LOGGER.info("Deleted sensor: " + sensorId);
        return Response.noContent().build();
    }

    /**
     * Part 4.1 — Sub-Resource Locator Pattern.
     *
     * This method does NOT handle an HTTP request itself — it delegates to
     * SensorReadingResource which manages the /readings sub-collection.
     *
     * Benefits of the Sub-Resource Locator pattern:
     *  - Separation of concerns: reading logic lives in its own class
     *  - Reduced complexity: each class stays focused and manageable
     *  - Reusability: SensorReadingResource could be reused if needed
     *  - Testability: each resource class can be tested independently
     *  - In large APIs, a single "mega-controller" becomes unmanageable;
     *    delegation mirrors how object-oriented design handles complexity
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getSensorReadings(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}

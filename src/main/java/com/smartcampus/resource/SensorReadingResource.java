package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.util.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Part 4 — SensorReadingResource (Sub-Resource).
 *
 * This class is invoked via the sub-resource locator in SensorResource:
 *   GET  /api/v1/sensors/{sensorId}/readings  → getAllReadings()
 *   POST /api/v1/sensors/{sensorId}/readings  → addReading()
 *
 * It is NOT annotated with @Path at class level — its path context is
 * established by the locator method in SensorResource.
 *
 * Part 4.2 — Side Effect:
 * A successful POST triggers an update to the parent Sensor's currentValue
 * field to ensure data consistency across the API.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private static final Logger LOGGER = Logger.getLogger(SensorReadingResource.class.getName());
    private final DataStore store = DataStore.getInstance();
    private final String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    /**
     * GET /api/v1/sensors/{sensorId}/readings
     * Returns the full historical reading log for the specified sensor.
     */
    @GET
    public Response getAllReadings() {
        Sensor sensor = store.getSensorById(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("error", "Not Found",
                                          "message", "Sensor '" + sensorId + "' does not exist."))
                           .build();
        }

        List<SensorReading> readings = store.getReadingsForSensor(sensorId);
        LOGGER.info("Fetching readings for sensor: " + sensorId + ". Count: " + readings.size());
        return Response.ok(readings).build();
    }

    /**
     * POST /api/v1/sensors/{sensorId}/readings
     *
     * Appends a new reading to the sensor's history.
     *
     * State Constraint (Part 5.3): If the sensor status is MAINTENANCE or OFFLINE,
     * the request is blocked and a SensorUnavailableException is thrown, which maps
     * to HTTP 403 Forbidden. Only ACTIVE sensors can accept new readings.
     *
     * Side Effect: After saving, the parent Sensor's currentValue is updated to
     * reflect the latest reading, ensuring data consistency.
     */
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensorById(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("error", "Not Found",
                                          "message", "Sensor '" + sensorId + "' does not exist."))
                           .build();
        }

        // Part 5.3 — State constraint check
        if (!"ACTIVE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId, sensor.getStatus());
        }

        if (reading == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", "Bad Request", "message", "Reading body is required."))
                           .build();
        }

        // Auto-generate ID and timestamp if not provided
        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(java.util.UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Save reading AND update parent sensor's currentValue (side effect)
        store.addReading(sensorId, reading);

        LOGGER.info("New reading added for sensor: " + sensorId +
                    " | value: " + reading.getValue() +
                    " | parent currentValue updated.");

        URI location = URI.create("/api/v1/sensors/" + sensorId + "/readings");
        return Response.created(location).entity(reading).build();
    }
}

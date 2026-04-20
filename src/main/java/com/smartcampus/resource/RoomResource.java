package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.util.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Part 2 — Room Resource.
 *
 * Manages the /api/v1/rooms collection.
 * Supports: GET all, POST create, GET by ID, DELETE with safety check.
 *
 * JAX-RS Lifecycle: By default, a new instance of this class is created per
 * HTTP request. This is why we use the DataStore singleton with ConcurrentHashMap
 * for thread-safe shared state rather than instance fields.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private static final Logger LOGGER = Logger.getLogger(RoomResource.class.getName());
    private final DataStore store = DataStore.getInstance();

    /**
     * GET /api/v1/rooms
     * Returns all rooms. Full objects are returned (not just IDs) to allow
     * clients to render rich lists without additional round-trips.
     *
     * Trade-off note: Returning full objects increases payload size but reduces
     * the number of subsequent GET /{id} calls. For a campus with thousands of
     * rooms, a pagination strategy would be recommended in production.
     */
    @GET
    public Response getAllRooms() {
        Collection<Room> rooms = store.getRooms().values();
        List<Room> roomList = new ArrayList<>(rooms);
        LOGGER.info("Fetching all rooms. Count: " + roomList.size());
        return Response.ok(roomList).build();
    }

    /**
     * POST /api/v1/rooms
     * Creates a new room. Returns 201 Created with a Location header pointing
     * to the newly created resource, following REST best practices.
     */
    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", "Bad Request", "message", "Room ID is required."))
                           .build();
        }
        if (store.getRoomById(room.getId()) != null) {
            return Response.status(Response.Status.CONFLICT)
                           .entity(Map.of("error", "Conflict",
                                          "message", "A room with ID '" + room.getId() + "' already exists."))
                           .build();
        }
        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<>());
        }
        store.addRoom(room);
        LOGGER.info("Created new room: " + room.getId());
        URI location = URI.create("/api/v1/rooms/" + room.getId());
        return Response.created(location).entity(room).build();
    }

    /**
     * GET /api/v1/rooms/{roomId}
     * Returns detailed metadata for a specific room.
     */
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = store.getRoomById(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("error", "Not Found",
                                          "message", "Room with ID '" + roomId + "' does not exist."))
                           .build();
        }
        return Response.ok(room).build();
    }

    /**
     * DELETE /api/v1/rooms/{roomId}
     *
     * Business Logic Constraint: A room cannot be deleted if it has sensors.
     * Throws RoomNotEmptyException (→ 409 Conflict) if sensors are present.
     *
     * Idempotency: DELETE is NOT fully idempotent in this implementation.
     * The first call succeeds (204 No Content). A second identical call returns
     * 404 Not Found because the resource no longer exists. Strictly speaking,
     * idempotent means the *server state* is the same after repeated calls —
     * the resource being absent satisfies that, but the response code differs.
     * This is a pragmatic and common REST design choice.
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoomById(roomId);

        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("error", "Not Found",
                                          "message", "Room with ID '" + roomId + "' does not exist."))
                           .build();
        }

        // Safety logic: block deletion if room has active sensors
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId);
        }

        store.deleteRoom(roomId);
        LOGGER.info("Deleted room: " + roomId);
        return Response.noContent().build(); // 204 No Content
    }
}

package com.smartcampus.exception;

/**
 * Thrown when a referenced resource (e.g., roomId in a Sensor POST)
 * does not exist in the system. Maps to HTTP 422 Unprocessable Entity.
 */
public class LinkedResourceNotFoundException extends RuntimeException {
    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}

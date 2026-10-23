package com.deadlocksimulator.exceptions;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

/** Uniform error body returned by every endpoint. */
public record ApiError(Instant timestamp, int status, String error, String message, String path,
                       List<String> details) {

    public static ApiError of(HttpStatus status, String message, String path, List<String> details) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, details);
    }
}

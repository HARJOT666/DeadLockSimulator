package com.deadlocksimulator.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException e, HttpServletRequest req) {
        List<String> details = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).toList();
        List<String> global = e.getBindingResult().getGlobalErrors().stream()
                .map(g -> g.getObjectName() + ": " + g.getDefaultMessage()).toList();
        return respond(HttpStatus.BAD_REQUEST, "Validation failed", req,
                java.util.stream.Stream.concat(details.stream(), global.stream()).toList());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> constraint(ConstraintViolationException e, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, "Validation failed", req,
                e.getConstraintViolations().stream().map(v -> v.getPropertyPath() + ": " + v.getMessage()).toList());
    }

    @ExceptionHandler(InvalidScenarioException.class)
    ResponseEntity<ApiError> invalidScenario(InvalidScenarioException e, HttpServletRequest req) {
        return respond(HttpStatus.UNPROCESSABLE_ENTITY, "Scenario cannot be executed", req, e.getProblems());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class})
    ResponseEntity<ApiError> badRequest(Exception e, HttpServletRequest req) {
        String msg = e instanceof HttpMessageNotReadableException ? "Malformed JSON request body" : e.getMessage();
        return respond(HttpStatus.BAD_REQUEST, msg, req, List.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> notFound(ResourceNotFoundException e, HttpServletRequest req) {
        return respond(HttpStatus.NOT_FOUND, e.getMessage(), req, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> noRoute(NoResourceFoundException e, HttpServletRequest req) {
        return respond(HttpStatus.NOT_FOUND, "No such endpoint", req, List.of());
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiError> conflict(ConflictException e, HttpServletRequest req) {
        return respond(HttpStatus.CONFLICT, e.getMessage(), req, List.of());
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ApiError> badCredentials(BadCredentialsException e, HttpServletRequest req) {
        return respond(HttpStatus.UNAUTHORIZED, "Invalid email or password", req, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> denied(AccessDeniedException e, HttpServletRequest req) {
        return respond(HttpStatus.FORBIDDEN, "Access denied", req, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception e, HttpServletRequest req) {
        log.error("Unhandled error on {}", req.getRequestURI(), e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", req, List.of());
    }

    private ResponseEntity<ApiError> respond(HttpStatus status, String message, HttpServletRequest req,
                                             List<String> details) {
        return ResponseEntity.status(status).body(ApiError.of(status, message, req.getRequestURI(), details));
    }
}

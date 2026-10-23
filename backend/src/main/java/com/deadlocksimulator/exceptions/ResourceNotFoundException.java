package com.deadlocksimulator.exceptions;

/** Also used for resources owned by another user, so their existence is not revealed. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String what, Object id) {
        super(what + " " + id + " not found");
    }
}

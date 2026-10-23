package com.deadlocksimulator.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/** Converts domain objects to and from the JSONB columns. */
@Component
public class JsonCodec {

    private final ObjectMapper mapper;

    public JsonCodec(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialise " + value.getClass().getSimpleName(), e);
        }
    }

    public <T> T read(String json, Class<T> type) {
        try {
            return json == null ? null : mapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Corrupt stored JSON for " + type.getSimpleName(), e);
        }
    }

    public <T> T read(String json, TypeReference<T> type) {
        try {
            return json == null ? null : mapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Corrupt stored JSON", e);
        }
    }
}

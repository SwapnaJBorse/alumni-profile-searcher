package com.alumnisearch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(String status, int code, String message, List<String> errors, Instant timestamp) {

    public static ApiError of(int code, String message) {
        return new ApiError("error", code, message, List.of(), Instant.now());
    }

    public static ApiError of(int code, String message, List<String> errors) {
        return new ApiError("error", code, message, errors, Instant.now());
    }
}

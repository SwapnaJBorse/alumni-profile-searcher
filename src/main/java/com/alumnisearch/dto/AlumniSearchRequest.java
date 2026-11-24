package com.alumnisearch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Search criteria for alumni profiles")
public record AlumniSearchRequest(
        @Schema(example = "Sandip University")
        @NotBlank(message = "university is required")
        @Size(max = 200, message = "university must be at most 200 characters")
        String university,

        @Schema(example = "Software Engineer")
        @NotBlank(message = "designation is required")
        @Size(max = 200, message = "designation must be at most 200 characters")
        String designation,

        @Schema(example = "2025", description = "Optional graduation year")
        @Min(value = 1950, message = "passoutYear must be 1950 or later")
        @Max(value = 2100, message = "passoutYear must be 2100 or earlier")
        Integer passoutYear) {
}

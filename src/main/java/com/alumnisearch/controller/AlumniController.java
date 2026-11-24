package com.alumnisearch.controller;

import com.alumnisearch.dto.AlumniProfileResponse;
import com.alumnisearch.dto.AlumniSearchRequest;
import com.alumnisearch.dto.ApiError;
import com.alumnisearch.dto.ApiResponse;
import com.alumnisearch.service.AlumniService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alumni")
@Tag(name = "Alumni", description = "Search and retrieve LinkedIn alumni profiles")
public class AlumniController {

    private final AlumniService alumniService;

    public AlumniController(AlumniService alumniService) {
        this.alumniService = alumniService;
    }

    @PostMapping("/search")
    @Operation(summary = "Search alumni profiles",
            description = "Scrapes LinkedIn via PhantomBuster for alumni of the given university and designation, "
                    + "saves them to the database and returns them. This call can take up to a few minutes.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Profiles found and saved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Invalid request (blank field, year out of range, malformed JSON)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502",
                    description = "PhantomBuster failed or returned an unusable result",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503",
                    description = "PhantomBuster is not configured (API key / agent id missing)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "504",
                    description = "PhantomBuster did not finish in time",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ApiResponse<List<AlumniProfileResponse>> search(@Valid @RequestBody AlumniSearchRequest request) {
        return ApiResponse.success(alumniService.searchAndSave(request));
    }

    @GetMapping("/all")
    @Operation(summary = "List all saved alumni profiles")
    public ApiResponse<List<AlumniProfileResponse>> findAll() {
        return ApiResponse.success(alumniService.findAll());
    }
}

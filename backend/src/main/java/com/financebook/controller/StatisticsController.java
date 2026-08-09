package com.financebook.controller;

import com.financebook.dto.response.StatisticsResponse;
import com.financebook.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Statistics controller.
 * Provides aggregated financial statistics for the current user.
 */
@Tag(name = "Statistics", description = "Aggregated statistics endpoints")
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "Get statistics",
            description = "Totals, monthly buckets and per-category buckets, optionally restricted to a date range")
    @GetMapping
    public ResponseEntity<StatisticsResponse> getStatistics(
            @Parameter(description = "Only items on or after this date-time (ISO-8601)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,

            @Parameter(description = "Only items on or before this date-time (ISO-8601)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,

            Authentication authentication) {

        return ResponseEntity.ok(
                statisticsService.getStatistics(dateFrom, dateTo, authentication.getName()));
    }
}

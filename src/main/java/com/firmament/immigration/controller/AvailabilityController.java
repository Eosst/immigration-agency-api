package com.firmament.immigration.controller;

import com.firmament.immigration.dto.request.BlockPeriodRequest;
import com.firmament.immigration.dto.response.BlockedPeriodResponse;
import com.firmament.immigration.dto.response.DayAvailabilityResponse;
import com.firmament.immigration.dto.response.MonthAvailabilityResponse;
import com.firmament.immigration.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
@Tag(name = "Availability", description = "Check consultant availability")
@CrossOrigin(origins = "*")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping("/day/{date}")
    @Operation(summary = "Get available time slots for a specific day")
    public ResponseEntity<DayAvailabilityResponse> getDayAvailability(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(availabilityService.getAvailableTimesForDay(date));
    }

    @GetMapping("/month/{year}/{month}")
    @Operation(summary = "Get month overview showing which days have availability")
    public ResponseEntity<MonthAvailabilityResponse> getMonthAvailability(
            @PathVariable int year,
            @PathVariable int month) {
        return ResponseEntity.ok(availabilityService.getMonthAvailability(year, month));
    }

    @PostMapping("/block")
    @Operation(summary = "Block a time period (Admin only)")
    public ResponseEntity<Void> blockPeriod(@Valid @RequestBody BlockPeriodRequest request) {  // <-- Make sure @RequestBody is here
        availabilityService.blockPeriod(request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/block/{id}")
    @Operation(summary = "Unblock a time period (Admin only)")
    public ResponseEntity<Void> unblockPeriod(@PathVariable String id) {
        availabilityService.unblockPeriod(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/blocked-periods")
    @Operation(summary = "Get all blocked periods (Admin only)")
    public ResponseEntity<List<BlockedPeriodResponse>> getBlockedPeriods(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(availabilityService.getBlockedPeriods(startDate, endDate));
    }
}
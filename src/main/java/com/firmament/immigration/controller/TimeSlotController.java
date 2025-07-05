package com.firmament.immigration.controller;

import com.firmament.immigration.dto.request.GenerateTimeSlotsRequest;
import com.firmament.immigration.dto.response.TimeSlotResponse;
import com.firmament.immigration.service.TimeSlotService;
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
@RequestMapping("/api/timeslots")
@RequiredArgsConstructor
@Tag(name = "Time Slots", description = "Time slot management")
@CrossOrigin(origins = "*")
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    @GetMapping("/available")
    @Operation(summary = "Get available time slots for a specific date")
    public ResponseEntity<List<TimeSlotResponse>> getAvailableSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<TimeSlotResponse> slots = timeSlotService.getAvailableSlots(date);
        return ResponseEntity.ok(slots);
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate time slots for date range", description = "Admin only")
    public ResponseEntity<Void> generateTimeSlots(@Valid @RequestBody GenerateTimeSlotsRequest request) {
        // TODO: Add admin auth check
        timeSlotService.generateTimeSlots(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/block")
    @Operation(summary = "Block a time slot", description = "Admin only")
    public ResponseEntity<Void> blockTimeSlot(@PathVariable String id) {
        // TODO: Add admin auth check
        timeSlotService.blockTimeSlot(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/unblock")
    @Operation(summary = "Unblock a time slot", description = "Admin only")
    public ResponseEntity<Void> unblockTimeSlot(@PathVariable String id) {
        // TODO: Add admin auth check
        timeSlotService.unblockTimeSlot(id);
        return ResponseEntity.ok().build();
    }
}
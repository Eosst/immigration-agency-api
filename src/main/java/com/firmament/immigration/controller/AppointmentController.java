package com.firmament.immigration.controller;

import com.firmament.immigration.dto.request.CreateAppointmentRequest;
import com.firmament.immigration.dto.request.UpdateAppointmentRequest;
import com.firmament.immigration.dto.response.AppointmentResponse;
import com.firmament.immigration.entity.AppointmentStatus;
import com.firmament.immigration.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Appointment management endpoints")
@CrossOrigin(origins = "*")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @Operation(summary = "Create new appointment", description = "Create a new appointment booking")
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request) {
        AppointmentResponse response = appointmentService.createAppointment(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment by ID")
    public ResponseEntity<AppointmentResponse> getAppointment(@PathVariable String id) {
        AppointmentResponse response = appointmentService.getAppointmentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming appointments", description = "Admin only - Get all upcoming confirmed appointments")
    public ResponseEntity<List<AppointmentResponse>> getUpcomingAppointments() {
        List<AppointmentResponse> appointments = appointmentService.getUpcomingAppointments();
        return ResponseEntity.ok(appointments);
    }

    @GetMapping
    @Operation(summary = "Get all appointments with optional status filter", description = "Admin only - Get all appointments, optionally filtered by status")
    public ResponseEntity<List<AppointmentResponse>> getAllAppointments(
            @RequestParam(required = false) AppointmentStatus status) {
        List<AppointmentResponse> appointments;
        if (status != null) {
            appointments = appointmentService.getAppointmentsByStatus(status);
        } else {
            appointments = appointmentService.getAllAppointments();
        }
        return ResponseEntity.ok(appointments);
    }

    @PostMapping("/{id}/confirm-payment")
    @Operation(summary = "Confirm payment for appointment")
    public ResponseEntity<AppointmentResponse> confirmPayment(
            @PathVariable String id,
            @RequestParam String paymentIntentId) {
        AppointmentResponse response = appointmentService.confirmPayment(id, paymentIntentId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel appointment")
    public ResponseEntity<Void> cancelAppointment(@PathVariable String id) {
        appointmentService.cancelAppointment(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update appointment details", description = "Admin only - Update specific fields of an appointment")
    public ResponseEntity<AppointmentResponse> updateAppointment(
            @PathVariable String id,
            @RequestBody UpdateAppointmentRequest request) {
        AppointmentResponse response = appointmentService.updateAppointment(id, request);
        return ResponseEntity.ok(response);
    }
}
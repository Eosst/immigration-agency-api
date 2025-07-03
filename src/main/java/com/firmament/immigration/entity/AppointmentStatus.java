package com.firmament.immigration.entity;

public enum AppointmentStatus {
    PENDING,        // Just created, awaiting payment
    CONFIRMED,      // Payment successful
    CANCELLED,      // Cancelled by client or admin
    COMPLETED,      // Appointment finished
    NO_SHOW         // Client didn't show up
}

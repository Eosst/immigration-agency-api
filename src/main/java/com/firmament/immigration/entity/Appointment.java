package com.firmament.immigration.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
@Entity
@Table(name = "appointments", indexes = {
        @Index(name = "idx_appointment_date", columnList = "appointmentDate"),
        @Index(name = "idx_email_status", columnList = "email,status"),
        @Index(name = "idx_status", columnList = "status")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment extends BaseEntity {

    // Client Information (no account needed!)
    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String country;

    // Appointment Details
    @Column(nullable = false)
    private ZonedDateTime appointmentDate;

    @Column(nullable = false)
    private Integer duration; // in minutes (30, 60, 90)

    @Column(nullable = false)
    private String consultationType;

    @Column(length = 1000)
    private String clientPresentation; // Their 500-word description

    @Column(nullable = false)
    private String userTimezone;

    // Payment Information
    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency; // CAD or MAD

    @Column(unique = true)
    private String paymentIntentId; // Stripe payment ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.PENDING;

    // For admin notes
    @Column(length = 1000)
    private String adminNotes;


    @Column(name = "reminder_sent")
    private Boolean reminderSent = false;

    @Column(name = "reminder_sent_at")
    private ZonedDateTime reminderSentAt;
}
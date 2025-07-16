package com.firmament.immigration.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "blocked_periods")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedPeriod extends BaseEntity {

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private String reason; // "APPOINTMENT", "VACATION", "MEETING", etc.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment; // null if blocked by admin

    @Column(length = 500)
    private String notes; // Admin notes for manual blocks
}
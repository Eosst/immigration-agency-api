package com.firmament.immigration.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "blocked_periods", indexes = {
        @Index(name = "idx_date", columnList = "date"),
        @Index(name = "idx_datetime", columnList = "startDateTime,endDateTime")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedPeriod extends BaseEntity {

    @Column(nullable = false)
    private LocalDate date; // Keep for indexing/querying
    
    // NEW: Store as UTC timestamps
    @Column(nullable = false)
    private ZonedDateTime startDateTime;
    
    @Column(nullable = false)
    private ZonedDateTime endDateTime;
    
    // Store the original timezone for reference
    @Column
    private String originalTimezone;
    
    // Deprecated - will be computed from startDateTime/endDateTime
    @Transient
    private LocalTime startTime;
    
    @Transient
    private LocalTime endTime;

    @Column(nullable = false)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(length = 500)
    private String notes;
    
    // Helper methods
    public LocalTime getStartTimeInZone(String timezone) {
        return startDateTime.withZoneSameInstant(java.time.ZoneId.of(timezone)).toLocalTime();
    }
    
    public LocalTime getEndTimeInZone(String timezone) {
        return endDateTime.withZoneSameInstant(java.time.ZoneId.of(timezone)).toLocalTime();
    }
    public LocalTime getStartTime() {
        if (startDateTime != null) {
            return startDateTime.toLocalTime();
        }
        return startTime; // fallback to transient field
    }

    public LocalTime getEndTime() {
        if (endDateTime != null) {
            return endDateTime.toLocalTime();
        }
        return endTime; // fallback to transient field
    }
}
package com.firmament.immigration.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
public class BlockPeriodRequest {
    @NotNull(message = "Date is required")
    @FutureOrPresent(message = "Date must be today or in the future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    // For date range blocking
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate; // Optional - if provided, blocks from date to endDate

    // For full day blocking
    private boolean fullDay = false;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    @NotBlank(message = "Reason is required")
    private String reason; // VACATION, MEETING, PERSONAL, etc.

    private String notes;
    
    // NEW: Add timezone field
    private String timezone = "UTC"; // Default to UTC if not provided

    // Custom validation
    @AssertTrue(message = "Either fullDay must be true OR both startTime and endTime must be provided")
    private boolean isValidTimeConfiguration() {
        return fullDay || (startTime != null && endTime != null);
    }

    @AssertTrue(message = "End date must be after or equal to start date")
    private boolean isValidDateRange() {
        if (endDate == null) return true; // Single day is always valid
        return !endDate.isBefore(date);
    }
}
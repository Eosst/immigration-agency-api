// BlockPeriodRequest.java
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

    @NotNull(message = "Start time is required")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    @NotBlank(message = "Reason is required")
    private String reason; // VACATION, MEETING, PERSONAL, etc.

    private String notes;
}
// GenerateTimeSlotsRequest.java
package com.firmament.immigration.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class GenerateTimeSlotsRequest {

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or in the future")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Min(30)
    @Max(90)
    private int slotDuration = 30; // Default 30 minutes

    @AssertTrue(message = "End date must be after start date")
    public boolean isEndDateValid() {
        return endDate == null || startDate == null || !endDate.isBefore(startDate);
    }
}
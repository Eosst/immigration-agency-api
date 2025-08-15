package com.firmament.immigration.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class DayAvailabilityResponse {
    private LocalDate date;
    private boolean fullyBooked;
    private List<TimeSlotDto> availableSlots;
    private String timezone; // ADD THIS FIELD
}
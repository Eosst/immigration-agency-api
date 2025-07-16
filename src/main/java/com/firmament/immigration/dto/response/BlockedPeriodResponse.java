package com.firmament.immigration.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class BlockedPeriodResponse {
    private String id;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String reason;
    private String notes;
    private String appointmentId;
}
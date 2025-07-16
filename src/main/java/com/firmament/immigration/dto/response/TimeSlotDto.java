package com.firmament.immigration.dto.response;

import lombok.Data;

import java.time.LocalTime;

@Data
public class TimeSlotDto {
    private LocalTime startTime;
    private boolean available30Min;
    private boolean available60Min;
    private boolean available90Min;
}
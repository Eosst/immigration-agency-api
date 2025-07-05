// TimeSlotResponse.java
package com.firmament.immigration.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TimeSlotResponse {
    private String id;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean available;
}
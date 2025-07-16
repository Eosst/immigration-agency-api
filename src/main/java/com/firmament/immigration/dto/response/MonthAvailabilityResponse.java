package com.firmament.immigration.dto.response;

import lombok.Data;
import java.util.Map;

@Data
public class MonthAvailabilityResponse {
    private int year;
    private int month;
    private Map<Integer, Boolean> dayAvailability; // day -> isAvailable
}
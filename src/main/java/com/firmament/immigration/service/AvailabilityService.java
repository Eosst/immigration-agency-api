package com.firmament.immigration.service;

import com.firmament.immigration.dto.request.BlockPeriodRequest;
import com.firmament.immigration.dto.response.BlockedPeriodResponse;
import com.firmament.immigration.dto.response.DayAvailabilityResponse;
import com.firmament.immigration.dto.response.MonthAvailabilityResponse;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

public interface AvailabilityService {
    boolean isAvailable(ZonedDateTime startDateTime, int durationInMinutes);
    
    // Overloaded methods for timezone support
    DayAvailabilityResponse getAvailableTimesForDay(LocalDate date);
    DayAvailabilityResponse getAvailableTimesForDay(LocalDate date, String timezone);
    
    MonthAvailabilityResponse getMonthAvailability(int year, int month);
    MonthAvailabilityResponse getMonthAvailability(int year, int month, String timezone);
    
    void blockPeriod(BlockPeriodRequest request);
    void unblockPeriod(String blockedPeriodId);
    void blockTimeForAppointment(String appointmentId, ZonedDateTime startTime, int duration);
    List<BlockedPeriodResponse> getBlockedPeriods(LocalDate startDate, LocalDate endDate);
    void freeUpBlockedTimeForAppointment(String appointmentId);
}

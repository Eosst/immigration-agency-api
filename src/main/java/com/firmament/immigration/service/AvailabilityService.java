package com.firmament.immigration.service;

import com.firmament.immigration.dto.request.BlockPeriodRequest;

import com.firmament.immigration.dto.response.BlockedPeriodResponse;
import com.firmament.immigration.dto.response.DayAvailabilityResponse;
import com.firmament.immigration.dto.response.MonthAvailabilityResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AvailabilityService {
    // Check if a specific time slot is available
    boolean isAvailable(LocalDateTime startDateTime, int durationInMinutes);

    // Get available times for a specific day
    DayAvailabilityResponse getAvailableTimesForDay(LocalDate date);

    // Get month overview (which days have availability)
    MonthAvailabilityResponse getMonthAvailability(int year, int month);

    // Admin: block a time period
    void blockPeriod(BlockPeriodRequest request);

    // Admin: unblock a period
    void unblockPeriod(String blockedPeriodId);

    // Automatically block time when appointment is created
    void blockTimeForAppointment(String appointmentId, LocalDateTime startTime, int duration);

    // Get blocked periods in a date range
    List<BlockedPeriodResponse> getBlockedPeriods(LocalDate startDate, LocalDate endDate);

}
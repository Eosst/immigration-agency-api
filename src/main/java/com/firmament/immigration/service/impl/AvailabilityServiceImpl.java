// In AvailabilityServiceImpl.java

package com.firmament.immigration.service.impl;

import com.firmament.immigration.dto.request.BlockPeriodRequest;
import com.firmament.immigration.dto.response.BlockedPeriodResponse;
import com.firmament.immigration.dto.response.DayAvailabilityResponse;
import com.firmament.immigration.dto.response.MonthAvailabilityResponse;
import com.firmament.immigration.dto.response.TimeSlotDto;
import com.firmament.immigration.entity.Appointment;
import com.firmament.immigration.entity.BlockedPeriod;
import com.firmament.immigration.exception.BusinessException;
import com.firmament.immigration.exception.ResourceNotFoundException;
import com.firmament.immigration.repository.AppointmentRepository;
import com.firmament.immigration.repository.BlockedPeriodRepository;
import com.firmament.immigration.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AvailabilityServiceImpl implements AvailabilityService {

    private final BlockedPeriodRepository blockedPeriodRepository;
    private final AppointmentRepository appointmentRepository;
    private final ModelMapper modelMapper;

    @Override
    public boolean isAvailable(ZonedDateTime startDateTime, int durationInMinutes) {
        if (startDateTime.isBefore(ZonedDateTime.now())) {
            return false;
        }
        LocalDate date = startDateTime.toLocalDate();
        LocalTime startTime = startDateTime.toLocalTime();
        LocalTime endTime = startTime.plusMinutes(durationInMinutes);

        // Check if it's blocked
        return !blockedPeriodRepository.isTimeBlocked(date, startTime, endTime);
    }

    @Override
    public DayAvailabilityResponse getAvailableTimesForDay(LocalDate date) {
        DayAvailabilityResponse response = new DayAvailabilityResponse();
        response.setDate(date);

        // Fetch all blocked periods for the day ONCE
        List<BlockedPeriod> blockedPeriods = blockedPeriodRepository.findByDate(date);

        // Log blocked periods for debugging
        log.debug("Blocked periods for {}: {}", date,
                blockedPeriods.stream()
                        .map(bp -> bp.getStartTime() + "-" + bp.getEndTime())
                        .collect(Collectors.joining(", ")));

        List<TimeSlotDto> availableSlots = new ArrayList<>();

        // Generate time slots for the entire day (00:00 to 23:30 in 30-minute increments)
        LocalTime currentTime = LocalTime.of(0, 0);

        // Use a counter to prevent infinite loops
        int slotCount = 0;
        final int MAX_SLOTS = 48; // 24 hours * 2 slots per hour

        while (slotCount < MAX_SLOTS) {
            TimeSlotDto slot = new TimeSlotDto();
            slot.setStartTime(currentTime);

            // Check availability for each duration
            slot.setAvailable30Min(isSlotFree(currentTime, 30, blockedPeriods));
            slot.setAvailable60Min(isSlotFree(currentTime, 60, blockedPeriods));
            slot.setAvailable90Min(isSlotFree(currentTime, 90, blockedPeriods));

            availableSlots.add(slot);
            currentTime = currentTime.plusMinutes(30);
            slotCount++;
        }

        response.setAvailableSlots(availableSlots);

        // A day is fully booked if no slot has any availability
        boolean isFullyBooked = availableSlots.stream()
                .noneMatch(s -> s.isAvailable30Min() || s.isAvailable60Min() || s.isAvailable90Min());
        response.setFullyBooked(isFullyBooked);

        return response;
    }

    private boolean isSlotFree(LocalTime startTime, int duration, List<BlockedPeriod> blockedPeriods) {
        LocalTime endTime = startTime.plusMinutes(duration);

        // Handle the case where duration extends past midnight
        if (endTime.isBefore(startTime)) {
            // For simplicity, don't allow bookings that cross midnight
            return false;
        }

        for (BlockedPeriod blocked : blockedPeriods) {
            // Check for any overlap: (StartA < EndB) and (EndA > StartB)
            if (startTime.isBefore(blocked.getEndTime()) && endTime.isAfter(blocked.getStartTime())) {
                log.debug("Slot {}+{} overlaps with blocked period {}-{}",
                        startTime, duration, blocked.getStartTime(), blocked.getEndTime());
                return false;
            }
        }
        return true;
    }

    @Override
    public MonthAvailabilityResponse getMonthAvailability(int year, int month) {
        MonthAvailabilityResponse response = new MonthAvailabilityResponse();
        response.setYear(year);
        response.setMonth(month);

        Map<Integer, Boolean> dayAvailability = new HashMap<>();
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.plusMonths(1).minusDays(1);
        LocalDate today = LocalDate.now();

        // Get all blocked periods for the month in one query
        List<BlockedPeriod> monthBlockedPeriods = blockedPeriodRepository.findByDateBetween(firstDay, lastDay);

        // Group blocked periods by date
        Map<LocalDate, List<BlockedPeriod>> blockedByDate = monthBlockedPeriods.stream()
                .collect(Collectors.groupingBy(BlockedPeriod::getDate));

        for (LocalDate date = firstDay; !date.isAfter(lastDay); date = date.plusDays(1)) {
            if (date.isBefore(today)) {
                dayAvailability.put(date.getDayOfMonth(), false);
                continue;
            }

            // Check if this day has any availability using a more efficient method
            boolean hasAvailability = hasAnyAvailabilityOptimized(date, blockedByDate.getOrDefault(date, List.of()));
            dayAvailability.put(date.getDayOfMonth(), hasAvailability);
        }
        response.setDayAvailability(dayAvailability);
        return response;
    }

    private boolean hasAnyAvailabilityOptimized(LocalDate date, List<BlockedPeriod> dayBlockedPeriods) {
        // If there are no blocked periods, the day is available
        if (dayBlockedPeriods.isEmpty()) {
            return true;
        }

        // Calculate total blocked minutes
        long totalBlockedMinutes = 0;

        // Sort blocked periods by start time
        List<BlockedPeriod> sortedPeriods = dayBlockedPeriods.stream()
                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .collect(Collectors.toList());

        // Merge overlapping periods and calculate total blocked time
        LocalTime mergedStart = null;
        LocalTime mergedEnd = null;

        for (BlockedPeriod period : sortedPeriods) {
            if (mergedStart == null) {
                mergedStart = period.getStartTime();
                mergedEnd = period.getEndTime();
            } else if (period.getStartTime().isBefore(mergedEnd) || period.getStartTime().equals(mergedEnd)) {
                // Overlapping or adjacent periods - merge them
                if (period.getEndTime().isAfter(mergedEnd)) {
                    mergedEnd = period.getEndTime();
                }
            } else {
                // Non-overlapping period - add the previous merged period to total
                totalBlockedMinutes += java.time.Duration.between(mergedStart, mergedEnd).toMinutes();
                mergedStart = period.getStartTime();
                mergedEnd = period.getEndTime();
            }
        }

        // Add the last merged period
        if (mergedStart != null) {
            totalBlockedMinutes += java.time.Duration.between(mergedStart, mergedEnd).toMinutes();
        }

        // If the entire day is blocked (23.5 hours or more), no availability
        // We use 23.5 hours because we have 48 30-minute slots
        return totalBlockedMinutes < (23.5 * 60);
    }

    @Override
    @Transactional
    public void blockPeriod(BlockPeriodRequest request) {
        // Handle single day blocking
        if (request.getDate() != null && request.getEndDate() == null) {
            if (request.isFullDay()) {
                // Block entire day
                BlockedPeriod blockedPeriod = BlockedPeriod.builder()
                        .date(request.getDate())
                        .startTime(LocalTime.of(0, 0))
                        .endTime(LocalTime.of(23, 59))
                        .reason(request.getReason())
                        .notes(request.getNotes())
                        .build();
                blockedPeriodRepository.save(blockedPeriod);
                log.info("Blocked entire day: {}", request.getDate());
            } else {
                // Block specific time period
                if (request.getStartTime() != null && request.getEndTime() != null &&
                        request.getStartTime().isAfter(request.getEndTime())) {
                    throw new BusinessException("Start time must be before end time");
                }
                BlockedPeriod blockedPeriod = BlockedPeriod.builder()
                        .date(request.getDate())
                        .startTime(request.getStartTime())
                        .endTime(request.getEndTime())
                        .reason(request.getReason())
                        .notes(request.getNotes())
                        .build();
                blockedPeriodRepository.save(blockedPeriod);
                log.info("Blocked period created: {} from {} to {}",
                        request.getDate(), request.getStartTime(), request.getEndTime());
            }
        }
        // Handle date range blocking
        else if (request.getDate() != null && request.getEndDate() != null) {
            if (request.getEndDate().isBefore(request.getDate())) {
                throw new BusinessException("End date must be after start date");
            }

            LocalDate currentDate = request.getDate();
            while (!currentDate.isAfter(request.getEndDate())) {
                BlockedPeriod blockedPeriod = BlockedPeriod.builder()
                        .date(currentDate)
                        .startTime(request.isFullDay() ? LocalTime.of(0, 0) : request.getStartTime())
                        .endTime(request.isFullDay() ? LocalTime.of(23, 59) : request.getEndTime())
                        .reason(request.getReason())
                        .notes(request.getNotes())
                        .build();
                blockedPeriodRepository.save(blockedPeriod);
                currentDate = currentDate.plusDays(1);
            }
            log.info("Blocked period from {} to {}", request.getDate(), request.getEndDate());
        }
    }

    @Override
    @Transactional
    public void unblockPeriod(String blockedPeriodId) {
        BlockedPeriod period = blockedPeriodRepository.findById(blockedPeriodId)
                .orElseThrow(() -> new ResourceNotFoundException("Blocked period not found"));
        if (period.getAppointment() != null) {
            throw new BusinessException("Cannot unblock period associated with an appointment");
        }
        blockedPeriodRepository.delete(period);
        log.info("Unblocked period: {}", blockedPeriodId);
    }

    @Override
    @Transactional
    public void blockTimeForAppointment(String appointmentId, ZonedDateTime startTime, int duration) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        LocalTime localStartTime = startTime.toLocalTime();
        LocalTime localEndTime = localStartTime.plusMinutes(duration);

        BlockedPeriod blockedPeriod = BlockedPeriod.builder()
                .date(startTime.toLocalDate())
                .startTime(localStartTime)
                .endTime(localEndTime)
                .reason("APPOINTMENT")
                .appointment(appointment)
                .build();

        blockedPeriodRepository.save(blockedPeriod);
        log.info("Blocked time for appointment {}: {} to {} on {}",
                appointmentId, localStartTime, localEndTime, startTime.toLocalDate());
    }

    @Override
    public List<BlockedPeriodResponse> getBlockedPeriods(LocalDate startDate, LocalDate endDate) {
        List<BlockedPeriod> blockedPeriods;
        if (startDate != null && endDate != null) {
            blockedPeriods = blockedPeriodRepository.findByDateBetween(startDate, endDate);
        } else if (startDate != null) {
            blockedPeriods = blockedPeriodRepository.findByDateGreaterThanEqual(startDate);
        } else if (endDate != null) {
            blockedPeriods = blockedPeriodRepository.findByDateLessThanEqual(endDate);
        } else {
            blockedPeriods = blockedPeriodRepository.findAll();
        }
        return blockedPeriods.stream()
                .map(this::mapToBlockedPeriodResponse)
                .collect(Collectors.toList());
    }

    private BlockedPeriodResponse mapToBlockedPeriodResponse(BlockedPeriod blockedPeriod) {
        BlockedPeriodResponse response = modelMapper.map(blockedPeriod, BlockedPeriodResponse.class);
        if (blockedPeriod.getAppointment() != null) {
            response.setAppointmentId(blockedPeriod.getAppointment().getId());
        }
        return response;
    }

    @Override
    @Transactional
    public void freeUpBlockedTimeForAppointment(String appointmentId) {
        blockedPeriodRepository.findAll().stream()
                .filter(bp -> bp.getAppointment() != null &&
                        bp.getAppointment().getId().equals(appointmentId))
                .findFirst()
                .ifPresent(blockedPeriod -> {
                    blockedPeriodRepository.delete(blockedPeriod);
                    log.info("Freed up blocked time for cancelled appointment: {}", appointmentId);
                });
    }
}
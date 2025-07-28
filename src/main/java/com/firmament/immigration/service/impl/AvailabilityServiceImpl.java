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
@Transactional(readOnly = true) // Set to read-only by default for safety
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

        // This check is fine for single-use cases but inefficient in a loop
        return !blockedPeriodRepository.isTimeBlocked(date, startTime, endTime);
    }

    /**
     * Corrected and Optimized Method.
     */
    @Override
    public DayAvailabilityResponse getAvailableTimesForDay(LocalDate date) {
        DayAvailabilityResponse response = new DayAvailabilityResponse();
        response.setDate(date);

        // 1. Fetch all blocked periods for the day ONCE.
        List<BlockedPeriod> blockedPeriods = blockedPeriodRepository.findByDate(date);

        List<TimeSlotDto> availableSlots = new ArrayList<>();
        LocalTime currentTime = LocalTime.MIDNIGHT;
        LocalTime dayEnd = LocalTime.of(23, 59); // Loop until the end of the day

        // 2. Fix the infinite loop by ensuring it terminates correctly.
        while (!currentTime.isAfter(dayEnd)) {
            TimeSlotDto slot = new TimeSlotDto();
            slot.setStartTime(currentTime);

            // 3. Perform availability checks IN-MEMORY instead of hitting the DB.
            slot.setAvailable30Min(isSlotFree(currentTime, 30, blockedPeriods));
            slot.setAvailable60Min(isSlotFree(currentTime, 60, blockedPeriods));
            slot.setAvailable90Min(isSlotFree(currentTime, 90, blockedPeriods));
            availableSlots.add(slot);

            // Break the loop if we're at the last possible slot (23:30) to avoid rollover
            if (currentTime.equals(LocalTime.of(23, 30))) {
                break;
            }
            currentTime = currentTime.plusMinutes(30);
        }

        response.setAvailableSlots(availableSlots);
        // A day is fully booked if no slot has any availability
        boolean isFullyBooked = availableSlots.stream()
                .noneMatch(s -> s.isAvailable30Min() || s.isAvailable60Min() || s.isAvailable90Min());
        response.setFullyBooked(isFullyBooked);

        return response;
    }

    /**
     * Helper method to check availability against a pre-fetched list of blocked periods.
     * This avoids hitting the database in a loop.
     */
    private boolean isSlotFree(LocalTime startTime, int duration, List<BlockedPeriod> blockedPeriods) {
        LocalTime endTime = startTime.plusMinutes(duration);
        // Handle overnight rollover for durations that cross midnight
        if (endTime.isBefore(startTime)) {
            return false; // Cannot book slots that cross midnight
        }

        for (BlockedPeriod blocked : blockedPeriods) {
            // Check for any overlap: (StartA < EndB) and (EndA > StartB)
            if (startTime.isBefore(blocked.getEndTime()) && endTime.isAfter(blocked.getStartTime())) {
                return false; // The slot is blocked
            }
        }
        return true; // The slot is free
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

        for (LocalDate date = firstDay; !date.isAfter(lastDay); date = date.plusDays(1)) {
            if (date.isBefore(today)) {
                dayAvailability.put(date.getDayOfMonth(), false);
                continue;
            }
            boolean fullyBlocked = isFullDayBlocked(date);
            dayAvailability.put(date.getDayOfMonth(), !fullyBlocked);
        }
        response.setDayAvailability(dayAvailability);
        return response;
    }

    private boolean isFullDayBlocked(LocalDate date) {
        List<BlockedPeriod> blockedPeriods = blockedPeriodRepository.findByDate(date);
        if (blockedPeriods.isEmpty()) {
            return false;
        }
        long totalBlockedMinutes = blockedPeriods.stream()
                .mapToLong(period -> java.time.Duration.between(period.getStartTime(), period.getEndTime()).toMinutes())
                .sum();
        return totalBlockedMinutes >= 480; // 8 hours
    }

    @Override
    @Transactional // Override to make this method writable
    public void blockPeriod(BlockPeriodRequest request) {
        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new BusinessException("Start time must be before end time");
        }
        BlockedPeriod blockedPeriod = modelMapper.map(request, BlockedPeriod.class);
        blockedPeriodRepository.save(blockedPeriod);
        log.info("Blocked period created: {} from {} to {}", request.getDate(), request.getStartTime(), request.getEndTime());
    }

    @Override
    @Transactional // Override to make this method writable
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
    @Transactional // Override to make this method writable
    public void blockTimeForAppointment(String appointmentId, ZonedDateTime  startTime, int duration) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        BlockedPeriod blockedPeriod = BlockedPeriod.builder()
                .date(startTime.toLocalDate())
                .startTime(startTime.toLocalTime())
                .endTime(startTime.toLocalTime().plusMinutes(duration))
                .reason("APPOINTMENT")
                .appointment(appointment)
                .build();
        blockedPeriodRepository.save(blockedPeriod);
        log.info("Blocked time for appointment: {}", appointmentId);
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
    @Transactional // Make this method writable
    public void freeUpBlockedTimeForAppointment(String appointmentId) {
        // This is the logic moved from AppointmentServiceImpl
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
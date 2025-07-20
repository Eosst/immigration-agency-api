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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AvailabilityServiceImpl implements AvailabilityService {

    private final BlockedPeriodRepository blockedPeriodRepository;
    private final AppointmentRepository appointmentRepository;
    private final ModelMapper modelMapper;

    // Working hours
    private static final LocalTime WORK_START = LocalTime.of(9, 0);
    private static final LocalTime WORK_END = LocalTime.of(17, 0);
    private static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_END = LocalTime.of(14, 0);

    @Override
    public boolean isAvailable(LocalDateTime startDateTime, int durationInMinutes) {
        if (startDateTime.isBefore(LocalDateTime.now())) {
            return false;
        }
        LocalDate date = startDateTime.toLocalDate();
        LocalTime startTime = startDateTime.toLocalTime();
        LocalTime endTime = startTime.plusMinutes(durationInMinutes);

        // Only check against blocked periods - no automatic restrictions
        return !blockedPeriodRepository.isTimeBlocked(date, startTime, endTime);
    }

    @Override
    public DayAvailabilityResponse getAvailableTimesForDay(LocalDate date) {
        DayAvailabilityResponse response = new DayAvailabilityResponse();
        response.setDate(date);

        // Get all blocked periods for this day
        List<BlockedPeriod> blockedPeriods = blockedPeriodRepository.findByDate(date);

        // Generate available time slots (every 30 minutes) - 24 hours
        List<TimeSlotDto> availableSlots = new ArrayList<>();
        LocalTime currentTime = LocalTime.MIDNIGHT;

        while (currentTime.isBefore(LocalTime.MAX)) {
            boolean isBlocked = false;
            for (BlockedPeriod blocked : blockedPeriods) {
                if (currentTime.compareTo(blocked.getStartTime()) >= 0 &&
                        currentTime.compareTo(blocked.getEndTime()) < 0) {
                    isBlocked = true;
                    break;
                }
            }

            if (!isBlocked) {
                TimeSlotDto slot = new TimeSlotDto();
                slot.setStartTime(currentTime);
                slot.setAvailable30Min(isAvailable(date.atTime(currentTime), 30));
                slot.setAvailable60Min(isAvailable(date.atTime(currentTime), 60));
                slot.setAvailable90Min(isAvailable(date.atTime(currentTime), 90));
                availableSlots.add(slot);
            }

            currentTime = currentTime.plusMinutes(30);
        }

        response.setAvailableSlots(availableSlots);
        response.setFullyBooked(availableSlots.isEmpty());

        return response;
    }

    @Override
    public MonthAvailabilityResponse getMonthAvailability(int year, int month) {
        MonthAvailabilityResponse response = new MonthAvailabilityResponse();
        response.setYear(year);
        response.setMonth(month);

        // Get all dates with blocked periods
        List<LocalDate> datesWithBlocks = blockedPeriodRepository.getDatesWithBlockedPeriods(year, month);

        Map<Integer, Boolean> dayAvailability = new HashMap<>();
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.plusMonths(1).minusDays(1);

        for (LocalDate date = firstDay; !date.isAfter(lastDay); date = date.plusDays(1)) {
            // Check if full day is blocked - no weekend restrictions
            boolean fullyBlocked = blockedPeriodRepository.isFullDayBlocked(date);
            dayAvailability.put(date.getDayOfMonth(), !fullyBlocked);
        }

        response.setDayAvailability(dayAvailability);
        return response;
    }

    @Override
    public void blockPeriod(BlockPeriodRequest request) {
        // Validate the period
        if (request.getStartTime().isAfter(request.getEndTime())) {
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

    @Override
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
    public void blockTimeForAppointment(String appointmentId, LocalDateTime startTime, int duration) {
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
            // Get blocked periods within date range
            blockedPeriods = blockedPeriodRepository.findByDateBetween(startDate, endDate);
        } else if (startDate != null) {
            // Get blocked periods from start date onwards
            blockedPeriods = blockedPeriodRepository.findByDateGreaterThanEqual(startDate);
        } else if (endDate != null) {
            // Get blocked periods up to end date
            blockedPeriods = blockedPeriodRepository.findByDateLessThanEqual(endDate);
        } else {
            // Get all blocked periods
            blockedPeriods = blockedPeriodRepository.findAll();
        }

        return blockedPeriods.stream()
                .map(this::mapToBlockedPeriodResponse)
                .collect(Collectors.toList());
    }

    private BlockedPeriodResponse mapToBlockedPeriodResponse(BlockedPeriod blockedPeriod) {
        BlockedPeriodResponse response = modelMapper.map(blockedPeriod, BlockedPeriodResponse.class);

        // Add appointment ID if linked to an appointment
        if (blockedPeriod.getAppointment() != null) {
            response.setAppointmentId(blockedPeriod.getAppointment().getId());
        }

        return response;
    }


}
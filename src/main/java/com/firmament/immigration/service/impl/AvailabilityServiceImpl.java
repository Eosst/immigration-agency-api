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

import java.time.*;
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

        ZonedDateTime utcStart = startDateTime.withZoneSameInstant(ZoneOffset.UTC);
        ZonedDateTime utcEnd = utcStart.plusMinutes(durationInMinutes);

        return !blockedPeriodRepository.isTimeBlockedUTC(utcStart, utcEnd);
    }

    @Override
    public DayAvailabilityResponse getAvailableTimesForDay(LocalDate date) {
        return getAvailableTimesForDay(date, "UTC");
    }

    @Override
    public DayAvailabilityResponse getAvailableTimesForDay(LocalDate date, String timezone) {
        DayAvailabilityResponse response = new DayAvailabilityResponse();
        response.setDate(date);
        response.setTimezone(timezone);

        ZoneId zoneId = ZoneId.of(timezone);

        // Define the start and end of the day in the user's timezone, then convert to UTC to fetch relevant records
        ZonedDateTime dayStart = date.atStartOfDay(zoneId);
        ZonedDateTime dayEnd = dayStart.plusDays(1);

        List<BlockedPeriod> blockedPeriodsUtc = blockedPeriodRepository.findByDateTimeBetween(dayStart.withZoneSameInstant(ZoneOffset.UTC), dayEnd.withZoneSameInstant(ZoneOffset.UTC));

        // Convert fetched UTC periods to local time ranges for comparison
        List<LocalTime[]> blockedLocalRanges = blockedPeriodsUtc.stream()
                .map(bp -> new LocalTime[]{
                        bp.getStartDateTime().withZoneSameInstant(zoneId).toLocalTime(),
                        bp.getEndDateTime().withZoneSameInstant(zoneId).toLocalTime()
                })
                .collect(Collectors.toList());

        List<TimeSlotDto> availableSlots = new ArrayList<>();

        // Generate potential time slots for the day
        LocalTime currentTime = LocalTime.of(0, 0);
        while (currentTime.isBefore(LocalTime.of(23, 30))) {
            TimeSlotDto slot = new TimeSlotDto();
            slot.setStartTime(currentTime);

            slot.setAvailable30Min(isSlotFree(currentTime, 30, blockedLocalRanges));
            slot.setAvailable60Min(isSlotFree(currentTime, 60, blockedLocalRanges));
            slot.setAvailable90Min(isSlotFree(currentTime, 90, blockedLocalRanges));

            availableSlots.add(slot);
            currentTime = currentTime.plusMinutes(30);
        }

        response.setAvailableSlots(availableSlots);

        boolean isFullyBooked = availableSlots.stream()
                .noneMatch(s -> s.isAvailable30Min() || s.isAvailable60Min() || s.isAvailable90Min());
        response.setFullyBooked(isFullyBooked);

        return response;
    }

    private boolean isSlotFree(LocalTime startTime, int duration, List<LocalTime[]> blockedRanges) {
        LocalTime endTime = startTime.plusMinutes(duration);

        if (endTime.isBefore(startTime)) { // Handles overnight slots if necessary
            return false;
        }

        for (LocalTime[] blocked : blockedRanges) {
            LocalTime blockedStart = blocked[0];
            LocalTime blockedEnd = blocked[1];
            // Check for overlap: (StartA < EndB) and (EndA > StartB)
            if (startTime.isBefore(blockedEnd) && endTime.isAfter(blockedStart)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public MonthAvailabilityResponse getMonthAvailability(int year, int month) {
        // Default implementation without timezone
        return getMonthAvailability(year, month, "UTC");
    }
    
    public MonthAvailabilityResponse getMonthAvailability(int year, int month, String timezone) {
        MonthAvailabilityResponse response = new MonthAvailabilityResponse();
        response.setYear(year);
        response.setMonth(month);
        response.setTimezone(timezone); // Set the timezone in response
        
        Map<Integer, Boolean> dayAvailability = new HashMap<>();
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.plusMonths(1).minusDays(1);
        LocalDate today = LocalDate.now();
        
        List<BlockedPeriod> monthBlockedPeriods = blockedPeriodRepository.findByDateBetween(firstDay, lastDay);
        
        Map<LocalDate, List<BlockedPeriod>> blockedByDate = monthBlockedPeriods.stream()
                .collect(Collectors.groupingBy(BlockedPeriod::getDate));
        
        for (LocalDate date = firstDay; !date.isAfter(lastDay); date = date.plusDays(1)) {
            if (date.isBefore(today)) {
                dayAvailability.put(date.getDayOfMonth(), false);
                continue;
            }
            
            boolean hasAvailability = hasAnyAvailabilityOptimized(date, blockedByDate.getOrDefault(date, List.of()));
            dayAvailability.put(date.getDayOfMonth(), hasAvailability);
        }
        response.setDayAvailability(dayAvailability);
        return response;
    }

    private boolean hasAnyAvailabilityOptimized(LocalDate date, List<BlockedPeriod> dayBlockedPeriods) {
        if (dayBlockedPeriods.isEmpty()) {
            return true;
        }
        
        long totalBlockedMinutes = 0;
        
        List<BlockedPeriod> sortedPeriods = dayBlockedPeriods.stream()
                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .collect(Collectors.toList());
        
        LocalTime mergedStart = null;
        LocalTime mergedEnd = null;
        
        for (BlockedPeriod period : sortedPeriods) {
            if (mergedStart == null) {
                mergedStart = period.getStartTime();
                mergedEnd = period.getEndTime();
            } else if (period.getStartTime().isBefore(mergedEnd) || period.getStartTime().equals(mergedEnd)) {
                if (period.getEndTime().isAfter(mergedEnd)) {
                    mergedEnd = period.getEndTime();
                }
            } else {
                totalBlockedMinutes += java.time.Duration.between(mergedStart, mergedEnd).toMinutes();
                mergedStart = period.getStartTime();
                mergedEnd = period.getEndTime();
            }
        }
        
        if (mergedStart != null) {
            totalBlockedMinutes += java.time.Duration.between(mergedStart, mergedEnd).toMinutes();
        }
        
        return totalBlockedMinutes < (23.5 * 60);
    }

    @Override
    @Transactional
    public void blockPeriod(BlockPeriodRequest request) {
        // Validate timezone if provided
        String timezone = request.getTimezone() != null ? request.getTimezone() : "UTC";
        try {
            ZoneId.of(timezone); // Validate timezone
        } catch (Exception e) {
            throw new BusinessException("Invalid timezone: " + timezone);
        }
        
        // Handle single day blocking
        if (request.getDate() != null && request.getEndDate() == null) {
            createSingleDayBlock(request, timezone);
        }
        // Handle date range blocking
        else if (request.getDate() != null && request.getEndDate() != null) {
            createDateRangeBlock(request, timezone);
        } else {
            throw new BusinessException("Invalid block period request");
        }
    }
    
    private void createSingleDayBlock(BlockPeriodRequest request, String timezone) {
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
            log.info("Blocked entire day: {} in timezone: {}", request.getDate(), timezone);
        } else {
            // Block specific time period
            if (request.getStartTime() == null || request.getEndTime() == null) {
                throw new BusinessException("Start time and end time are required for time-specific blocking");
            }
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
            log.info("Blocked period created: {} from {} to {} in timezone: {}",
                    request.getDate(), request.getStartTime(), request.getEndTime(), timezone);
        }
    }
    
    private void createDateRangeBlock(BlockPeriodRequest request, String timezone) {
        if (request.getEndDate().isBefore(request.getDate())) {
            throw new BusinessException("End date must be after start date");
        }
        
        // Calculate the number of days to block
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
            request.getDate(), 
            request.getEndDate()
        ) + 1; // +1 to include both start and end dates
        
        if (daysBetween > 365) {
            throw new BusinessException("Cannot block more than 365 days at once");
        }
        
        List<BlockedPeriod> periodsToSave = new ArrayList<>();
        LocalDate currentDate = request.getDate();
        
        while (!currentDate.isAfter(request.getEndDate())) {
            BlockedPeriod blockedPeriod = BlockedPeriod.builder()
                    .date(currentDate)
                    .startTime(request.isFullDay() ? LocalTime.of(0, 0) : request.getStartTime())
                    .endTime(request.isFullDay() ? LocalTime.of(23, 59) : request.getEndTime())
                    .reason(request.getReason())
                    .notes(request.getNotes())
                    .build();
            
            periodsToSave.add(blockedPeriod);
            currentDate = currentDate.plusDays(1);
        }
        
        // Save all periods in batch
        blockedPeriodRepository.saveAll(periodsToSave);
        
        log.info("Blocked date range from {} to {} ({} days) in timezone: {}", 
                request.getDate(), request.getEndDate(), daysBetween, timezone);
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

        // Convert the user's provided start time to UTC for storage
        ZonedDateTime utcStartTime = startTime.withZoneSameInstant(ZoneOffset.UTC);

        // Calculate the end time by adding the duration
        ZonedDateTime utcEndTime = utcStartTime.plusMinutes(duration);

        BlockedPeriod blockedPeriod = BlockedPeriod.builder()
                .date(utcStartTime.toLocalDate()) // Keep the date for simpler queries
                .startDateTime(utcStartTime)      // Set the persistent startDateTime field
                .endDateTime(utcEndTime)          // Set the persistent endDateTime field
                .originalTimezone(startTime.getZone().getId()) // Store original timezone for context
                .reason("APPOINTMENT")
                .appointment(appointment)
                .build();

        blockedPeriodRepository.save(blockedPeriod);
        log.info("Blocked time for appointment {}: from {} to {} (UTC)",
                appointmentId, utcStartTime, utcEndTime);
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
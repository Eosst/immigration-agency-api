package com.firmament.immigration.service.impl;

import com.firmament.immigration.dto.request.GenerateTimeSlotsRequest;
import com.firmament.immigration.dto.response.TimeSlotResponse;
import com.firmament.immigration.entity.TimeSlot;
import com.firmament.immigration.exception.BusinessException;
import com.firmament.immigration.repository.TimeSlotRepository;
import com.firmament.immigration.service.TimeSlotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TimeSlotServiceImpl implements TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final ModelMapper modelMapper;

    // Default working hours
    private static final LocalTime MORNING_START = LocalTime.of(9, 0);
    private static final LocalTime MORNING_END = LocalTime.of(12, 0);
    private static final LocalTime AFTERNOON_START = LocalTime.of(14, 0);
    private static final LocalTime AFTERNOON_END = LocalTime.of(17, 0);

    @Override
    public void generateTimeSlots(GenerateTimeSlotsRequest request) {
        log.info("Generating time slots from {} to {}", request.getStartDate(), request.getEndDate());

        LocalDate currentDate = request.getStartDate();

        while (!currentDate.isAfter(request.getEndDate())) {
            // Skip weekends (Saturday = 6, Sunday = 7)
            if (currentDate.getDayOfWeek().getValue() < 6) {
                generateSlotsForDay(currentDate, request.getSlotDuration());
            }
            currentDate = currentDate.plusDays(1);
        }
    }

    private void generateSlotsForDay(LocalDate date, int duration) {
        List<TimeSlot> slots = new ArrayList<>();

        // Morning slots
        LocalTime currentTime = MORNING_START;
        while (currentTime.plusMinutes(duration).isBefore(MORNING_END.plusMinutes(1))) {
            if (!timeSlotRepository.existsByDateAndStartTime(date, currentTime)) {
                TimeSlot slot = TimeSlot.builder()
                        .date(date)
                        .startTime(currentTime)
                        .endTime(currentTime.plusMinutes(duration))
                        .available(true)
                        .build();
                slots.add(slot);
            }
            currentTime = currentTime.plusMinutes(duration);
        }

        // Afternoon slots
        currentTime = AFTERNOON_START;
        while (currentTime.plusMinutes(duration).isBefore(AFTERNOON_END.plusMinutes(1))) {
            if (!timeSlotRepository.existsByDateAndStartTime(date, currentTime)) {
                TimeSlot slot = TimeSlot.builder()
                        .date(date)
                        .startTime(currentTime)
                        .endTime(currentTime.plusMinutes(duration))
                        .available(true)
                        .build();
                slots.add(slot);
            }
            currentTime = currentTime.plusMinutes(duration);
        }

        if (!slots.isEmpty()) {
            timeSlotRepository.saveAll(slots);
            log.info("Generated {} slots for {}", slots.size(), date);
        }
    }

    @Override
    public List<TimeSlotResponse> getAvailableSlots(LocalDate date) {
        // Generate slots for next 30 days if none exist
        if (!timeSlotRepository.existsByDateAndStartTime(date, MORNING_START)) {
            GenerateTimeSlotsRequest request = new GenerateTimeSlotsRequest();
            request.setStartDate(date);
            request.setEndDate(date.plusDays(30));
            request.setSlotDuration(30); // Default 30 min slots
            generateTimeSlots(request);
        }

        List<TimeSlot> availableSlots = timeSlotRepository.findByDateAndAvailableTrue(date);

        return availableSlots.stream()
                .map(slot -> modelMapper.map(slot, TimeSlotResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public void blockTimeSlot(String slotId) {
        TimeSlot slot = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new BusinessException("Time slot not found"));

        if (!slot.getAvailable()) {
            throw new BusinessException("Time slot is already blocked");
        }

        slot.setAvailable(false);
        timeSlotRepository.save(slot);
    }

    @Override
    public void unblockTimeSlot(String slotId) {
        TimeSlot slot = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new BusinessException("Time slot not found"));

        slot.setAvailable(true);
        slot.setAppointment(null);
        timeSlotRepository.save(slot);
    }
}
package com.firmament.immigration.service;

import com.firmament.immigration.dto.request.GenerateTimeSlotsRequest;
import com.firmament.immigration.dto.response.TimeSlotResponse;
import java.time.LocalDate;
import java.util.List;

public interface TimeSlotService {
    void generateTimeSlots(GenerateTimeSlotsRequest request);
    List<TimeSlotResponse> getAvailableSlots(LocalDate date);
    void blockTimeSlot(String slotId);
    void unblockTimeSlot(String slotId);
}
package com.firmament.immigration.repository;

import com.firmament.immigration.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, String> {

    // Find available slots for a specific date
    List<TimeSlot> findByDateAndAvailableTrue(LocalDate date);

    // Find all slots for a date
    List<TimeSlot> findByDate(LocalDate date);

    // Check if slot exists
    boolean existsByDateAndStartTime(LocalDate date, java.time.LocalTime startTime);
}
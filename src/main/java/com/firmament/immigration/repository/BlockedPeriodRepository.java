package com.firmament.immigration.repository;

import com.firmament.immigration.entity.BlockedPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface BlockedPeriodRepository extends JpaRepository<BlockedPeriod, String> {

    // Find all blocked periods for a specific date
    List<BlockedPeriod> findByDate(LocalDate date);

    // Find blocked periods for a date range
    List<BlockedPeriod> findByDateBetween(LocalDate startDate, LocalDate endDate);

    // Check if a specific time is blocked
    @Query("SELECT COUNT(b) > 0 FROM BlockedPeriod b WHERE b.date = ?1 AND " +
            "((?2 >= b.startTime AND ?2 < b.endTime) OR " +
            "(?3 > b.startTime AND ?3 <= b.endTime) OR " +
            "(?2 <= b.startTime AND ?3 >= b.endTime))")
    boolean isTimeBlocked(LocalDate date, LocalTime startTime, LocalTime endTime);

    // Get dates that have at least one blocked period in a month
    @Query("SELECT DISTINCT b.date FROM BlockedPeriod b WHERE YEAR(b.date) = ?1 AND MONTH(b.date) = ?2")
    List<LocalDate> getDatesWithBlockedPeriods(int year, int month);

    // Check if entire day is blocked (9am-5pm)
    @Query("SELECT CASE WHEN " +
            "(SELECT SUM(TIMESTAMPDIFF(MINUTE, b.startTime, b.endTime)) FROM BlockedPeriod b WHERE b.date = ?1) >= 480 " +
            "THEN true ELSE false END")
    boolean isFullDayBlocked(LocalDate date);
    List<BlockedPeriod> findByDateGreaterThanEqual(LocalDate startDate);

    // Find blocked periods up to a date
    List<BlockedPeriod> findByDateLessThanEqual(LocalDate endDate);

    // Find blocked periods ordered by date and time
    List<BlockedPeriod> findAllByOrderByDateAscStartTimeAsc();
}
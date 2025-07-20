package com.firmament.immigration.repository;

import com.firmament.immigration.entity.BlockedPeriod;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface BlockedPeriodRepository extends JpaRepository<BlockedPeriod, String> {
    @Query("SELECT b FROM BlockedPeriod b WHERE b.date = ?1 FOR UPDATE")
    List<BlockedPeriod> findByDateWithLock(LocalDate date);

    // Find all blocked periods for a specific date
    List<BlockedPeriod> findByDate(LocalDate date);

    // Find blocked periods for a date range
    @EntityGraph(attributePaths = {"appointment"})
    List<BlockedPeriod> findByDateBetween(LocalDate startDate, LocalDate endDate);

    // Check if a specific time is blocked
    @Query("SELECT EXISTS(SELECT 1 FROM BlockedPeriod b WHERE b.date = ?1 AND " +
            "((b.startTime < ?3 AND b.endTime > ?2)))")
    boolean isTimeBlocked(LocalDate date, LocalTime startTime, LocalTime endTime);


    // Get dates that have at least one blocked period in a month
    @Query("SELECT DISTINCT b.date FROM BlockedPeriod b WHERE " +
            "EXTRACT(YEAR FROM b.date) = ?1 AND EXTRACT(MONTH FROM b.date) = ?2")
    List<LocalDate> getDatesWithBlockedPeriods(int year, int month);

    // Check if entire day is blocked (9am-5pm)
    @Query("SELECT CASE WHEN " +
            "(SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (b.endTime - b.startTime))/60), 0) " +
            "FROM BlockedPeriod b WHERE b.date = ?1) >= 480 " +
            "THEN true ELSE false END")
    boolean isFullDayBlocked(LocalDate date);
    List<BlockedPeriod> findByDateGreaterThanEqual(LocalDate startDate);

    // Find blocked periods up to a date
    List<BlockedPeriod> findByDateLessThanEqual(LocalDate endDate);

    // Find blocked periods ordered by date and time
    List<BlockedPeriod> findAllByOrderByDateAscStartTimeAsc();

    // Find overlapping blocked periods for validation
    @Query("SELECT b FROM BlockedPeriod b WHERE b.date = ?1 AND " +
            "((b.startTime < ?3 AND b.endTime > ?2))")
    List<BlockedPeriod> findOverlappingPeriods(LocalDate date, LocalTime startTime, LocalTime endTime);

    // Get total blocked minutes for a date
    @Query("SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (b.endTime - b.startTime))/60), 0) " +
            "FROM BlockedPeriod b WHERE b.date = ?1")
    Long getTotalBlockedMinutesForDate(LocalDate date);

    // Check if specific period conflicts with existing blocks
    @Query("SELECT COUNT(b) > 0 FROM BlockedPeriod b WHERE b.date = ?1 AND b.id != ?4 AND " +
            "((?2 < b.endTime AND ?3 > b.startTime))")
    boolean hasConflictingPeriod(LocalDate date, LocalTime startTime, LocalTime endTime, String excludeId);

}
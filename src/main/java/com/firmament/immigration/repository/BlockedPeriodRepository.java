// In BlockedPeriodRepository.java, update the isTimeBlocked query:

package com.firmament.immigration.repository;

import com.firmament.immigration.entity.BlockedPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface BlockedPeriodRepository extends JpaRepository<BlockedPeriod, String> {

    // Find all blocked periods for a specific date
    List<BlockedPeriod> findByDate(LocalDate date);

    // Find blocked periods for a date range
    List<BlockedPeriod> findByDateBetween(LocalDate startDate, LocalDate endDate);

        @Query("SELECT b FROM BlockedPeriod b WHERE " +
           "b.startDateTime < ?2 AND b.endDateTime > ?1")
    List<BlockedPeriod> findByDateTimeBetween(ZonedDateTime startUtc, ZonedDateTime endUtc);

        @Query("SELECT COUNT(b) > 0 FROM BlockedPeriod b WHERE " +
           "b.startDateTime < ?2 AND b.endDateTime > ?1")
    boolean isTimeBlockedUTC(ZonedDateTime startUtc, ZonedDateTime endUtc);

    // Check if a specific time is blocked
    // Fixed query - using proper overlap detection
//    @Query("SELECT COUNT(b) > 0 FROM BlockedPeriod b WHERE b.date = ?1 AND " +
//            "(b.startTime < ?3 AND b.endTime > ?2)")
//    boolean isTimeBlocked(LocalDate date, LocalTime startTime, LocalTime endTime);

    // Get dates that have at least one blocked period in a month
    @Query("SELECT DISTINCT b.date FROM BlockedPeriod b WHERE YEAR(b.date) = ?1 AND MONTH(b.date) = ?2")
    List<LocalDate> getDatesWithBlockedPeriods(int year, int month);

    // Find all blocked periods for a date
    @Query("SELECT b FROM BlockedPeriod b WHERE b.date = ?1")
    List<BlockedPeriod> findAllByDate(LocalDate date);

    // Count total blocked periods for a date
    @Query("SELECT COUNT(b) FROM BlockedPeriod b WHERE b.date = ?1")
    long countByDate(LocalDate date);

    // Find blocked periods ordered by date and time
    List<BlockedPeriod> findAllByOrderByDateAscStartDateTimeAsc();


    // Find blocked periods from a date onwards
    List<BlockedPeriod> findByDateGreaterThanEqual(LocalDate startDate);

    // Find blocked periods up to a date
    List<BlockedPeriod> findByDateLessThanEqual(LocalDate endDate);
}
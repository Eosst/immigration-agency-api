package com.firmament.immigration.repository;

import com.firmament.immigration.entity.Appointment;
import com.firmament.immigration.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, String> {

    // Find appointments by status
    List<Appointment> findByStatus(AppointmentStatus status);

    // Find appointments for a specific date range
    List<Appointment> findByAppointmentDateBetween(LocalDateTime start, LocalDateTime end);

    // Check if email has existing appointment
    boolean existsByEmailAndStatus(String email, AppointmentStatus status);

    // For admin dashboard
    @Query("SELECT a FROM Appointment a WHERE a.status = ?1 ORDER BY a.appointmentDate ASC")
    List<Appointment> findUpcomingAppointments(AppointmentStatus status);
}
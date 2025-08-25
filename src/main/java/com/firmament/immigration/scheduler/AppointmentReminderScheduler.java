package com.firmament.immigration.scheduler;

import com.firmament.immigration.entity.Appointment;
import com.firmament.immigration.entity.AppointmentStatus;
import com.firmament.immigration.repository.AppointmentRepository;
import com.firmament.immigration.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final EmailService emailService;

    /**
     * Runs every hour to check for appointments that need reminders
     * Sends reminder emails for appointments happening in the next 24-25 hours
     */
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    public void sendAppointmentReminders() {
        log.info("Starting appointment reminder check...");

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime tomorrow = now.plusHours(24);
        ZonedDateTime dayAfterTomorrow = now.plusHours(25);

        // Find confirmed appointments happening between 24-25 hours from now
        List<Appointment> upcomingAppointments = appointmentRepository
                .findByStatusAndAppointmentDateBetween(
                        AppointmentStatus.CONFIRMED,
                        tomorrow,
                        dayAfterTomorrow
                );

        log.info("Found {} appointments needing reminders", upcomingAppointments.size());

        for (Appointment appointment : upcomingAppointments) {
            try {
                // Check if we haven't already sent a reminder (you might want to add a flag to track this)
                emailService.sendAppointmentReminder(appointment);
                log.info("Sent reminder for appointment {}", appointment.getId());
            } catch (Exception e) {
                log.error("Failed to send reminder for appointment {}", appointment.getId(), e);
            }
        }
    }

    /**
     * Runs daily at 9 AM to send a summary to admin (optional)
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendDailySummary() {
        log.info("Sending daily appointment summary to admin...");

        ZonedDateTime today = ZonedDateTime.now(ZoneOffset.UTC).withHour(0).withMinute(0).withSecond(0);
        ZonedDateTime tomorrow = today.plusDays(1);

        List<Appointment> todaysAppointments = appointmentRepository
                .findByStatusAndAppointmentDateBetween(
                        AppointmentStatus.CONFIRMED,
                        today,
                        tomorrow
                );

        if (!todaysAppointments.isEmpty()) {
            log.info("Today has {} confirmed appointments", todaysAppointments.size());
            // You could send a summary email to admin here
        }
    }
}
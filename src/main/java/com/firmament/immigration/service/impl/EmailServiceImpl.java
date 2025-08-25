package com.firmament.immigration.service.impl;

import com.firmament.immigration.entity.Appointment;
import com.firmament.immigration.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.from-name}")
    private String fromName;

    @Value("${app.company.name}")
    private String companyName;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * Format appointment date/time in user's timezone
     */
    private String formatAppointmentDateTime(Appointment appointment) {
        // Get the appointment date (stored in UTC)
        ZonedDateTime utcDateTime = appointment.getAppointmentDate();

        // Convert to user's timezone
        ZoneId userZone = ZoneId.of(appointment.getUserTimezone());
        ZonedDateTime userDateTime = utcDateTime.withZoneSameInstant(userZone);

        // Format with timezone info
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                "EEEE, MMMM d, yyyy 'at' h:mm a z",
                Locale.ENGLISH
        );

        return userDateTime.format(formatter);
    }

    /**
     * Get formatted time only in user's timezone
     */
    private String formatTimeOnly(Appointment appointment) {
        ZonedDateTime utcDateTime = appointment.getAppointmentDate();
        ZoneId userZone = ZoneId.of(appointment.getUserTimezone());
        ZonedDateTime userDateTime = utcDateTime.withZoneSameInstant(userZone);

        return userDateTime.format(DateTimeFormatter.ofPattern("h:mm a"));
    }

    /**
     * Get timezone abbreviation for display
     */
    private String getTimezoneAbbreviation(Appointment appointment) {
        ZoneId userZone = ZoneId.of(appointment.getUserTimezone());
        ZonedDateTime userDateTime = appointment.getAppointmentDate()
                .withZoneSameInstant(userZone);

        // This will return abbreviations like EST, PST, CET, etc.
        return userDateTime.format(DateTimeFormatter.ofPattern("z"));
    }

    @Override
    public void sendAppointmentConfirmation(Appointment appointment) {
        Context context = new Context();
        context.setVariable("firstName", appointment.getFirstName());

        // Format date/time in user's timezone
        context.setVariable("appointmentDate", formatAppointmentDateTime(appointment));
        context.setVariable("timezone", appointment.getUserTimezone());
        context.setVariable("timezoneAbbr", getTimezoneAbbreviation(appointment));

        context.setVariable("duration", appointment.getDuration());
        context.setVariable("consultationType", appointment.getConsultationType());
        context.setVariable("appointmentId", appointment.getId());
        context.setVariable("viewLink", frontendUrl + "/appointments/" + appointment.getId());

        sendEmail(
                appointment.getEmail(),
                "Appointment Confirmation - " + companyName,
                "appointment-confirmation",
                context
        );
    }

    @Override
    public void sendPaymentReceipt(Appointment appointment, String paymentIntentId) {
        Context context = new Context();
        context.setVariable("firstName", appointment.getFirstName());
        context.setVariable("amount", appointment.getAmount());
        context.setVariable("currency", appointment.getCurrency());
        context.setVariable("paymentId", paymentIntentId);

        // Format date/time in user's timezone
        context.setVariable("appointmentDate", formatAppointmentDateTime(appointment));
        context.setVariable("timezone", appointment.getUserTimezone());

        // Current date/time in user's timezone for receipt
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(appointment.getUserTimezone()));
        context.setVariable("paymentDate", now.format(
                DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a z")
        ));

        sendEmail(
                appointment.getEmail(),
                "Payment Receipt - " + companyName,
                "payment-receipt",
                context
        );
    }

    @Override
    public void sendAppointmentReminder(Appointment appointment) {
        Context context = new Context();
        context.setVariable("firstName", appointment.getFirstName());

        // Format date/time in user's timezone
        context.setVariable("appointmentDate", formatAppointmentDateTime(appointment));
        context.setVariable("appointmentTime", formatTimeOnly(appointment));
        context.setVariable("timezone", appointment.getUserTimezone());
        context.setVariable("timezoneAbbr", getTimezoneAbbreviation(appointment));

        context.setVariable("joinLink", frontendUrl + "/appointments/" + appointment.getId());

        sendEmail(
                appointment.getEmail(),
                "Appointment Reminder - Tomorrow at " + formatTimeOnly(appointment),
                "appointment-reminder",
                context
        );
    }

    @Override
    public void sendDocumentUploadConfirmation(Appointment appointment, List<String> fileNames) {
        Context context = new Context();
        context.setVariable("firstName", appointment.getFirstName());
        context.setVariable("fileNames", fileNames);

        // Format date/time in user's timezone
        context.setVariable("appointmentDate", formatAppointmentDateTime(appointment));
        context.setVariable("timezone", appointment.getUserTimezone());

        sendEmail(
                appointment.getEmail(),
                "Documents Received - " + companyName,
                "document-confirmation",
                context
        );
    }

    @Override
    public void sendCancellationNotification(Appointment appointment) {
        Context context = new Context();
        context.setVariable("firstName", appointment.getFirstName());

        // Format date/time in user's timezone
        context.setVariable("appointmentDate", formatAppointmentDateTime(appointment));
        context.setVariable("timezone", appointment.getUserTimezone());

        sendEmail(
                appointment.getEmail(),
                "Appointment Cancelled - " + companyName,
                "appointment-cancellation",
                context
        );
    }

    private void sendEmail(String to, String subject, String template, Context context) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);

            String htmlContent = templateEngine.process(template, context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to: {} for timezone: {}",
                    to, context.getVariable("timezone"));

        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            // Don't throw exception to not break the flow
        }
    }
}
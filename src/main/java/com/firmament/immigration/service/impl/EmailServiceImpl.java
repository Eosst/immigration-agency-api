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

import java.time.format.DateTimeFormatter;
import java.util.List;

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

    @Override
    public void sendAppointmentConfirmation(Appointment appointment) {
        Context context = new Context();
        context.setVariable("firstName", appointment.getFirstName());
        context.setVariable("appointmentDate", appointment.getAppointmentDate()
                .format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a")));
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
        context.setVariable("appointmentDate", appointment.getAppointmentDate()
                .format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a")));

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
        context.setVariable("appointmentDate", appointment.getAppointmentDate()
                .format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a")));
        context.setVariable("joinLink", frontendUrl + "/appointments/" + appointment.getId());

        sendEmail(
                appointment.getEmail(),
                "Appointment Reminder - Tomorrow at " +
                        appointment.getAppointmentDate().format(DateTimeFormatter.ofPattern("h:mm a")),
                "appointment-reminder",
                context
        );
    }

    @Override
    public void sendDocumentUploadConfirmation(Appointment appointment, List<String> fileNames) {
        Context context = new Context();
        context.setVariable("firstName", appointment.getFirstName());
        context.setVariable("fileNames", fileNames);
        context.setVariable("appointmentDate", appointment.getAppointmentDate()
                .format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a")));

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
        context.setVariable("appointmentDate", appointment.getAppointmentDate()
                .format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a")));

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
            log.info("Email sent successfully to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            // Don't throw exception to not break the flow
        }
    }
}
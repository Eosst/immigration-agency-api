package com.firmament.immigration.service;

import com.firmament.immigration.entity.Appointment;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EmailService {
    void sendAppointmentConfirmation(Appointment appointment);
    void sendPaymentReceipt(Appointment appointment, String paymentIntentId);
    void sendAppointmentReminder(Appointment appointment);
    void sendDocumentUploadConfirmation(Appointment appointment, List<String> fileNames);
    void sendCancellationNotification(Appointment appointment);
}
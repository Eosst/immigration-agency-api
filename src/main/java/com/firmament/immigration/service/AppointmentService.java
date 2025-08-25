package com.firmament.immigration.service;

import com.firmament.immigration.dto.request.CreateAppointmentRequest;
import com.firmament.immigration.dto.request.UpdateAppointmentRequest;
import com.firmament.immigration.dto.response.AppointmentResponse;
import com.firmament.immigration.entity.AppointmentStatus;
import java.util.List;

public interface AppointmentService {
    AppointmentResponse createAppointment(CreateAppointmentRequest request);
    AppointmentResponse getAppointmentById(String id);
    List<AppointmentResponse> getUpcomingAppointments();
    List<AppointmentResponse> getAllAppointments();
    List<AppointmentResponse> getAppointmentsByStatus(AppointmentStatus status);
    AppointmentResponse confirmPayment(String appointmentId, String paymentIntentId);
    void cancelAppointment(String id);
    AppointmentResponse updateAppointment(String appointmentId, UpdateAppointmentRequest request);
}
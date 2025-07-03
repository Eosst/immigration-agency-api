package com.firmament.immigration.service;

import com.firmament.immigration.dto.request.CreateAppointmentRequest;
import com.firmament.immigration.dto.response.AppointmentResponse;
import java.util.List;

public interface AppointmentService {
    AppointmentResponse createAppointment(CreateAppointmentRequest request);
    AppointmentResponse getAppointmentById(String id);
    List<AppointmentResponse> getUpcomingAppointments();
    AppointmentResponse confirmPayment(String appointmentId, String paymentIntentId);
    void cancelAppointment(String id);
}
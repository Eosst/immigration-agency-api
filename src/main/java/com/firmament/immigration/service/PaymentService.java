package com.firmament.immigration.service;

import com.firmament.immigration.dto.response.PaymentIntentResponse;
import com.stripe.model.PaymentIntent;

public interface PaymentService {
    PaymentIntentResponse createPaymentIntent(String appointmentId);
    PaymentIntent confirmPayment(String paymentIntentId);
    void handleWebhook(String payload, String sigHeader);
}
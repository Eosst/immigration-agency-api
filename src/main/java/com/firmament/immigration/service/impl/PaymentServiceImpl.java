package com.firmament.immigration.service.impl;

import com.firmament.immigration.dto.response.PaymentIntentResponse;
import com.firmament.immigration.entity.Appointment;
import com.firmament.immigration.entity.AppointmentStatus;
import com.firmament.immigration.exception.BusinessException;
import com.firmament.immigration.exception.ResourceNotFoundException;
import com.firmament.immigration.repository.AppointmentRepository;
import com.firmament.immigration.service.EmailService;
import com.firmament.immigration.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final AppointmentRepository appointmentRepository;
    private final EmailService emailService;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Override
    public PaymentIntentResponse createPaymentIntent(String appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (appointment.getPaymentIntentId() != null) {
            // Return existing payment intent
            return PaymentIntentResponse.builder()
                    .clientSecret(appointment.getPaymentIntentId())
                    .amount(appointment.getAmount())
                    .currency(appointment.getCurrency())
                    .build();
        }

        try {
            // Create payment intent with Stripe
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(appointment.getAmount().multiply(new java.math.BigDecimal(100)).longValue()) // Convert to cents
                    .setCurrency(appointment.getCurrency().toLowerCase())
                    .setDescription("Immigration Consultation - " + appointment.getConsultationType())
                    .putMetadata("appointmentId", appointment.getId())
                    .putMetadata("clientEmail", appointment.getEmail())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            // Store payment intent ID
            appointment.setPaymentIntentId(intent.getId());
            appointmentRepository.save(appointment);

            return PaymentIntentResponse.builder()
                    .clientSecret(intent.getClientSecret())
                    .amount(appointment.getAmount())
                    .currency(appointment.getCurrency())
                    .build();

        } catch (StripeException e) {
            log.error("Stripe error creating payment intent", e);
            throw new BusinessException("Failed to create payment intent: " + e.getMessage());
        }
    }

    @Override
    public PaymentIntent confirmPayment(String paymentIntentId) {
        try {
            return PaymentIntent.retrieve(paymentIntentId);
        } catch (StripeException e) {
            log.error("Error retrieving payment intent", e);
            throw new BusinessException("Failed to retrieve payment status");
        }
    }

    @Override
    public void handleWebhook(String payload, String sigHeader) {
        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid webhook signature", e);
            throw new BusinessException("Invalid webhook signature");
        }

        // Handle the event
        switch (event.getType()) {
            case "payment_intent.succeeded":
                PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject().orElse(null);
                if (paymentIntent != null) {
                    handlePaymentSuccess(paymentIntent);
                }
                break;

            case "payment_intent.payment_failed":
                paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject().orElse(null);
                if (paymentIntent != null) {
                    handlePaymentFailure(paymentIntent);
                }
                break;

            default:
                log.info("Unhandled event type: {}", event.getType());
        }
    }

    private void handlePaymentSuccess(PaymentIntent paymentIntent) {
        String appointmentId = paymentIntent.getMetadata().get("appointmentId");
        if (appointmentId != null) {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

            appointment.setStatus(AppointmentStatus.CONFIRMED);
            appointmentRepository.save(appointment);

            // Send confirmation email
            emailService.sendAppointmentConfirmation(appointment);
            emailService.sendPaymentReceipt(appointment, paymentIntent.getId());

            log.info("Payment confirmed for appointment: {}", appointmentId);
        }
    }

    private void handlePaymentFailure(PaymentIntent paymentIntent) {
        String appointmentId = paymentIntent.getMetadata().get("appointmentId");
        if (appointmentId != null) {
            log.warn("Payment failed for appointment: {}", appointmentId);
            // You might want to send a failure notification email
        }
    }
}
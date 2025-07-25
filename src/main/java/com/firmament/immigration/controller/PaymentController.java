package com.firmament.immigration.controller;

import com.firmament.immigration.dto.response.PaymentIntentResponse;
import com.firmament.immigration.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment processing endpoints")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-intent/{appointmentId}")
    @Operation(summary = "Create payment intent for appointment")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(@PathVariable String appointmentId) {
        return ResponseEntity.ok(paymentService.createPaymentIntent(appointmentId));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Stripe webhook endpoint", description = "Handle Stripe payment events")
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        paymentService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok().build();
    }
}
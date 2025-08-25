package com.firmament.immigration.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

@Data
public class AppointmentResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String country;
    private ZonedDateTime appointmentDate; // UTC time
    private String userTimezone; // User's timezone for display
    private Integer duration;
    private String consultationType;
    private String clientPresentation;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String paymentIntentId;
    private List<DocumentResponse> documents;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
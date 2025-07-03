package com.firmament.immigration.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
public class AppointmentResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDateTime appointmentDate;
    private Integer duration;
    private String consultationType;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String paymentIntentId;
}
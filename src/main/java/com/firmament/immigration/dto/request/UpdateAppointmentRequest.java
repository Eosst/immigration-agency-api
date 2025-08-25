package com.firmament.immigration.dto.request;

import com.firmament.immigration.entity.AppointmentStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
public class UpdateAppointmentRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private ZonedDateTime appointmentDate;
    private Integer duration;
    private AppointmentStatus status;
    private String adminNotes;
    private String consultationType;
    private BigDecimal amount;
    private String currency;
}
// src/main/java/com/firmament/immigration/dto/request/UpdateAppointmentRequest.java
package com.firmament.immigration.dto.request;

import com.firmament.immigration.entity.AppointmentStatus;
import lombok.Data;
import java.time.ZonedDateTime;

@Data
public class UpdateAppointmentRequest {
    private ZonedDateTime appointmentDate;
    private Integer duration;
    private AppointmentStatus status;
    private String adminNotes;
    private String consultationType;
}
package com.firmament.immigration.dto.request;


import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Data
public class CreateAppointmentRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Valid email is required")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "Country is required")
    private String country;

    @NotNull(message = "Appointment date is required")
    @Future(message = "Appointment date must be in future")
    private ZonedDateTime appointmentDate;

    @NotNull(message = "Duration is required")
    @Min(30) @Max(90)
    private Integer duration;

    @NotBlank(message = "Consultation type is required")
    private String consultationType;

    @Size(max = 1000, message = "Client presentation must not exceed 1000 characters")
    private String clientPresentation;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "CAD|MAD", message = "Currency must be CAD or MAD")
    private String currency;

    @NotBlank(message = "User timezone is required")
    private String userTimezone;
}
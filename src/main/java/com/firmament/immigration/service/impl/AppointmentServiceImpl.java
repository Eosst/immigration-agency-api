package com.firmament.immigration.service.impl;

import com.firmament.immigration.dto.request.CreateAppointmentRequest;
import com.firmament.immigration.dto.response.AppointmentResponse;
import com.firmament.immigration.entity.Appointment;
import com.firmament.immigration.entity.AppointmentStatus;
import com.firmament.immigration.exception.ResourceNotFoundException;
import com.firmament.immigration.exception.BusinessException;
import com.firmament.immigration.repository.AppointmentRepository;
import com.firmament.immigration.repository.BlockedPeriodRepository;
import com.firmament.immigration.service.AppointmentService;
import com.firmament.immigration.service.AvailabilityService;
import com.firmament.immigration.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.firmament.immigration.config.PricingConfig;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final BlockedPeriodRepository blockedPeriodRepository;
    private final AvailabilityService availabilityService;
    private final EmailService emailService; // ADD THIS
    private final ModelMapper modelMapper;
    private final PricingConfig pricingConfig;

    @Override
    public AppointmentResponse createAppointment(CreateAppointmentRequest request) {
        log.info("Creating appointment for: {} {}", request.getFirstName(), request.getLastName());

        // 1. Validate no existing pending appointment for this email
        if (appointmentRepository.existsByEmailAndStatus(request.getEmail(), AppointmentStatus.PENDING)) {
            throw new BusinessException("You already have a pending appointment. Please complete or cancel it first.");
        }

        // 2. Check if the requested time is available
        if (!availabilityService.isAvailable(request.getAppointmentDate(), request.getDuration())) {
            throw new BusinessException("Selected time is not available. Please choose another time.");
        }

        // 3. Calculate price
        BigDecimal amount = calculatePrice(request.getDuration(), request.getCurrency());

        // 4. Create and save appointment
        Appointment appointment = Appointment.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .country(request.getCountry())
                .appointmentDate(request.getAppointmentDate())
                .duration(request.getDuration())
                .consultationType(request.getConsultationType())
                .clientPresentation(request.getClientPresentation())
                .amount(amount)
                .currency(request.getCurrency())
                .status(AppointmentStatus.PENDING)
                .build();

        appointment = appointmentRepository.save(appointment);

        // 5. Block the time slot for this appointment
        availabilityService.blockTimeForAppointment(
                appointment.getId(),
                request.getAppointmentDate(),
                request.getDuration()
        );

        // 6. Send initial confirmation email (appointment created, payment pending)
        try {
            emailService.sendAppointmentConfirmation(appointment);
        } catch (Exception e) {
            log.error("Failed to send appointment confirmation email", e);
            // Don't fail the appointment creation if email fails
        }

        log.info("Appointment created with ID: {}", appointment.getId());

        return mapToResponse(appointment);
    }

    @Override
    public AppointmentResponse confirmPayment(String appointmentId, String paymentIntentId) {
        log.info("Confirming payment for appointment: {}", appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new BusinessException("Appointment is not in pending status");
        }

        appointment.setPaymentIntentId(paymentIntentId);
        appointment.setStatus(AppointmentStatus.CONFIRMED);

        appointment = appointmentRepository.save(appointment);

        // Send payment confirmation email
        try {
            emailService.sendPaymentReceipt(appointment, paymentIntentId);
        } catch (Exception e) {
            log.error("Failed to send payment receipt email", e);
        }

        return mapToResponse(appointment);
    }

    @Override
    public void cancelAppointment(String id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessException("Cannot cancel completed appointment");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
        availabilityService.freeUpBlockedTimeForAppointment(id);

        // Send cancellation notification email
        try {
            emailService.sendCancellationNotification(appointment);
        } catch (Exception e) {
            log.error("Failed to send cancellation email", e);
        }

        log.info("Appointment {} cancelled", id);
    }

    // ... rest of the methods remain the same

    @Override
    public AppointmentResponse getAppointmentById(String id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        return mapToResponse(appointment);
    }

    @Override
    public List<AppointmentResponse> getUpcomingAppointments() {
        List<Appointment> appointments = appointmentRepository
                .findUpcomingAppointments(AppointmentStatus.CONFIRMED);

        return appointments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private BigDecimal calculatePrice(int duration, String currency) {
        Map<String, Integer> prices;
        if ("CAD".equalsIgnoreCase(currency)) {
            prices = pricingConfig.getCadDuration();
        } else {
            prices = pricingConfig.getMadDuration();
        }
        if (prices == null) {
            throw new BusinessException("Pricing not configured for currency: " + currency);
        }

        BigDecimal price = BigDecimal.valueOf(prices.getOrDefault(String.valueOf(duration), 0));

        if (price.equals(BigDecimal.ZERO)) {
            throw new BusinessException("No price configured for duration: " + duration + " and currency: " + currency);
        }
        return price;
    }

    private AppointmentResponse mapToResponse(Appointment appointment) {
        return modelMapper.map(appointment, AppointmentResponse.class);
    }
}
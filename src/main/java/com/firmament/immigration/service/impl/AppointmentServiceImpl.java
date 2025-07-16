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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final BlockedPeriodRepository blockedPeriodRepository;
    private final AvailabilityService availabilityService;
    private final ModelMapper modelMapper;

    // Price configuration
    private static final BigDecimal PRICE_30_MIN_CAD = new BigDecimal("50");
    private static final BigDecimal PRICE_60_MIN_CAD = new BigDecimal("90");
    private static final BigDecimal PRICE_90_MIN_CAD = new BigDecimal("130");
    private static final BigDecimal CAD_TO_MAD_RATE = new BigDecimal("10");

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

        log.info("Appointment created with ID: {}", appointment.getId());

        return mapToResponse(appointment);
    }

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

        // TODO: Send confirmation email here

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

        // Free up the blocked time
        // Find and delete the blocked period associated with this appointment
        blockedPeriodRepository.findAll().stream()
                .filter(bp -> bp.getAppointment() != null &&
                        bp.getAppointment().getId().equals(appointment.getId()))
                .findFirst()
                .ifPresent(blockedPeriod -> {
                    blockedPeriodRepository.delete(blockedPeriod);
                    log.info("Freed up blocked time for cancelled appointment: {}", id);
                });

        log.info("Appointment {} cancelled", id);
    }

    private BigDecimal calculatePrice(Integer duration, String currency) {
        BigDecimal priceInCAD;

        switch (duration) {
            case 30:
                priceInCAD = PRICE_30_MIN_CAD;
                break;
            case 60:
                priceInCAD = PRICE_60_MIN_CAD;
                break;
            case 90:
                priceInCAD = PRICE_90_MIN_CAD;
                break;
            default:
                throw new BusinessException("Invalid duration: " + duration);
        }

        if ("MAD".equals(currency)) {
            return priceInCAD.multiply(CAD_TO_MAD_RATE);
        }

        return priceInCAD;
    }

    private AppointmentResponse mapToResponse(Appointment appointment) {
        return modelMapper.map(appointment, AppointmentResponse.class);
    }

}


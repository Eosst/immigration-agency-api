package com.firmament.immigration.service.impl;

import com.firmament.immigration.dto.request.CreateAppointmentRequest;
import com.firmament.immigration.dto.response.AppointmentResponse;
import com.firmament.immigration.entity.Appointment;
import com.firmament.immigration.entity.AppointmentStatus;
import com.firmament.immigration.entity.TimeSlot;
import com.firmament.immigration.exception.ResourceNotFoundException;
import com.firmament.immigration.exception.BusinessException;
import com.firmament.immigration.repository.AppointmentRepository;
import com.firmament.immigration.repository.TimeSlotRepository;
import com.firmament.immigration.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j  // Lombok annotation for logging
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final ModelMapper modelMapper;

    // Price configuration (move to config later)
    private static final BigDecimal PRICE_30_MIN_CAD = new BigDecimal("50");
    private static final BigDecimal PRICE_60_MIN_CAD = new BigDecimal("90");
    private static final BigDecimal PRICE_90_MIN_CAD = new BigDecimal("130");
    private static final BigDecimal CAD_TO_MAD_RATE = new BigDecimal("10"); // Approximate

    @Override
    public AppointmentResponse createAppointment(CreateAppointmentRequest request) {
        log.info("Creating appointment for: {} {}", request.getFirstName(), request.getLastName());

        // 1. Validate no existing pending appointment for this email
        if (appointmentRepository.existsByEmailAndStatus(request.getEmail(), AppointmentStatus.PENDING)) {
            throw new BusinessException("You already have a pending appointment. Please complete or cancel it first.");
        }

        // 2. Check time slot availability
        LocalDate date = request.getAppointmentDate().toLocalDate();
        LocalTime time = request.getAppointmentDate().toLocalTime();

        // First, ensure slots exist for this date
        List<TimeSlot> availableSlots = timeSlotRepository.findByDateAndAvailableTrue(date);
        if (availableSlots.isEmpty()) {
            throw new BusinessException("No available time slots for selected date");
        }

        // Find the specific requested slot
        TimeSlot timeSlot = availableSlots.stream()
                .filter(slot -> slot.getStartTime().equals(time))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        String.format("Time slot at %s is not available on %s", time, date)
                ));

        // 3. Calculate price
        BigDecimal amount = calculatePrice(request.getDuration(), request.getCurrency());

        // 4. Create appointment
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

        // 5. Mark time slot as unavailable
        timeSlot.setAvailable(false);
        timeSlot.setAppointment(appointment);
        timeSlotRepository.save(timeSlot);

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

        // Free up the time slot
        TimeSlot timeSlot = timeSlotRepository.findByDate(appointment.getAppointmentDate().toLocalDate())
                .stream()
                .filter(slot -> slot.getAppointment() != null &&
                        slot.getAppointment().getId().equals(appointment.getId()))
                .findFirst()
                .orElse(null);

        if (timeSlot != null) {
            timeSlot.setAvailable(true);
            timeSlot.setAppointment(null);
            timeSlotRepository.save(timeSlot);
        }

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
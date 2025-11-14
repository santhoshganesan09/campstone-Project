package com.project.back_end.services;

import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorRepository doctorRepository,
                              PatientRepository patientRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    /**
     * DTO for creating an appointment. You can replace this with your project's DTO class.
     */
    public static class AppointmentDto {
        public Long doctorId;
        public Long patientId;
        public LocalDateTime appointmentTime;
        public String status; // optional
        // constructors/getters/setters can be added as needed
    }

    /**
     * Book an appointment.
     * Validations performed:
     *  - doctor exists
     *  - patient exists
     *  - no conflicting appointment for the doctor at the same time
     *
     * @param dto appointment data
     * @return saved Appointment entity
     * @throws IllegalArgumentException on validation failures
     */
    @Transactional
    public Appointment bookAppointment(AppointmentDto dto) {
        if (dto == null) throw new IllegalArgumentException("Appointment data is required");
        if (dto.doctorId == null) throw new IllegalArgumentException("doctorId is required");
        if (dto.patientId == null) throw new IllegalArgumentException("patientId is required");
        if (dto.appointmentTime == null) throw new IllegalArgumentException("appointmentTime is required");

        Optional<Doctor> doctorOpt = doctorRepository.findById(dto.doctorId);
        if (doctorOpt.isEmpty()) throw new IllegalArgumentException("Doctor not found: " + dto.doctorId);

        Optional<Patient> patientOpt = patientRepository.findById(dto.patientId);
        if (patientOpt.isEmpty()) throw new IllegalArgumentException("Patient not found: " + dto.patientId);

        LocalDateTime requested = dto.appointmentTime;

        // Check conflict: you can implement more advanced overlap logic if appointments have duration.
        boolean conflict = appointmentRepository.existsByDoctorIdAndAppointmentTime(dto.doctorId, requested);
        if (conflict) {
            throw new IllegalArgumentException("Requested time slot is already taken for doctor " + dto.doctorId);
        }

        Appointment appt = new Appointment();
        appt.setDoctor(doctorOpt.get());
        appt.setPatient(patientOpt.get());
        appt.setAppointmentTime(requested);
        appt.setStatus(dto.status != null ? dto.status : "BOOKED");

        return appointmentRepository.save(appt);
    }

    /**
     * Get appointments for a doctor on a specific date.
     *
     * @param doctorId doctor id
     * @param date     date for which to fetch appointments
     * @return list of appointments on that date
     */
    @Transactional(readOnly = true)
    public List<Appointment> getAppointmentsForDoctorOnDate(Long doctorId, LocalDate date) {
        if (doctorId == null) throw new IllegalArgumentException("doctorId is required");
        if (date == null) throw new IllegalArgumentException("date is required");

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX); // or date.plusDays(1).atStartOfDay();

        return appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(doctorId, start, end);
    }

    /**
     * Get all appointments booked by a patient.
     *
     * @param patientId patient id
     * @return list of appointments
     */
    @Transactional(readOnly = true)
    public List<Appointment> getAppointmentsByPatient(Long patientId) {
        if (patientId == null) throw new IllegalArgumentException("patientId is required");
        return appointmentRepository.findByPatientId(patientId);
    }

    /**
     * Cancel (soft delete) an appointment by updating its status to CANCELLED.
     *
     * @param appointmentId id of appointment to cancel
     * @param cancelledBy   who cancelled (optional; for audit if you want)
     * @return updated appointment
     */
    @Transactional
    public Appointment cancelAppointment(Long appointmentId, String cancelledBy) {
        if (appointmentId == null) throw new IllegalArgumentException("appointmentId is required");
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));

        appt.setStatus("CANCELLED");
        // optionally add audit fields: cancelledBy, cancelledAt
        return appointmentRepository.save(appt);
    }
// 1. **Add @Service Annotation**:
//    - To indicate that this class is a service layer class for handling business logic.
//    - The `@Service` annotation should be added before the class declaration to mark it as a Spring service component.
//    - Instruction: Add `@Service` above the class definition.

// 2. **Constructor Injection for Dependencies**:
//    - The `AppointmentService` class requires several dependencies like `AppointmentRepository`, `Service`, `TokenService`, `PatientRepository`, and `DoctorRepository`.
//    - These dependencies should be injected through the constructor.
//    - Instruction: Ensure constructor injection is used for proper dependency management in Spring.

// 3. **Add @Transactional Annotation for Methods that Modify Database**:
//    - The methods that modify or update the database should be annotated with `@Transactional` to ensure atomicity and consistency of the operations.
//    - Instruction: Add the `@Transactional` annotation above methods that interact with the database, especially those modifying data.

// 4. **Book Appointment Method**:
//    - Responsible for saving the new appointment to the database.
//    - If the save operation fails, it returns `0`; otherwise, it returns `1`.
//    - Instruction: Ensure that the method handles any exceptions and returns an appropriate result code.

// 5. **Update Appointment Method**:
//    - This method is used to update an existing appointment based on its ID.
//    - It validates whether the patient ID matches, checks if the appointment is available for updating, and ensures that the doctor is available at the specified time.
//    - If the update is successful, it saves the appointment; otherwise, it returns an appropriate error message.
//    - Instruction: Ensure proper validation and error handling is included for appointment updates.

// 6. **Cancel Appointment Method**:
//    - This method cancels an appointment by deleting it from the database.
//    - It ensures the patient who owns the appointment is trying to cancel it and handles possible errors.
//    - Instruction: Make sure that the method checks for the patient ID match before deleting the appointment.

// 7. **Get Appointments Method**:
//    - This method retrieves a list of appointments for a specific doctor on a particular day, optionally filtered by the patient's name.
//    - It uses `@Transactional` to ensure that database operations are consistent and handled in a single transaction.
//    - Instruction: Ensure the correct use of transaction boundaries, especially when querying the database for appointments.

// 8. **Change Status Method**:
//    - This method updates the status of an appointment by changing its value in the database.
//    - It should be annotated with `@Transactional` to ensure the operation is executed in a single transaction.
//    - Instruction: Add `@Transactional` before this method to ensure atomicity when updating appointment status.


}

package com.opennova.service;

import com.opennova.model.Doctor;
import com.opennova.model.Establishment;
import com.opennova.repository.DoctorRepository;
import com.opennova.repository.EstablishmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DoctorService {

    @Autowired
    private DoctorRepository doctorRepository;
    
    @Autowired
    private EstablishmentRepository establishmentRepository;

    public List<Doctor> getDoctorsByEstablishmentId(Long establishmentId) {
        return doctorRepository.findActiveDoctorsByEstablishmentIdOrderByCreatedAtDesc(establishmentId);
    }

    public Doctor createDoctor(Long establishmentId, Doctor doctor) {
        Optional<Establishment> establishment = establishmentRepository.findById(establishmentId);
        if (establishment.isPresent()) {
            doctor.setEstablishment(establishment.get());
            doctor.setCreatedAt(LocalDateTime.now());
            doctor.setUpdatedAt(LocalDateTime.now());
            doctor.setIsActive(true);
            return doctorRepository.save(doctor);
        }
        throw new RuntimeException("Establishment not found");
    }

    public Doctor updateDoctor(Long doctorId, Doctor doctorData, Long establishmentId) {
        Optional<Doctor> existingDoctor = doctorRepository.findById(doctorId);
        if (existingDoctor.isPresent()) {
            Doctor doctor = existingDoctor.get();
            
            // Verify the doctor belongs to the correct establishment
            if (!doctor.getEstablishment().getId().equals(establishmentId)) {
                throw new RuntimeException("Doctor does not belong to this establishment");
            }
            
            doctor.setName(doctorData.getName());
            doctor.setSpecialization(doctorData.getSpecialization());
            doctor.setPrice(doctorData.getPrice());
            doctor.setAvailabilityTime(doctorData.getAvailabilityTime());
            doctor.setImagePath(doctorData.getImagePath());
            doctor.setUpdatedAt(LocalDateTime.now());
            
            return doctorRepository.save(doctor);
        }
        throw new RuntimeException("Doctor not found");
    }

    public boolean deleteDoctor(Long doctorId, Long establishmentId) {
        Optional<Doctor> existingDoctor = doctorRepository.findById(doctorId);
        if (existingDoctor.isPresent()) {
            Doctor doctor = existingDoctor.get();
            
            // Verify the doctor belongs to the correct establishment
            if (!doctor.getEstablishment().getId().equals(establishmentId)) {
                return false;
            }
            
            doctor.setIsActive(false);
            doctor.setUpdatedAt(LocalDateTime.now());
            doctorRepository.save(doctor);
            return true;
        }
        return false;
    }

    public Doctor getDoctorById(Long doctorId, Long establishmentId) {
        Optional<Doctor> doctor = doctorRepository.findById(doctorId);
        if (doctor.isPresent() && doctor.get().getEstablishment().getId().equals(establishmentId) && doctor.get().getIsActive()) {
            return doctor.get();
        }
        return null;
    }

    public long getDoctorCountByEstablishment(Long establishmentId) {
        return doctorRepository.countByEstablishmentIdAndIsActive(establishmentId);
    }

    public boolean doctorExistsByName(Long establishmentId, String name) {
        return doctorRepository.existsByEstablishmentIdAndName(establishmentId, name);
    }
}
package com.opennova.repository;

import com.opennova.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    
    @Query("SELECT d FROM Doctor d WHERE d.establishment.id = :establishmentId AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Doctor> findActiveDoctorsByEstablishmentIdOrderByCreatedAtDesc(@Param("establishmentId") Long establishmentId);
    
    @Query("SELECT COUNT(d) FROM Doctor d WHERE d.establishment.id = :establishmentId AND d.isActive = true")
    long countByEstablishmentIdAndIsActive(@Param("establishmentId") Long establishmentId);
    
    @Query("SELECT d FROM Doctor d WHERE d.establishment.id = :establishmentId AND d.name = :name AND d.isActive = true")
    List<Doctor> findByEstablishmentIdAndNameAndIsActive(@Param("establishmentId") Long establishmentId, @Param("name") String name);
    
    boolean existsByEstablishmentIdAndName(Long establishmentId, String name);
}
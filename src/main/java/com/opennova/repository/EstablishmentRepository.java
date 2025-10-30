package com.opennova.repository;

import com.opennova.model.Establishment;
import com.opennova.model.EstablishmentStatus;
import com.opennova.model.EstablishmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstablishmentRepository extends JpaRepository<Establishment, Long> {
    Optional<Establishment> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Establishment> findByType(EstablishmentType type);
    List<Establishment> findByStatus(EstablishmentStatus status);
    List<Establishment> findByTypeAndStatus(EstablishmentType type, EstablishmentStatus status);
    List<Establishment> findByIsActiveTrue();
    
    @Query("SELECT e FROM Establishment e WHERE e.isActive = true AND " +
           "(:type IS NULL OR e.type = :type) AND " +
           "(:status IS NULL OR e.status = :status)")
    List<Establishment> findByFilters(@Param("type") EstablishmentType type, 
                                    @Param("status") EstablishmentStatus status);
    
    // Search methods
    List<Establishment> findByNameContainingIgnoreCaseOrAddressContainingIgnoreCase(String name, String address);
    
    // Owner methods
    List<Establishment> findByOwnerId(Long ownerId);
    List<Establishment> findByOwner(com.opennova.model.User owner);
    
    // Count methods
    long countByType(EstablishmentType type);
    long countByStatus(EstablishmentStatus status);
    long countByIsActiveAndStatus(boolean isActive, EstablishmentStatus status);
}
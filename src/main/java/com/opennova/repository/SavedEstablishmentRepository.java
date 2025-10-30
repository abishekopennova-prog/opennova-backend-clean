package com.opennova.repository;

import com.opennova.model.SavedEstablishment;
import com.opennova.model.EstablishmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedEstablishmentRepository extends JpaRepository<SavedEstablishment, Long> {
    
    // Find saved establishments by user
    List<SavedEstablishment> findByUserIdOrderBySavedAtDesc(Long userId);
    
    // Find saved establishments by user and type
    @Query("SELECT se FROM SavedEstablishment se WHERE se.user.id = :userId AND se.establishment.type = :type ORDER BY se.savedAt DESC")
    List<SavedEstablishment> findByUserIdAndEstablishmentType(@Param("userId") Long userId, @Param("type") EstablishmentType type);
    
    // Check if establishment is saved by user
    boolean existsByUserIdAndEstablishmentId(Long userId, Long establishmentId);
    
    // Find specific saved establishment
    Optional<SavedEstablishment> findByUserIdAndEstablishmentId(Long userId, Long establishmentId);
    
    // Count saved establishments by user
    long countByUserId(Long userId);
    
    // Count saved establishments by type for a user
    @Query("SELECT COUNT(se) FROM SavedEstablishment se WHERE se.user.id = :userId AND se.establishment.type = :type")
    long countByUserIdAndEstablishmentType(@Param("userId") Long userId, @Param("type") EstablishmentType type);
    
    // Get all users who saved a specific establishment (for owner portal)
    @Query("SELECT se FROM SavedEstablishment se WHERE se.establishment.id = :establishmentId ORDER BY se.savedAt DESC")
    List<SavedEstablishment> findByEstablishmentId(@Param("establishmentId") Long establishmentId);
}
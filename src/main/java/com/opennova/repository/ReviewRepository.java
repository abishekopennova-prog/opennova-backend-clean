package com.opennova.repository;

import com.opennova.model.Review;
import com.opennova.model.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    List<Review> findByEstablishmentIdOrderByCreatedAtDesc(Long establishmentId);
    
    List<Review> findByEstablishmentIdAndStatusOrderByCreatedAtDesc(Long establishmentId, ReviewStatus status);
    
    @Query("SELECT r FROM Review r WHERE r.establishment.owner.id = :ownerId ORDER BY r.createdAt DESC")
    List<Review> findByEstablishmentOwnerIdOrderByCreatedAtDesc(@Param("ownerId") Long ownerId);
    
    @Query("SELECT r FROM Review r WHERE r.establishment.owner.id = :ownerId AND r.status = :status ORDER BY r.createdAt DESC")
    List<Review> findByEstablishmentOwnerIdAndStatusOrderByCreatedAtDesc(@Param("ownerId") Long ownerId, @Param("status") ReviewStatus status);
    
    List<Review> findAllByOrderByCreatedAtDesc();
    
    long countByEstablishmentId(Long establishmentId);
    
    long countByEstablishmentIdAndStatus(Long establishmentId, ReviewStatus status);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.establishment.id = :establishmentId AND r.status = 'APPROVED'")
    Double getAverageRatingByEstablishmentId(@Param("establishmentId") Long establishmentId);
    
    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);
}
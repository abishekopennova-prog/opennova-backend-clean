package com.opennova.repository;

import com.opennova.model.Booking;
import com.opennova.model.Establishment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<Booking> findByEstablishmentInOrderByCreatedAtDesc(List<Establishment> establishments);
    
    List<Booking> findAllByOrderByCreatedAtDesc();
    
    long countByStatus(com.opennova.model.BookingStatus status);
    
    @Query("SELECT SUM(b.paymentAmount) FROM Booking b WHERE b.status = :status")
    java.math.BigDecimal sumPaidAmountByStatus(@Param("status") com.opennova.model.BookingStatus status);
    
    List<Booking> findByEstablishmentIdAndStatus(Long establishmentId, com.opennova.model.BookingStatus status);
    
    @Query("SELECT b FROM Booking b WHERE b.establishment.owner.id = :ownerId ORDER BY b.createdAt DESC")
    List<Booking> findByEstablishmentOwnerIdOrderByCreatedAtDesc(@Param("ownerId") Long ownerId);
    
    @Query("SELECT b FROM Booking b WHERE b.establishment.id = :establishmentId AND b.visitingTime = :visitingTime")
    List<Booking> findByEstablishmentIdAndVisitingTime(@Param("establishmentId") Long establishmentId, @Param("visitingTime") String visitingTime);
    
    List<Booking> findByEstablishmentIdOrderByCreatedAtDesc(Long establishmentId);
    
    List<Booking> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<Booking> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);
}
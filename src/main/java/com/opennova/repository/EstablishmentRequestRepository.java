package com.opennova.repository;

import com.opennova.model.EstablishmentRequest;
import com.opennova.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstablishmentRequestRepository extends JpaRepository<EstablishmentRequest, Long> {
    
    List<EstablishmentRequest> findByStatus(RequestStatus status);
    
    List<EstablishmentRequest> findByUser_Id(Long userId);
    
    List<EstablishmentRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);
    
    List<EstablishmentRequest> findAllByOrderByCreatedAtDesc();
    
    boolean existsByEmailAndStatus(String email, RequestStatus status);
    
    long countByStatus(RequestStatus status);
    
    List<EstablishmentRequest> findByUser_IdOrderByCreatedAtDesc(Long userId);
}
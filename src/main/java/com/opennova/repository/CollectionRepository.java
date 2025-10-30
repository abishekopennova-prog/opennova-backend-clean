package com.opennova.repository;

import com.opennova.model.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, Long> {
    
    @Query("SELECT c FROM Collection c WHERE c.establishment.id = :establishmentId AND c.isActive = true ORDER BY c.createdAt DESC")
    List<Collection> findActiveCollectionsByEstablishmentIdOrderByCreatedAtDesc(@Param("establishmentId") Long establishmentId);
    
    @Query("SELECT COUNT(c) FROM Collection c WHERE c.establishment.id = :establishmentId AND c.isActive = true")
    long countByEstablishmentIdAndIsActive(@Param("establishmentId") Long establishmentId);
    
    @Query("SELECT c FROM Collection c WHERE c.establishment.id = :establishmentId AND c.itemName = :itemName AND c.isActive = true")
    List<Collection> findByEstablishmentIdAndItemNameAndIsActive(@Param("establishmentId") Long establishmentId, @Param("itemName") String itemName);
    
    boolean existsByEstablishmentIdAndItemName(Long establishmentId, String itemName);
}
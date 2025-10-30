package com.opennova.repository;

import com.opennova.model.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {
    
    List<Menu> findByEstablishmentId(Long establishmentId);
    
    List<Menu> findByEstablishmentIdOrderByCreatedAtDesc(Long establishmentId);
    
    @Query("SELECT m FROM Menu m WHERE m.establishment.id = :establishmentId AND m.isActive = true")
    List<Menu> findActiveMenusByEstablishmentId(@Param("establishmentId") Long establishmentId);
    
    @Query("SELECT m FROM Menu m WHERE m.establishment.id = :establishmentId AND m.isActive = true ORDER BY m.createdAt DESC")
    List<Menu> findActiveMenusByEstablishmentIdOrderByCreatedAtDesc(@Param("establishmentId") Long establishmentId);
    
    long countByEstablishmentId(Long establishmentId);
    
    boolean existsByEstablishmentIdAndName(Long establishmentId, String name);
}
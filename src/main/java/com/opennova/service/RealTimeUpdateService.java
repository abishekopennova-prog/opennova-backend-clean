package com.opennova.service;

import com.opennova.model.Establishment;
import com.opennova.model.Booking;
import com.opennova.repository.EstablishmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;

@Service
public class RealTimeUpdateService {
    
    @Autowired
    private EstablishmentRepository establishmentRepository;
    
    @Autowired
    private SharedStateService sharedStateService;
    
    // Cache for real-time updates
    private final Map<Long, LocalDateTime> lastUpdateCache = new ConcurrentHashMap<>();
    
    @Transactional
    public void updateEstablishmentStatus(Long establishmentId, String status, Long ownerId) {
        try {
            Establishment establishment = establishmentRepository.findById(establishmentId)
                .orElseThrow(() -> new RuntimeException("Establishment not found"));
            
            // Verify ownership
            if (!establishment.getOwner().getId().equals(ownerId)) {
                throw new RuntimeException("Unauthorized to update this establishment");
            }
            
            // Update status
            com.opennova.model.EstablishmentStatus establishmentStatus = 
                com.opennova.model.EstablishmentStatus.valueOf(status.toUpperCase());
            establishment.setStatus(establishmentStatus);
            establishment.setUpdatedAt(LocalDateTime.now());
            
            // Save to database
            establishmentRepository.save(establishment);
            
            // Update shared state for real-time sync
            String stateKey = "establishment_" + establishmentId + "_status";
            sharedStateService.setState(stateKey, status);
            
            // Update cache
            lastUpdateCache.put(establishmentId, LocalDateTime.now());
            
            System.out.println("Updated establishment " + establishmentId + " status to: " + status);
            
        } catch (Exception e) {
            System.err.println("Failed to update establishment status: " + e.getMessage());
            throw new RuntimeException("Failed to update establishment status: " + e.getMessage());
        }
    }
    
    public String getEstablishmentStatus(Long establishmentId) {
        try {
            // First check shared state for real-time updates
            String stateKey = "establishment_" + establishmentId + "_status";
            String cachedStatus = sharedStateService.getState(stateKey, String.class);
            
            if (cachedStatus != null) {
                return cachedStatus;
            }
            
            // Fallback to database
            Establishment establishment = establishmentRepository.findById(establishmentId)
                .orElse(null);
            
            if (establishment != null) {
                String status = establishment.getStatus().toString();
                // Cache it for next time
                sharedStateService.setState(stateKey, status);
                return status;
            }
            
            return "UNKNOWN";
        } catch (Exception e) {
            System.err.println("Failed to get establishment status: " + e.getMessage());
            return "ERROR";
        }
    }
    
    public void syncEstablishmentData(Long establishmentId) {
        try {
            Establishment establishment = establishmentRepository.findById(establishmentId)
                .orElse(null);
            
            if (establishment != null) {
                // Update all cached data
                String statusKey = "establishment_" + establishmentId + "_status";
                String dataKey = "establishment_" + establishmentId + "_data";
                
                sharedStateService.setState(statusKey, establishment.getStatus().toString());
                sharedStateService.setState(dataKey, establishment);
                
                lastUpdateCache.put(establishmentId, LocalDateTime.now());
            }
        } catch (Exception e) {
            System.err.println("Failed to sync establishment data: " + e.getMessage());
        }
    }
    
    public void notifyBookingUpdate(Booking booking) {
        try {
            Long establishmentId = booking.getEstablishment().getId();
            String notificationKey = "booking_update_" + establishmentId;
            
            Map<String, Object> updateData = new ConcurrentHashMap<>();
            updateData.put("bookingId", booking.getId());
            updateData.put("status", booking.getStatus().toString());
            updateData.put("timestamp", LocalDateTime.now());
            updateData.put("establishmentId", establishmentId);
            
            sharedStateService.setState(notificationKey, updateData);
            
            System.out.println("Notified booking update for establishment: " + establishmentId);
        } catch (Exception e) {
            System.err.println("Failed to notify booking update: " + e.getMessage());
        }
    }
    
    public boolean hasRecentUpdates(Long establishmentId, LocalDateTime since) {
        LocalDateTime lastUpdate = lastUpdateCache.get(establishmentId);
        return lastUpdate != null && lastUpdate.isAfter(since);
    }
    
    public void clearCache(Long establishmentId) {
        lastUpdateCache.remove(establishmentId);
        
        // Clear shared state
        String statusKey = "establishment_" + establishmentId + "_status";
        String dataKey = "establishment_" + establishmentId + "_data";
        String notificationKey = "booking_update_" + establishmentId;
        
        sharedStateService.removeState(statusKey);
        sharedStateService.removeState(dataKey);
        sharedStateService.removeState(notificationKey);
    }
    
    public void refreshAllEstablishments() {
        try {
            List<Establishment> establishments = establishmentRepository.findAll();
            for (Establishment establishment : establishments) {
                syncEstablishmentData(establishment.getId());
            }
            System.out.println("Refreshed " + establishments.size() + " establishments");
        } catch (Exception e) {
            System.err.println("Failed to refresh all establishments: " + e.getMessage());
        }
    }
    
    public void notifyEstablishmentStatusUpdate(Establishment establishment) {
        try {
            Long establishmentId = establishment.getId();
            String notificationKey = "status_update_" + establishmentId;
            
            Map<String, Object> updateData = new ConcurrentHashMap<>();
            updateData.put("establishmentId", establishmentId);
            updateData.put("status", establishment.getStatus().toString());
            updateData.put("timestamp", LocalDateTime.now());
            updateData.put("name", establishment.getName());
            
            sharedStateService.setState(notificationKey, updateData);
            
            // Also update the main status cache
            String statusKey = "establishment_" + establishmentId + "_status";
            sharedStateService.setState(statusKey, establishment.getStatus().toString());
            
            System.out.println("Notified status update for establishment: " + establishment.getName());
        } catch (Exception e) {
            System.err.println("Failed to notify establishment status update: " + e.getMessage());
        }
    }
    
    public void notifyEstablishmentUpdate(Establishment establishment) {
        try {
            Long establishmentId = establishment.getId();
            String notificationKey = "establishment_update_" + establishmentId;
            
            Map<String, Object> updateData = new ConcurrentHashMap<>();
            updateData.put("establishmentId", establishmentId);
            updateData.put("name", establishment.getName());
            updateData.put("operatingHours", establishment.getOperatingHours());
            updateData.put("weeklySchedule", establishment.getWeeklySchedule());
            updateData.put("status", establishment.getStatus().toString());
            updateData.put("timestamp", LocalDateTime.now());
            
            sharedStateService.setState(notificationKey, updateData);
            
            // Also update the main establishment data cache
            String dataKey = "establishment_" + establishmentId + "_data";
            sharedStateService.setState(dataKey, establishment);
            
            // Update the public establishments list cache to force refresh
            String publicListKey = "public_establishments_list";
            sharedStateService.removeState(publicListKey); // Force refresh on next request
            
            System.out.println("Notified establishment update (including operating hours) for: " + establishment.getName());
        } catch (Exception e) {
            System.err.println("Failed to notify establishment update: " + e.getMessage());
        }
    }
}
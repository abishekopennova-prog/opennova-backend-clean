package com.opennova.service;

import com.opennova.model.SavedEstablishment;
import com.opennova.model.Establishment;
import com.opennova.model.User;
import com.opennova.model.EstablishmentType;
import com.opennova.repository.SavedEstablishmentRepository;
import com.opennova.repository.EstablishmentRepository;
import com.opennova.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SavedEstablishmentService {

    @Autowired
    private SavedEstablishmentRepository savedEstablishmentRepository;

    @Autowired
    private EstablishmentRepository establishmentRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Save an establishment for a user
     */
    public SavedEstablishment saveEstablishment(Long userId, Long establishmentId) {
        // Check if already saved
        if (savedEstablishmentRepository.existsByUserIdAndEstablishmentId(userId, establishmentId)) {
            throw new RuntimeException("Establishment already saved by user");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Establishment establishment = establishmentRepository.findById(establishmentId)
                .orElseThrow(() -> new RuntimeException("Establishment not found"));

        SavedEstablishment savedEstablishment = new SavedEstablishment(user, establishment);
        return savedEstablishmentRepository.save(savedEstablishment);
    }

    /**
     * Remove saved establishment
     */
    public void removeSavedEstablishment(Long userId, Long establishmentId) {
        Optional<SavedEstablishment> savedEstablishment = 
                savedEstablishmentRepository.findByUserIdAndEstablishmentId(userId, establishmentId);
        
        if (savedEstablishment.isPresent()) {
            savedEstablishmentRepository.delete(savedEstablishment.get());
        } else {
            throw new RuntimeException("Saved establishment not found");
        }
    }

    /**
     * Get all saved establishments for a user
     */
    public List<SavedEstablishment> getSavedEstablishments(Long userId) {
        return savedEstablishmentRepository.findByUserIdOrderBySavedAtDesc(userId);
    }

    /**
     * Get saved establishments by type for a user
     */
    public List<SavedEstablishment> getSavedEstablishmentsByType(Long userId, EstablishmentType type) {
        return savedEstablishmentRepository.findByUserIdAndEstablishmentType(userId, type);
    }

    /**
     * Check if establishment is saved by user
     */
    public boolean isEstablishmentSaved(Long userId, Long establishmentId) {
        return savedEstablishmentRepository.existsByUserIdAndEstablishmentId(userId, establishmentId);
    }

    /**
     * Get count of saved establishments for a user
     */
    public long getSavedEstablishmentsCount(Long userId) {
        return savedEstablishmentRepository.countByUserId(userId);
    }

    /**
     * Get count of saved establishments by type for a user
     */
    public long getSavedEstablishmentsCountByType(Long userId, EstablishmentType type) {
        return savedEstablishmentRepository.countByUserIdAndEstablishmentType(userId, type);
    }

    /**
     * Get users who saved a specific establishment (for owner portal)
     */
    public List<SavedEstablishment> getUsersWhoSavedEstablishment(Long establishmentId) {
        return savedEstablishmentRepository.findByEstablishmentId(establishmentId);
    }

    /**
     * Toggle save status of an establishment
     */
    public boolean toggleSaveEstablishment(Long userId, Long establishmentId) {
        if (isEstablishmentSaved(userId, establishmentId)) {
            removeSavedEstablishment(userId, establishmentId);
            return false; // Removed
        } else {
            saveEstablishment(userId, establishmentId);
            return true; // Added
        }
    }

    /**
     * Get all saved establishments for a user (alias method for UserController compatibility)
     */
    public List<SavedEstablishment> getUserSavedEstablishments(Long userId) {
        return getSavedEstablishments(userId);
    }
}
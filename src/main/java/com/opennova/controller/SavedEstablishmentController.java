package com.opennova.controller;

import com.opennova.model.SavedEstablishment;
import com.opennova.model.EstablishmentType;
import com.opennova.service.SavedEstablishmentService;
import com.opennova.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/saved-establishments")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3002", "http://127.0.0.1:3002", "http://localhost:3003", "http://127.0.0.1:3003"})
public class SavedEstablishmentController {

    @Autowired
    private SavedEstablishmentService savedEstablishmentService;

    @Autowired
    private JwtUtil jwtUtil;

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                if (!jwtUtil.validateToken(token)) {
                    throw new RuntimeException("Invalid or expired token");
                }
                return jwtUtil.extractUserId(token);
            } catch (Exception e) {
                throw new RuntimeException("Invalid or expired token: " + e.getMessage());
            }
        }
        throw new RuntimeException("Authorization header missing or invalid");
    }

    /**
     * Save an establishment
     */
    @PostMapping("/{establishmentId}")
    public ResponseEntity<?> saveEstablishment(@PathVariable Long establishmentId, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            SavedEstablishment savedEstablishment = savedEstablishmentService.saveEstablishment(userId, establishmentId);
            return ResponseEntity.ok(Map.of(
                "message", "Establishment saved successfully",
                "saved", true,
                "savedEstablishment", savedEstablishment
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", e.getMessage(),
                "saved", false
            ));
        }
    }

    /**
     * Remove saved establishment
     */
    @DeleteMapping("/{establishmentId}")
    public ResponseEntity<?> removeSavedEstablishment(@PathVariable Long establishmentId, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            savedEstablishmentService.removeSavedEstablishment(userId, establishmentId);
            return ResponseEntity.ok(Map.of(
                "message", "Establishment removed from saved list",
                "saved", false
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", e.getMessage(),
                "saved", true
            ));
        }
    }

    /**
     * Toggle save status
     */
    @PostMapping("/{establishmentId}/toggle")
    public ResponseEntity<?> toggleSaveEstablishment(@PathVariable Long establishmentId, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            boolean isSaved = savedEstablishmentService.toggleSaveEstablishment(userId, establishmentId);
            return ResponseEntity.ok(Map.of(
                "message", isSaved ? "Establishment saved successfully" : "Establishment removed from saved list",
                "saved", isSaved
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get all saved establishments
     */
    @GetMapping
    public ResponseEntity<?> getSavedEstablishments(HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            List<SavedEstablishment> savedEstablishments = savedEstablishmentService.getSavedEstablishments(userId);
            return ResponseEntity.ok(savedEstablishments);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                "message", "Authentication required: " + e.getMessage(),
                "error", "UNAUTHORIZED"
            ));
        }
    }

    /**
     * Get saved establishments by type
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<SavedEstablishment>> getSavedEstablishmentsByType(
            @PathVariable EstablishmentType type, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            List<SavedEstablishment> savedEstablishments = 
                    savedEstablishmentService.getSavedEstablishmentsByType(userId, type);
            return ResponseEntity.ok(savedEstablishments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Check if establishment is saved
     */
    @GetMapping("/{establishmentId}/status")
    public ResponseEntity<?> checkSaveStatus(@PathVariable Long establishmentId, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            boolean isSaved = savedEstablishmentService.isEstablishmentSaved(userId, establishmentId);
            return ResponseEntity.ok(Map.of("saved", isSaved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("saved", false));
        }
    }

    /**
     * Get saved establishments count
     */
    @GetMapping("/count")
    public ResponseEntity<?> getSavedEstablishmentsCount(HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            long count = savedEstablishmentService.getSavedEstablishmentsCount(userId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("count", 0));
        }
    }
}
package com.opennova.controller;

import com.opennova.dto.AuthResponse;
import com.opennova.dto.LoginRequest;
import com.opennova.dto.RegisterRequest;
import com.opennova.model.User;
import com.opennova.security.JwtUtil;
import com.opennova.service.AuthService;
import com.opennova.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3002", "http://127.0.0.1:3002", "http://localhost:3003", "http://127.0.0.1:3003"}, maxAge = 3600)
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private com.opennova.service.EstablishmentService establishmentService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            System.out.println("=== Login Request Received ===");
            
            // Validate input
            if (loginRequest == null) {
                System.err.println("Login request is null");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Login request is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            System.out.println("Login request data: email=" + loginRequest.getEmail() + ", password=" + (loginRequest.getPassword() != null ? "[PROVIDED]" : "[NULL]"));
            
            if (loginRequest.getEmail() == null || loginRequest.getEmail().trim().isEmpty()) {
                System.err.println("Email is null or empty");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Email is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
                System.err.println("Password is null or empty");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Password is required");
                return ResponseEntity.badRequest().body(error);
            }

            String email = loginRequest.getEmail().trim();
            System.out.println("Login attempt for email: " + email);

            // Check if user exists first
            User existingUser = userService.findByEmailSafe(email);
            if (existingUser == null) {
                System.err.println("User not found in database: " + email);
                Map<String, String> error = new HashMap<>();
                error.put("message", "Invalid email or password");
                return ResponseEntity.badRequest().body(error);
            }
            
            System.out.println("User found: " + existingUser.getName() + " with role: " + existingUser.getRole());

            // Ensure establishment assignment for owner roles
            if (existingUser.getRole() == com.opennova.model.UserRole.HOSPITAL_OWNER ||
                existingUser.getRole() == com.opennova.model.UserRole.SHOP_OWNER ||
                existingUser.getRole() == com.opennova.model.UserRole.HOTEL_OWNER) {
                
                try {
                    establishmentService.assignEstablishmentToUser(existingUser);
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to ensure establishment assignment during login: " + e.getMessage());
                    // Don't fail login if establishment assignment fails
                }
            }

            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, loginRequest.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            System.out.println("Authentication successful for: " + userDetails.getUsername());
            
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", existingUser.getRole().name());
            claims.put("userId", existingUser.getId());
            
            String jwt = jwtUtil.generateToken(userDetails, claims);
            System.out.println("JWT token generated successfully");

            // Get establishment type for owner roles
            String establishmentType = null;
            if (existingUser.getRole() == com.opennova.model.UserRole.HOSPITAL_OWNER ||
                existingUser.getRole() == com.opennova.model.UserRole.SHOP_OWNER ||
                existingUser.getRole() == com.opennova.model.UserRole.HOTEL_OWNER ||
                existingUser.getRole() == com.opennova.model.UserRole.OWNER) {
                
                try {
                    com.opennova.model.Establishment establishment = establishmentService.findByOwner(existingUser);
                    if (establishment != null && establishment.getType() != null) {
                        establishmentType = establishment.getType().toString();
                    }
                } catch (Exception e) {
                    System.err.println("Failed to get establishment type: " + e.getMessage());
                }
            }

            AuthResponse.UserResponse userResponse = new AuthResponse.UserResponse(
                existingUser.getId(),
                existingUser.getName(),
                existingUser.getEmail(),
                existingUser.getRole(),
                establishmentType
            );

            System.out.println("Login successful for user: " + existingUser.getEmail() + " with role: " + existingUser.getRole());
            return ResponseEntity.ok(new AuthResponse(jwt, userResponse));
            
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            System.err.println("Bad credentials for email: " + (loginRequest != null ? loginRequest.getEmail() : "null"));
            System.err.println("BadCredentialsException details: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("message", "Invalid email or password");
            return ResponseEntity.badRequest().body(error);
        } catch (org.springframework.security.authentication.DisabledException e) {
            System.err.println("Account disabled: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("message", "Account is disabled");
            return ResponseEntity.badRequest().body(error);
        } catch (org.springframework.security.authentication.LockedException e) {
            System.err.println("Account locked: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("message", "Account is locked");
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            System.err.println("Login error: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Login failed. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            if (userService.existsByEmail(registerRequest.getEmail())) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Email is already registered");
                return ResponseEntity.badRequest().body(error);
            }

            User user = authService.registerUser(registerRequest);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "User registered successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Registration failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            
            if (email == null || email.trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Email is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            authService.sendPasswordResetEmail(email.trim());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password reset link sent to your email");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to send reset email: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            String newPassword = request.get("password");
            
            authService.resetPassword(token, newPassword);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password reset successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Password reset failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/assign-establishment")
    public ResponseEntity<?> assignEstablishment() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            String email = authentication.getName();
            User user = userService.findByEmailSafe(email);
            if (user == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(404).body(error);
            }
            
            com.opennova.model.Establishment establishment = establishmentService.assignEstablishmentToUser(user);
            
            if (establishment != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Establishment assigned successfully");
                response.put("establishment", establishment.getName());
                response.put("type", establishment.getType());
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("message", "No available establishment found for your role");
                return ResponseEntity.badRequest().body(error);
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to assign establishment: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile() {
        try {
            System.out.println("=== Profile Endpoint Called ===");
            
            // Check authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                System.err.println("No authentication found in profile endpoint");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            String email = authentication.getName();
            if (email == null || email.trim().isEmpty()) {
                System.err.println("No email found in authentication");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Invalid authentication token");
                return ResponseEntity.status(401).body(error);
            }
            
            System.out.println("Fetching profile for user: " + email);
            
            // Get user with safe method
            User user = null;
            try {
                user = userService.findByEmailSafe(email);
            } catch (Exception e) {
                System.err.println("Error finding user by email: " + e.getMessage());
                Map<String, String> error = new HashMap<>();
                error.put("message", "Database error while fetching user profile");
                return ResponseEntity.status(500).body(error);
            }
            
            if (user == null) {
                System.err.println("User not found in database: " + email);
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(404).body(error);
            }
            
            System.out.println("User found: " + user.getName() + " with role: " + user.getRole().name());
            
            // Get establishment type for owner roles
            String establishmentType = null;
            if (user.getRole() == com.opennova.model.UserRole.HOSPITAL_OWNER ||
                user.getRole() == com.opennova.model.UserRole.SHOP_OWNER ||
                user.getRole() == com.opennova.model.UserRole.HOTEL_OWNER ||
                user.getRole() == com.opennova.model.UserRole.OWNER) {
                
                try {
                    com.opennova.model.Establishment establishment = establishmentService.findByOwner(user);
                    if (establishment != null && establishment.getType() != null) {
                        establishmentType = establishment.getType().toString();
                    }
                } catch (Exception e) {
                    System.err.println("Failed to get establishment type for profile: " + e.getMessage());
                }
            }
            
            AuthResponse.UserResponse userResponse = new AuthResponse.UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                establishmentType
            );
            
            return ResponseEntity.ok(userResponse);
        } catch (Exception e) {
            System.err.println("Error in getUserProfile: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to get user profile: " + e.getMessage());
            error.put("error", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(error);
        }
    }
}
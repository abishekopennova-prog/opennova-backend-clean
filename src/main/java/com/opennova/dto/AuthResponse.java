package com.opennova.dto;

import com.opennova.model.UserRole;

public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private UserResponse user;

    // Constructors
    public AuthResponse() {}

    public AuthResponse(String token, UserResponse user) {
        this.token = token;
        this.user = user;
    }

    // Getters and Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public UserResponse getUser() { return user; }
    public void setUser(UserResponse user) { this.user = user; }

    // Inner class for user response
    public static class UserResponse {
        private Long id;
        private String name;
        private String email;
        private UserRole role;
        private String establishmentType;

        public UserResponse() {}

        public UserResponse(Long id, String name, String email, UserRole role) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.role = role;
        }

        public UserResponse(Long id, String name, String email, UserRole role, String establishmentType) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.role = role;
            this.establishmentType = establishmentType;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public UserRole getRole() { return role; }
        public void setRole(UserRole role) { this.role = role; }

        public String getEstablishmentType() { return establishmentType; }
        public void setEstablishmentType(String establishmentType) { this.establishmentType = establishmentType; }
    }
}
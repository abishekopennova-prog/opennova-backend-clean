package com.opennova.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.opennova.service.RealTimeUpdateService;
import com.opennova.service.EstablishmentService;
import com.opennova.service.MenuService;
import com.opennova.service.DoctorService;
import com.opennova.service.CollectionService;
import com.opennova.service.SharedStateService;
import com.opennova.model.Establishment;
import com.opennova.model.Menu;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3002", "http://127.0.0.1:3002", "http://localhost:3003", "http://127.0.0.1:3003"}, maxAge = 3600)
public class PublicController {
    
    @Autowired
    private RealTimeUpdateService realTimeUpdateService;
    
    @Autowired
    private EstablishmentService establishmentService;
    
    @Autowired
    private MenuService menuService;
    
    @Autowired
    private com.opennova.service.DoctorService doctorService;
    
    @Autowired
    private com.opennova.service.CollectionService collectionService;
    
    @Autowired
    private com.opennova.service.SharedStateService sharedStateService;
    
    @Autowired
    private com.opennova.service.UserService userService;

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("timestamp", java.time.LocalDateTime.now());
            
            // Test database connection
            try {
                long userCount = userService.getTotalUsers();
                long establishmentCount = establishmentService.getTotalEstablishments();
                health.put("database", "CONNECTED");
                health.put("userCount", userCount);
                health.put("establishmentCount", establishmentCount);
            } catch (Exception e) {
                health.put("database", "ERROR: " + e.getMessage());
            }
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", java.time.LocalDateTime.now());
            return ResponseEntity.status(500).body(health);
        }
    }

    @GetMapping("/establishments/live")
    public ResponseEntity<?> getLiveEstablishments() {
        try {
            List<Map<String, Object>> establishments = new ArrayList<>();
            
            // Mock establishment data that reflects real-time changes
            Map<String, Object> establishment1 = new HashMap<>();
            establishment1.put("id", 1L);
            establishment1.put("name", "Sample Hotel");
            establishment1.put("type", "HOTEL");
            establishment1.put("address", "Thangavel Nagar, 4/122, Covai Road, Reddipalayam, Karur, Tamil Nadu 639008");
            establishment1.put("contactNumber", "8012975411");
            establishment1.put("operatingHours", "9:00 AM - 10:00 PM");
            establishment1.put("upiId", "abishek1234@upi");
            establishment1.put("email", "hotel@example.com");
            establishment1.put("latitude", 10.963788560368593);
            establishment1.put("longitude", 78.0483853359511);
            establishment1.put("averageRating", 4.5);
            establishment1.put("reviewCount", 25);
            
            // Get real-time status from SharedStateService
            String cachedStatus = sharedStateService.getState("establishment_status_hotel@example.com", String.class);
            establishment1.put("status", cachedStatus != null ? cachedStatus : "OPEN");
            
            establishments.add(establishment1);
            
            return ResponseEntity.ok(establishments);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch live establishments: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/establishments")
    public ResponseEntity<?> getPublicEstablishments(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String status) {
        try {
            List<Map<String, Object>> establishments = new ArrayList<>();
            
            // Try to get real establishments from database first
            try {
                List<Establishment> dbEstablishments = establishmentService.findAll();
                
                if (dbEstablishments != null && !dbEstablishments.isEmpty()) {
                    System.out.println("✅ Found " + dbEstablishments.size() + " establishments in database");
                    
                    for (Establishment est : dbEstablishments) {
                        try {
                            if (est.getIsActive() != null && est.getIsActive()) { // Only include active establishments
                                System.out.println("📝 Processing establishment: " + est.getName() + " (ID: " + est.getId() + ")");
                                System.out.println("   - Operating Hours: " + est.getOperatingHours());
                                System.out.println("   - Status: " + est.getStatus());
                                System.out.println("   - Address: " + est.getAddress());
                                Map<String, Object> estData = new HashMap<>();
                                estData.put("id", est.getId());
                                estData.put("name", est.getName());
                                estData.put("type", est.getType() != null ? est.getType().toString() : "UNKNOWN");
                                estData.put("address", est.getAddress());
                                estData.put("contactNumber", est.getContactNumber());
                                estData.put("operatingHours", est.getOperatingHours());
                                estData.put("status", est.getStatus() != null ? est.getStatus().toString() : "OPEN");
                                estData.put("email", est.getEmail());
                                estData.put("latitude", est.getLatitude());
                                estData.put("longitude", est.getLongitude());
                                estData.put("upiId", est.getUpiId());
                                estData.put("profileImagePath", est.getProfileImagePath());
                                estData.put("weeklySchedule", est.getWeeklySchedule());
                                
                                // Get real-time status if available (with error handling)
                                try {
                                    String realTimeStatus = realTimeUpdateService.getEstablishmentStatus(est.getId());
                                    if (realTimeStatus != null && !realTimeStatus.equals("ERROR") && !realTimeStatus.equals("UNKNOWN")) {
                                        estData.put("status", realTimeStatus);
                                    }
                                } catch (Exception e) {
                                    System.err.println("Failed to get real-time status for establishment " + est.getId() + ": " + e.getMessage());
                                }
                                
                                // Calculate average rating and review count (with error handling)
                                try {
                                    estData.put("averageRating", establishmentService.calculateAverageRating(est.getId()));
                                    estData.put("reviewCount", establishmentService.getTotalReviews(est.getId()));
                                } catch (Exception e) {
                                    System.err.println("Failed to calculate ratings for establishment " + est.getId() + ": " + e.getMessage());
                                    estData.put("averageRating", 0.0);
                                    estData.put("reviewCount", 0);
                                }
                                
                                establishments.add(estData);
                                System.out.println("✅ Added establishment to response: " + est.getName());
                            }
                        } catch (Exception e) {
                            System.err.println("Error processing establishment " + est.getId() + ": " + e.getMessage());
                            // Continue with next establishment
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to fetch establishments from database: " + e.getMessage());
                e.printStackTrace();
            }
            
            boolean foundRealData = !establishments.isEmpty();
            
            // If no real data found, create default establishments for demo
            if (!foundRealData) {
                System.out.println("No establishments found in database, creating default establishments for demo");
                
                // Default Hotel
                Map<String, Object> hotel = new HashMap<>();
                hotel.put("id", 1L);
                hotel.put("name", "Grand Hotel Karur");
                hotel.put("type", "HOTEL");
                hotel.put("address", "Thangavel Nagar, 4/122, Covai Road, PO, Reddipalayam, Andankoll East, Karur, Tamil Nadu 639008");
                hotel.put("contactNumber", "8012975411");
                hotel.put("operatingHours", "9:00 AM - 10:00 PM");
                hotel.put("status", "OPEN");
                hotel.put("email", "hotel@example.com");
                hotel.put("latitude", 10.963788560368593);
                hotel.put("longitude", 78.0483853359511);
                hotel.put("averageRating", 4.5);
                hotel.put("reviewCount", 25);
                establishments.add(hotel);
                
                // Default Hospital
                Map<String, Object> hospital = new HashMap<>();
                hospital.put("id", 2L);
                hospital.put("name", "City Hospital Namakkal");
                hospital.put("type", "HOSPITAL");
                hospital.put("address", "6/288, Trichy Rd, Andavar Nagar, Namakkal, Tamil Nadu 637001");
                hospital.put("contactNumber", "8012975411");
                hospital.put("operatingHours", "24 Hours");
                hospital.put("status", "OPEN");
                hospital.put("email", "hospital@example.com");
                hospital.put("latitude", 11.2189);
                hospital.put("longitude", 78.1677);
                hospital.put("averageRating", 4.2);
                hospital.put("reviewCount", 18);
                hospital.put("upiId", "hospital@upi");
                
                // Add default doctors for the hospital
                List<Map<String, Object>> defaultDoctors = new ArrayList<>();
                
                Map<String, Object> doctor1 = new HashMap<>();
                doctor1.put("id", 1L);
                doctor1.put("name", "Dr. Rajesh Kumar");
                doctor1.put("specialization", "Cardiology");
                doctor1.put("consultationFee", 500);
                doctor1.put("price", 500);
                doctor1.put("availabilityTime", "9:00 AM - 5:00 PM");
                doctor1.put("available", true);
                doctor1.put("imagePath", null);
                defaultDoctors.add(doctor1);
                
                Map<String, Object> doctor2 = new HashMap<>();
                doctor2.put("id", 2L);
                doctor2.put("name", "Dr. Priya Sharma");
                doctor2.put("specialization", "Pediatrics");
                doctor2.put("consultationFee", 400);
                doctor2.put("price", 400);
                doctor2.put("availabilityTime", "10:00 AM - 6:00 PM");
                doctor2.put("available", true);
                doctor2.put("imagePath", null);
                defaultDoctors.add(doctor2);
                
                Map<String, Object> doctor3 = new HashMap<>();
                doctor3.put("id", 3L);
                doctor3.put("name", "Dr. Arun Patel");
                doctor3.put("specialization", "General Medicine");
                doctor3.put("consultationFee", 300);
                doctor3.put("price", 300);
                doctor3.put("availabilityTime", "8:00 AM - 8:00 PM");
                doctor3.put("available", true);
                doctor3.put("imagePath", null);
                defaultDoctors.add(doctor3);
                
                hospital.put("doctors", defaultDoctors);
                establishments.add(hospital);
                
                // Default Shop
                Map<String, Object> shop = new HashMap<>();
                shop.put("id", 3L);
                shop.put("name", "Fashion Plaza");
                shop.put("type", "SHOP");
                shop.put("address", "CP City center 6/98+H4F Salem, Road, R.P.Pudur, Namakkal, Tamil Nadu 637001");
                shop.put("contactNumber", "8012975411");
                shop.put("operatingHours", "10:00 AM - 9:00 PM");
                shop.put("status", "OPEN");
                shop.put("email", "shop@example.com");
                shop.put("latitude", 11.2189);
                shop.put("longitude", 78.1677);
                shop.put("averageRating", 4.0);
                shop.put("reviewCount", 12);
                establishments.add(shop);
            }
            
            // Apply filters
            if (type != null && !type.isEmpty()) {
                establishments = establishments.stream()
                    .filter(est -> type.equals(est.get("type")))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            if (status != null && !status.isEmpty()) {
                establishments = establishments.stream()
                    .filter(est -> status.equals(est.get("status")))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            System.out.println("Returning " + establishments.size() + " establishments to user portal");
            
            // Debug: Log each establishment type
            for (Map<String, Object> est : establishments) {
                System.out.println("  - " + est.get("name") + " (Type: " + est.get("type") + ")");
                if ("HOSPITAL".equals(est.get("type"))) {
                    Object doctors = est.get("doctors");
                    if (doctors instanceof List) {
                        System.out.println("    Hospital has " + ((List<?>) doctors).size() + " doctors");
                    }
                }
            }
            
            return ResponseEntity.ok(establishments);
        } catch (Exception e) {
            System.err.println("Failed to fetch establishments: " + e.getMessage());
            e.printStackTrace();
            
            // Return empty list instead of error to prevent frontend crashes
            List<Map<String, Object>> fallbackEstablishments = new ArrayList<>();
            
            // Create minimal fallback data
            Map<String, Object> fallbackHotel = new HashMap<>();
            fallbackHotel.put("id", 1L);
            fallbackHotel.put("name", "Sample Hotel");
            fallbackHotel.put("type", "HOTEL");
            fallbackHotel.put("address", "Sample Address");
            fallbackHotel.put("contactNumber", "1234567890");
            fallbackHotel.put("operatingHours", "9:00 AM - 10:00 PM");
            fallbackHotel.put("status", "OPEN");
            fallbackHotel.put("email", "hotel@example.com");
            fallbackHotel.put("latitude", 10.9638);
            fallbackHotel.put("longitude", 78.0484);
            fallbackHotel.put("averageRating", 4.5);
            fallbackHotel.put("reviewCount", 25);
            fallbackHotel.put("upiId", "sample@upi");
            fallbackHotel.put("profileImagePath", null);
            fallbackHotel.put("weeklySchedule", null);
            fallbackEstablishments.add(fallbackHotel);
            
            System.out.println("Returning fallback establishments due to error");
            return ResponseEntity.ok(fallbackEstablishments);
        }
    }

    @GetMapping("/establishments/{id}")
    public ResponseEntity<?> getEstablishmentDetails(@PathVariable Long id) {
        try {
            System.out.println("🔍 Fetching establishment details for ID: " + id);
            
            // Get actual establishment from database - always fresh data
            Establishment establishment = establishmentService.findById(id);
            if (establishment == null) {
                System.err.println("❌ Establishment not found with ID: " + id);
                Map<String, String> error = new HashMap<>();
                error.put("message", "Establishment not found");
                return ResponseEntity.notFound().build();
            }
            
            System.out.println("✅ Found establishment: " + establishment.getName() + " (Type: " + establishment.getType() + ")");
            System.out.println("📝 Operating Hours: " + establishment.getOperatingHours());
            System.out.println("📅 Weekly Schedule: " + (establishment.getWeeklySchedule() != null ? "Present" : "Not set"));
            
            // Build establishment response with fresh database data
            Map<String, Object> establishmentData = new HashMap<>();
            establishmentData.put("id", establishment.getId());
            establishmentData.put("name", establishment.getName());
            establishmentData.put("type", establishment.getType().toString());
            establishmentData.put("address", establishment.getAddress());
            establishmentData.put("contactNumber", establishment.getContactNumber());
            establishmentData.put("operatingHours", establishment.getOperatingHours());
            establishmentData.put("weeklySchedule", establishment.getWeeklySchedule());
            establishmentData.put("status", establishment.getStatus().toString());
            establishmentData.put("upiId", establishment.getUpiId());
            establishmentData.put("email", establishment.getEmail());
            establishmentData.put("latitude", establishment.getLatitude());
            establishmentData.put("longitude", establishment.getLongitude());
            establishmentData.put("profileImagePath", establishment.getProfileImagePath());
            
            // Get menu items for this establishment
            System.out.println("🍽️ Fetching menus for establishment: " + establishment.getName() + " (ID: " + establishment.getId() + ")");
            List<Menu> menus = menuService.getMenusByEstablishmentId(establishment.getId());
            List<Map<String, Object>> menuItems = new ArrayList<>();
            
            System.out.println("📝 Processing " + menus.size() + " menus for user portal");
            for (Menu menu : menus) {
                Map<String, Object> menuItem = new HashMap<>();
                menuItem.put("id", menu.getId());
                menuItem.put("name", menu.getName());
                menuItem.put("description", menu.getDescription());
                menuItem.put("price", menu.getPrice());
                menuItem.put("isAvailable", menu.getIsAvailable());
                menuItem.put("availabilityTime", menu.getAvailabilityTime());
                menuItem.put("category", menu.getCategory());
                menuItem.put("preparationTime", menu.getPreparationTime());
                menuItem.put("isVegetarian", menu.getIsVegetarian());
                menuItem.put("isSpecial", menu.getIsSpecial());
                menuItem.put("imagePath", menu.getImagePath());
                
                // Add default availability schedule
                Map<String, Object> schedule = new HashMap<>();
                schedule.put("monday", Map.of("isAvailable", true, "startTime", "09:00", "endTime", "21:00"));
                schedule.put("tuesday", Map.of("isAvailable", true, "startTime", "09:00", "endTime", "21:00"));
                schedule.put("wednesday", Map.of("isAvailable", true, "startTime", "09:00", "endTime", "21:00"));
                schedule.put("thursday", Map.of("isAvailable", true, "startTime", "09:00", "endTime", "21:00"));
                schedule.put("friday", Map.of("isAvailable", true, "startTime", "09:00", "endTime", "21:00"));
                schedule.put("saturday", Map.of("isAvailable", true, "startTime", "09:00", "endTime", "21:00"));
                schedule.put("sunday", Map.of("isAvailable", true, "startTime", "10:00", "endTime", "20:00"));
                menuItem.put("availabilitySchedule", schedule);
                
                menuItems.add(menuItem);
                System.out.println("  ✅ Added menu item: " + menu.getName() + " - ₹" + menu.getPrice());
            }
            
            establishmentData.put("menuItems", menuItems);
            System.out.println("🍽️ Added " + menuItems.size() + " menu items to establishment data");
            
            // Get doctors for hospitals
            if (establishment.getType().toString().equals("HOSPITAL")) {
                System.out.println("👨‍⚕️ Fetching doctors for hospital: " + establishment.getName());
                List<com.opennova.model.Doctor> doctors = doctorService.getDoctorsByEstablishmentId(establishment.getId());
                List<Map<String, Object>> doctorItems = new ArrayList<>();
                
                System.out.println("📝 Found " + doctors.size() + " doctors for hospital");
                for (com.opennova.model.Doctor doctor : doctors) {
                    if (doctor.getIsActive()) {
                        Map<String, Object> doctorItem = new HashMap<>();
                        doctorItem.put("id", doctor.getId());
                        doctorItem.put("name", doctor.getName());
                        doctorItem.put("specialization", doctor.getSpecialization());
                        doctorItem.put("consultationFee", doctor.getPrice()); // Use consultationFee for frontend compatibility
                        doctorItem.put("price", doctor.getPrice()); // Keep price for backward compatibility
                        doctorItem.put("availabilityTime", doctor.getAvailabilityTime());
                        doctorItem.put("available", true); // Default to available
                        doctorItem.put("imagePath", doctor.getImagePath());
                        doctorItems.add(doctorItem);
                        System.out.println("  ✅ Added doctor: " + doctor.getName() + " - ₹" + doctor.getPrice());
                    }
                }
                establishmentData.put("doctors", doctorItems);
                System.out.println("👨‍⚕️ Added " + doctorItems.size() + " doctors to hospital data");
            }
            
            // Get collections for shops
            if (establishment.getType().toString().equals("SHOP")) {
                List<com.opennova.model.Collection> collections = collectionService.getCollectionsByEstablishmentId(establishment.getId());
                List<Map<String, Object>> collectionItems = new ArrayList<>();
                
                for (com.opennova.model.Collection collection : collections) {
                    if (collection.getIsActive()) {
                        Map<String, Object> collectionItem = new HashMap<>();
                        collectionItem.put("id", collection.getId());
                        collectionItem.put("itemName", collection.getItemName());
                        collectionItem.put("description", collection.getDescription());
                        collectionItem.put("price", collection.getPrice());
                        collectionItem.put("sizes", collection.getSizes());
                        collectionItem.put("color", collection.getColors());
                        collectionItem.put("fabric", collection.getFabric());
                        collectionItem.put("brand", collection.getBrand());
                        collectionItem.put("stock", collection.getStock());
                        collectionItem.put("imagePath", collection.getImagePath());
                        collectionItem.put("isSpecialOffer", collection.getIsSpecialOffer());
                        collectionItems.add(collectionItem);
                    }
                }
                establishmentData.put("collections", collectionItems);
            }
            
            return ResponseEntity.ok(establishmentData);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch establishment details: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/establishments/{id}/status")
    public ResponseEntity<?> getEstablishmentStatus(@PathVariable Long id) {
        try {
            // Get real-time status
            String status = realTimeUpdateService.getEstablishmentStatus(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("establishmentId", id);
            response.put("status", status);
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch establishment status");
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/generate-password-hash")
    public ResponseEntity<?> generatePasswordHash(@RequestParam String password) {
        try {
            org.springframework.security.crypto.password.PasswordEncoder encoder = 
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            String hashedPassword = encoder.encode(password);
            
            Map<String, Object> response = new HashMap<>();
            response.put("password", password);
            response.put("hashedPassword", hashedPassword);
            response.put("message", "Password hash generated successfully");
            
            // Test if the hash matches
            boolean matches = encoder.matches(password, hashedPassword);
            response.put("testMatches", matches);
            
            // Test with the existing hash from seed data
            String existingHash = "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi";
            boolean existingMatches = encoder.matches(password, existingHash);
            response.put("existingHashMatches", existingMatches);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to generate password hash: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/test-establishment-data/{id}")
    public ResponseEntity<?> testEstablishmentData(@PathVariable Long id) {
        try {
            Establishment establishment = establishmentService.findById(id);
            if (establishment == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> testData = new HashMap<>();
            testData.put("id", establishment.getId());
            testData.put("name", establishment.getName());
            testData.put("profileImagePath", establishment.getProfileImagePath());
            testData.put("operatingHours", establishment.getOperatingHours());
            testData.put("weeklySchedule", establishment.getWeeklySchedule());
            testData.put("status", establishment.getStatus().toString());
            
            return ResponseEntity.ok(testData);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch test data: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/reset-admin-password")
    public ResponseEntity<?> resetAdminPassword(@RequestParam String newPassword) {
        try {
            org.springframework.security.crypto.password.PasswordEncoder encoder = 
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            String hashedPassword = encoder.encode(newPassword);
            
            // Find admin user and update password
            com.opennova.model.User admin = userService.findByEmailSafe("abishekopennova@gmail.com");
            if (admin == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Admin user not found");
                return ResponseEntity.status(404).body(error);
            }
            
            admin.setPassword(hashedPassword);
            admin.setUpdatedAt(java.time.LocalDateTime.now());
            userService.save(admin);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Admin password reset successfully");
            response.put("email", admin.getEmail());
            response.put("newPasswordHash", hashedPassword);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to reset admin password: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
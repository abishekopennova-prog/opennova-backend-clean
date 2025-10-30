package com.opennova.service;

import com.opennova.model.Menu;
import com.opennova.model.Establishment;
import com.opennova.repository.MenuRepository;
import com.opennova.repository.EstablishmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MenuService {

    @Autowired
    private MenuRepository menuRepository;
    
    @Autowired
    private EstablishmentRepository establishmentRepository;

    @Autowired
    private FileStorageService fileStorageService;

    public List<Menu> getMenusByEstablishmentId(Long establishmentId) {
        System.out.println("🔍 MenuService: Fetching menus for establishment ID: " + establishmentId);
        List<Menu> menus = menuRepository.findActiveMenusByEstablishmentIdOrderByCreatedAtDesc(establishmentId);
        System.out.println("✅ Found " + menus.size() + " active menus for establishment " + establishmentId);
        
        for (Menu menu : menus) {
            System.out.println("  - Menu: " + menu.getName() + " (ID: " + menu.getId() + ", Active: " + menu.getIsActive() + ")");
        }
        
        return menus;
    }

    public Menu createMenu(Long establishmentId, Menu menu, MultipartFile imageFile) {
        System.out.println("🍽️ MenuService: Creating menu for establishment ID: " + establishmentId);
        
        Optional<Establishment> establishment = establishmentRepository.findById(establishmentId);
        if (establishment.isPresent()) {
            System.out.println("✅ Found establishment: " + establishment.get().getName());
            
            menu.setEstablishment(establishment.get());
            menu.setCreatedAt(LocalDateTime.now());
            menu.setUpdatedAt(LocalDateTime.now());
            menu.setIsActive(true);

            // Handle image upload
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    System.out.println("📷 Storing menu image: " + imageFile.getOriginalFilename());
                    String imagePath = fileStorageService.storeFile(imageFile, "menu-images");
                    menu.setImagePath(imagePath);
                    System.out.println("✅ Image stored at: " + imagePath);
                } catch (Exception e) {
                    System.err.println("❌ Failed to store menu image: " + e.getMessage());
                    e.printStackTrace();
                    // Continue without image
                }
            } else {
                System.out.println("ℹ️ No image provided for menu item");
            }

            System.out.println("💾 Saving menu to database...");
            Menu savedMenu = menuRepository.save(menu);
            System.out.println("✅ Menu saved successfully with ID: " + savedMenu.getId());
            
            return savedMenu;
        }
        
        System.err.println("❌ Establishment not found with ID: " + establishmentId);
        throw new RuntimeException("Establishment not found");
    }

    public Menu updateMenu(Long menuId, Menu menuData, MultipartFile imageFile) {
        Optional<Menu> existingMenu = menuRepository.findById(menuId);
        if (existingMenu.isPresent()) {
            Menu menu = existingMenu.get();
            menu.setName(menuData.getName());
            menu.setDescription(menuData.getDescription());
            menu.setPrice(menuData.getPrice());
            menu.setPreparationTime(menuData.getPreparationTime());
            menu.setCategory(menuData.getCategory());
            menu.setIsVegetarian(menuData.getIsVegetarian());
            menu.setIsAvailable(menuData.getIsAvailable());
            menu.setUpdatedAt(LocalDateTime.now());

            // Handle image upload
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    // Delete old image if exists
                    if (menu.getImagePath() != null) {
                        try {
                            fileStorageService.deleteFile(menu.getImagePath());
                        } catch (Exception e) {
                            System.err.println("Failed to delete old menu image: " + e.getMessage());
                        }
                    }
                    
                    String imagePath = fileStorageService.storeFile(imageFile, "menu-images");
                    menu.setImagePath(imagePath);
                } catch (Exception e) {
                    System.err.println("Failed to store menu image: " + e.getMessage());
                    // Continue without updating image
                }
            }

            return menuRepository.save(menu);
        }
        throw new RuntimeException("Menu not found");
    }

    public boolean deleteMenu(Long menuId) {
        Optional<Menu> menu = menuRepository.findById(menuId);
        if (menu.isPresent()) {
            Menu menuItem = menu.get();
            
            // Delete associated image file
            if (menuItem.getImagePath() != null) {
                try {
                    fileStorageService.deleteFile(menuItem.getImagePath());
                } catch (Exception e) {
                    System.err.println("Failed to delete menu image: " + e.getMessage());
                }
            }
            
            menuItem.setIsActive(false);
            menuItem.setUpdatedAt(LocalDateTime.now());
            menuRepository.save(menuItem);
            return true;
        }
        return false;
    }

    public Menu getMenuById(Long menuId) {
        return menuRepository.findById(menuId).orElse(null);
    }

    public long getMenuCountByEstablishment(Long establishmentId) {
        return menuRepository.countByEstablishmentId(establishmentId);
    }

    public boolean menuExistsByName(Long establishmentId, String name) {
        return menuRepository.existsByEstablishmentIdAndName(establishmentId, name);
    }
}
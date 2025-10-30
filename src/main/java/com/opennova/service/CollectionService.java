package com.opennova.service;

import com.opennova.model.Collection;
import com.opennova.model.Establishment;
import com.opennova.repository.CollectionRepository;
import com.opennova.repository.EstablishmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CollectionService {

    @Autowired
    private CollectionRepository collectionRepository;
    
    @Autowired
    private EstablishmentRepository establishmentRepository;

    public List<Collection> getCollectionsByEstablishmentId(Long establishmentId) {
        return collectionRepository.findActiveCollectionsByEstablishmentIdOrderByCreatedAtDesc(establishmentId);
    }

    public Collection createCollection(Long establishmentId, Collection collection) {
        Optional<Establishment> establishment = establishmentRepository.findById(establishmentId);
        if (establishment.isPresent()) {
            collection.setEstablishment(establishment.get());
            collection.setCreatedAt(LocalDateTime.now());
            collection.setUpdatedAt(LocalDateTime.now());
            collection.setIsActive(true);
            return collectionRepository.save(collection);
        }
        throw new RuntimeException("Establishment not found");
    }

    public Collection updateCollection(Long collectionId, Collection collectionData, Long establishmentId) {
        Optional<Collection> existingCollection = collectionRepository.findById(collectionId);
        if (existingCollection.isPresent()) {
            Collection collection = existingCollection.get();
            
            // Verify the collection belongs to the correct establishment
            if (!collection.getEstablishment().getId().equals(establishmentId)) {
                throw new RuntimeException("Collection does not belong to this establishment");
            }
            
            collection.setItemName(collectionData.getItemName());
            collection.setDescription(collectionData.getDescription());
            collection.setPrice(collectionData.getPrice());
            collection.setSizes(collectionData.getSizes());
            collection.setColors(collectionData.getColors());
            collection.setFabric(collectionData.getFabric());
            collection.setBrand(collectionData.getBrand());
            collection.setStock(collectionData.getStock());
            collection.setImagePath(collectionData.getImagePath());
            collection.setUpdatedAt(LocalDateTime.now());
            
            return collectionRepository.save(collection);
        }
        throw new RuntimeException("Collection item not found");
    }

    public boolean deleteCollection(Long collectionId, Long establishmentId) {
        Optional<Collection> existingCollection = collectionRepository.findById(collectionId);
        if (existingCollection.isPresent()) {
            Collection collection = existingCollection.get();
            
            // Verify the collection belongs to the correct establishment
            if (!collection.getEstablishment().getId().equals(establishmentId)) {
                return false;
            }
            
            collection.setIsActive(false);
            collection.setUpdatedAt(LocalDateTime.now());
            collectionRepository.save(collection);
            return true;
        }
        return false;
    }

    public Collection getCollectionById(Long collectionId, Long establishmentId) {
        Optional<Collection> collection = collectionRepository.findById(collectionId);
        if (collection.isPresent() && collection.get().getEstablishment().getId().equals(establishmentId) && collection.get().getIsActive()) {
            return collection.get();
        }
        return null;
    }

    public long getCollectionCountByEstablishment(Long establishmentId) {
        return collectionRepository.countByEstablishmentIdAndIsActive(establishmentId);
    }

    public boolean collectionExistsByItemName(Long establishmentId, String itemName) {
        return collectionRepository.existsByEstablishmentIdAndItemName(establishmentId, itemName);
    }
}
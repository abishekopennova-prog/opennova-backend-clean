package com.opennova.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class SharedStateService {
    
    private final Map<String, Object> sharedState = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> establishmentRequests = new ArrayList<>();
    
    public void setState(String key, Object value) {
        sharedState.put(key, value);
    }
    
    public Object getState(String key) {
        return sharedState.get(key);
    }
    
    public <T> T getState(String key, Class<T> type) {
        Object value = sharedState.get(key);
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }
    
    public void removeState(String key) {
        sharedState.remove(key);
    }
    
    public boolean hasState(String key) {
        return sharedState.containsKey(key);
    }
    
    public void clearAll() {
        sharedState.clear();
    }
    
    // Establishment Request Management
    public synchronized void addEstablishmentRequest(Map<String, Object> request) {
        establishmentRequests.add(request);
    }
    
    public synchronized List<Map<String, Object>> getAllEstablishmentRequests() {
        return new ArrayList<>(establishmentRequests);
    }
    
    public synchronized List<Map<String, Object>> getUserEstablishmentRequests(Long userId) {
        return establishmentRequests.stream()
            .filter(request -> userId.equals(request.get("requestedBy")))
            .collect(Collectors.toList());
    }
    
    public synchronized boolean removeEstablishmentRequest(Long requestId) {
        return establishmentRequests.removeIf(request -> {
            Object id = request.get("id");
            if (id instanceof Long) {
                return requestId.equals(id);
            } else if (id instanceof Number) {
                return requestId.equals(((Number) id).longValue());
            }
            return false;
        });
    }
    
    public synchronized Map<String, Object> getEstablishmentRequest(Long requestId) {
        return establishmentRequests.stream()
            .filter(request -> {
                Object id = request.get("id");
                if (id instanceof Long) {
                    return requestId.equals(id);
                } else if (id instanceof Number) {
                    return requestId.equals(((Number) id).longValue());
                }
                return false;
            })
            .findFirst()
            .orElse(null);
    }
    
    public synchronized void updateEstablishmentRequestStatus(Long requestId, String status) {
        establishmentRequests.stream()
            .filter(request -> {
                Object id = request.get("id");
                if (id instanceof Long) {
                    return requestId.equals(id);
                } else if (id instanceof Number) {
                    return requestId.equals(((Number) id).longValue());
                }
                return false;
            })
            .findFirst()
            .ifPresent(request -> request.put("status", status));
    }
}
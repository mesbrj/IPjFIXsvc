package com.ipfix_scenario_ai.ipjfix_svc.adapters.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.ipfix_scenario_ai.ipjfix_svc.core.models.User;

@Repository
public class InMemoryUserRepository {
    
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private long nextId = 1;
    
    public User save(User user) {
        if (user.getId() == null || user.getId().isEmpty()) {
            user.setId(String.valueOf(nextId++));
        }
        users.put(user.getId(), user);
        return user;
    }
    
    public Optional<User> findById(String id) {
        return Optional.ofNullable(users.get(id));
    }
    
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }
    
    public void delete(String id) {
        users.remove(id);
    }
    
    public void deleteAll() {
        users.clear();
    }
    
    public long count() {
        return users.size();
    }
    
    public boolean existsById(String id) {
        return users.containsKey(id);
    }
    
    public List<User> findByTenantId(String tenantId) {
        return users.values().stream()
                .filter(user -> tenantId.equals(user.getTenantId()))
                .collect(Collectors.toList());
    }
    
    public List<User> findByRole(String role) {
        return users.values().stream()
                .filter(user -> role.equals(user.getRole()))
                .collect(Collectors.toList());
    }
    
    public List<User> findActiveUsers() {
        return users.values().stream()
                .filter(User::isActive)
                .collect(Collectors.toList());
    }
    
    public List<User> findUsersByTenantAndRole(String tenantId, String role) {
        return users.values().stream()
                .filter(user -> tenantId.equals(user.getTenantId()) && role.equals(user.getRole()))
                .collect(Collectors.toList());
    }
}

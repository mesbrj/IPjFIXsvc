package com.ipfix_scenario_ai.ipjfix_svc.adapters.ignite;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import com.ipfix_scenario_ai.ipjfix_svc.core.models.User;

@Repository
@ConditionalOnProperty(name = "ignite.enabled", havingValue = "true", matchIfMissing = false)
public class UserRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);
    private static final String CACHE_NAME = "UserCache";
    
    
    private final IgniteCache<String, User> userCache;
    
    public UserRepository(Ignite ignite) {
        this.userCache = ignite.getOrCreateCache(CACHE_NAME);
        logger.info("UserRepository initialized with cache: {}", CACHE_NAME);
    }
    
    // CRUD Operations
    public User save(User user) {
        if (user.getId() == null) {
            user.setId(generateUserId());
        }
        
        userCache.put(user.getId(), user);
        logger.debug("Saved user: {}", user.getId());
        return user;
    }
    
    public Optional<User> findById(String id) {
        User user = userCache.get(id);
        return Optional.ofNullable(user);
    }
    
    public List<User> findAll() {
        // Use cache iteration instead of SQL queries since indexing is disabled
        List<User> users = new java.util.ArrayList<>();
        for (javax.cache.Cache.Entry<String, User> entry : userCache) {
            users.add(entry.getValue());
        }
        return users;
    }
    
    public Optional<User> findByUsername(String username) {
        // Use cache iteration instead of SQL queries since indexing is disabled
        for (javax.cache.Cache.Entry<String, User> entry : userCache) {
            User user = entry.getValue();
            if (user.getUsername().equals(username)) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }
    
    public Optional<User> findByEmail(String email) {
        // Use cache iteration instead of SQL queries since indexing is disabled
        for (javax.cache.Cache.Entry<String, User> entry : userCache) {
            User user = entry.getValue();
            if (user.getEmail().equals(email)) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }
    
    public List<User> findByTenantId(String tenantId) {
        // Use cache iteration instead of SQL queries since indexing is disabled
        List<User> users = new java.util.ArrayList<>();
        for (javax.cache.Cache.Entry<String, User> entry : userCache) {
            User user = entry.getValue();
            if (tenantId.equals(user.getTenantId())) {
                users.add(user);
            }
        }
        return users;
    }
    
    public List<User> findActiveUsers() {
        // Use cache iteration instead of SQL queries since indexing is disabled
        List<User> users = new java.util.ArrayList<>();
        for (javax.cache.Cache.Entry<String, User> entry : userCache) {
            User user = entry.getValue();
            if (user.isActive()) {
                users.add(user);
            }
        }
        return users;
    }
    
    public List<User> findByRole(String role) {
        // Use cache iteration instead of SQL queries since indexing is disabled
        List<User> users = new java.util.ArrayList<>();
        for (javax.cache.Cache.Entry<String, User> entry : userCache) {
            User user = entry.getValue();
            if (role.equals(user.getRole())) {
                users.add(user);
            }
        }
        return users;
    }
    
    // Advanced queries for IPFIX project
    public List<User> findUsersByTenantAndRole(String tenantId, String role) {
        // Use cache iteration instead of SQL queries since indexing is disabled
        List<User> users = new java.util.ArrayList<>();
        for (javax.cache.Cache.Entry<String, User> entry : userCache) {
            User user = entry.getValue();
            if (tenantId.equals(user.getTenantId()) && role.equals(user.getRole()) && user.isActive()) {
                users.add(user);
            }
        }
        return users;
    }
    
    public long countUsersByTenant(String tenantId) {
        // Use cache iteration instead of SQL queries since indexing is disabled
        long count = 0;
        for (javax.cache.Cache.Entry<String, User> entry : userCache) {
            User user = entry.getValue();
            if (tenantId.equals(user.getTenantId())) {
                count++;
            }
        }
        return count;
    }
    
    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }
    
    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }
    
    public void deleteById(String id) {
        userCache.remove(id);
        logger.debug("Deleted user: {}", id);
    }
    
    public void delete(User user) {
        deleteById(user.getId());
    }
    
    public User update(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("User ID cannot be null for update");
        }
        
        if (!userCache.containsKey(user.getId())) {
            throw new IllegalArgumentException("User not found: " + user.getId());
        }
        
        return save(user);
    }
    
    // Utility methods
    private String generateUserId() {
        return UUID.randomUUID().toString();
    }
    
    public void clearCache() {
        userCache.clear();
        logger.info("Cleared user cache");
    }
    
    public long getCacheSize() {
        return userCache.size();
    }
}
package com.ipfix_scenario_ai.ipjfix_svc.core.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

public class User implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @QuerySqlField(index = true)
    private String id;
    
    @QuerySqlField(index = true)
    private String username;
    
    @QuerySqlField
    private String email;
    
    @QuerySqlField
    private String firstName;
    
    @QuerySqlField
    private String lastName;
    
    @QuerySqlField
    private String role;
    
    @QuerySqlField
    private boolean active;
    
    @QuerySqlField
    private LocalDateTime createdAt;
    
    @QuerySqlField
    private LocalDateTime lastLoginAt;
    
    @QuerySqlField(index = true)
    private String tenantId;

    // Default constructor
    public User() {
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }

    // Constructor with essential fields
    public User(String id, String username, String email, String tenantId) {
        this();
        this.id = id;
        this.username = username;
        this.email = email;
        this.tenantId = tenantId;
    }

    // Full constructor
    public User(String id, String username, String email, String firstName, 
                String lastName, String role, String tenantId) {
        this(id, username, email, tenantId);
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    // Utility methods
    public String getFullName() {
        return String.format("%s %s", 
            firstName != null ? firstName : "", 
            lastName != null ? lastName : "").trim();
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("User{id='%s', username='%s', email='%s', tenantId='%s'}", 
                            id, username, email, tenantId);
    }
}
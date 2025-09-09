package com.userapi.common.constants;

/**
 * Enumeration of resource names used in permission checks.
 * This ensures consistency and prevents typos in resource names across the application.
 */
public enum ResourceNames {
    
    // Role Management Resources
    ROLE("ROLE"),
    USER_ROLE("USER_ROLE"),
    SYSTEM_ROLE("SYSTEM_ROLE"),
    PERMISSION("PERMISSION"),
    
    // User Management Resources
    USER("USER"),
    USER_PROFILE("USER_PROFILE"),
    
    // Organization Resources
    ORGANIZATION("ORGANIZATION"),
    
    // System Resources
    SYSTEM("SYSTEM"),
    ADMIN("ADMIN");
    
    private final String value;
    
    ResourceNames(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}

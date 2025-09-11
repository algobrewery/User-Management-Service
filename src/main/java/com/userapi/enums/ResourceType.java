package com.userapi.enums;

/**
 * Enumeration of valid resource types for role permissions.
 * This ensures consistent resource naming across the system.
 */
public enum ResourceType {
    
    // Core system resources
    USERS("users", "User profile information"),
    ROLES("roles", "Role management data"),
    ORGANIZATIONS("organizations", "Organization settings"),
    
    // Business domain resources
    TASKS("tasks", "Task management data"),
    CLIENTS("clients", "Client/customer information"),
    
    // System resources
    SYSTEM_ROLES("system_roles", "System-level role management"),
    USER_ROLES("user_roles", "User-role assignment management"),
    PERMISSIONS("permissions", "Permission checking and validation"),
    
    // Wildcard for admin access
    ALL("*", "Wildcard for all resources");
    
    private final String resourceName;
    private final String description;
    
    ResourceType(String resourceName, String description) {
        this.resourceName = resourceName;
        this.description = description;
    }
    
    public String getResourceName() {
        return resourceName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get ResourceType by resource name
     * @param resourceName the resource name to lookup
     * @return ResourceType or null if not found
     */
    public static ResourceType fromResourceName(String resourceName) {
        if (resourceName == null) {
            return null;
        }
        
        for (ResourceType type : values()) {
            if (type.resourceName.equals(resourceName)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * Check if a resource name is valid
     * @param resourceName the resource name to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidResource(String resourceName) {
        return fromResourceName(resourceName) != null;
    }
    
    /**
     * Get all valid resource names as array
     * @return array of valid resource names
     */
    public static String[] getValidResourceNames() {
        ResourceType[] types = values();
        String[] names = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            names[i] = types[i].resourceName;
        }
        return names;
    }
}

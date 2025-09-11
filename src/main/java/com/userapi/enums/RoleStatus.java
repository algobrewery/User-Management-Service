package com.userapi.enums;

/**
 * Enumeration of valid role statuses.
 * Provides clear, extensible status management for roles.
 */
public enum RoleStatus {
    
    ACTIVE("active", "Role is active and can be assigned to users"),
    INACTIVE("inactive", "Role is inactive and cannot be assigned to users"),
    DELETED("deleted", "Role is marked for deletion"),
    SUSPENDED("suspended", "Role is temporarily suspended");
    
    private final String statusName;
    private final String description;
    
    RoleStatus(String statusName, String description) {
        this.statusName = statusName;
        this.description = description;
    }
    
    public String getStatusName() {
        return statusName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get RoleStatus by status name
     * @param statusName the status name to lookup
     * @return RoleStatus or null if not found
     */
    public static RoleStatus fromStatusName(String statusName) {
        if (statusName == null) {
            return null;
        }
        
        for (RoleStatus status : values()) {
            if (status.statusName.equals(statusName)) {
                return status;
            }
        }
        return null;
    }
    
    /**
     * Check if a status name is valid
     * @param statusName the status name to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidStatus(String statusName) {
        return fromStatusName(statusName) != null;
    }
    
    /**
     * Get all valid status names as array
     * @return array of valid status names
     */
    public static String[] getValidStatusNames() {
        RoleStatus[] statuses = values();
        String[] names = new String[statuses.length];
        for (int i = 0; i < statuses.length; i++) {
            names[i] = statuses[i].statusName;
        }
        return names;
    }
}

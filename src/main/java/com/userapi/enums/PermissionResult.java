package com.userapi.enums;

/**
 * Enumeration of permission check results.
 * Provides clear, extensible results for permission validation.
 */
public enum PermissionResult {
    
    ACCEPTED("accepted", "Permission granted - user can perform the action"),
    DENIED("denied", "Permission denied - user cannot perform the action"),
    PENDING("pending", "Permission check is pending approval"),
    EXPIRED("expired", "Permission has expired and needs renewal");
    
    private final String resultName;
    private final String description;
    
    PermissionResult(String resultName, String description) {
        this.resultName = resultName;
        this.description = description;
    }
    
    public String getResultName() {
        return resultName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get PermissionResult by result name
     * @param resultName the result name to lookup
     * @return PermissionResult or null if not found
     */
    public static PermissionResult fromResultName(String resultName) {
        if (resultName == null) {
            return null;
        }
        
        for (PermissionResult result : values()) {
            if (result.resultName.equals(resultName)) {
                return result;
            }
        }
        return null;
    }
    
    /**
     * Check if a result name is valid
     * @param resultName the result name to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidResult(String resultName) {
        return fromResultName(resultName) != null;
    }
    
    /**
     * Get all valid result names as array
     * @return array of valid result names
     */
    public static String[] getValidResultNames() {
        PermissionResult[] results = values();
        String[] names = new String[results.length];
        for (int i = 0; i < results.length; i++) {
            names[i] = results[i].resultName;
        }
        return names;
    }
}

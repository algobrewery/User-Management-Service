package com.userapi.models.external.roles;

import lombok.Data;

import java.util.List;

@Data
public class ListRolesFilterCriteria {
    private List<ListRolesFilterCriteriaAttribute> attributes;
    
    // Role-specific filter options
    // Note: organizationUuid is removed - it's extracted from HTTP headers for security
    // This prevents clients from potentially providing different organization UUIDs
    // in the request body vs headers, which could be a security risk
    private Boolean isSystemManaged;
    private Boolean isActive;
    private String roleType; // e.g., "ADMIN", "USER", "CUSTOM"
    
    // User-specific filter options
    private String userUuid; // Filter roles assigned to specific user
}

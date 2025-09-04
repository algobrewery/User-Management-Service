package com.userapi.models.external.roles;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRoleResponse {
    private String role_uuid;
    private String name;                    // This maps to role_name from request
    private String description;
    private String role_management_type;
    private Object policy;                  // Changed from String to Object
    private String message;
    private String status;
    private String organization_uuid;
    private Boolean is_active;
    private String created_by;
    private String created_at;  // Changed to String to handle different date formats
    
    // Add computed fields for better response handling
    public String getDisplayName() {
        return name != null ? name : "Unnamed Role";
    }
    
    public boolean isActive() {
        return is_active != null ? is_active : true;
    }
    
    public String getCreatedBy() {
        return created_by != null ? created_by : "system";
    }
    
    public String getOrganizationUuid() {
        return organization_uuid != null ? organization_uuid : "unknown";
    }
}

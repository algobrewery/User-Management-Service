package com.userapi.models.external.roles;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignRoleRequest {
    @NotBlank(message = "Role UUID is required")
    private String role_uuid;
    
    // Note: organization_uuid is removed - it's extracted from HTTP headers for security
    // This prevents clients from potentially providing different organization UUIDs
    // in the request body vs headers, which could be a security risk
}

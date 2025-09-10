package com.userapi.models.external.roles;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * Request model for assigning roles to users.
 * Organization context is provided via x-app-org-uuid header, not in request body.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssignRoleRequest {
    @NotBlank(message = "Role UUID is required")
    private String role_uuid;
    
    // organization_uuid removed - now comes from x-app-org-uuid header
    // This matches the updated Roles and Permissions Service implementation
}

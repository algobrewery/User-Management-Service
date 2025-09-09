package com.userapi.models.external.roles;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * Request model for assigning roles to users.
 * Note: The external Roles and Permissions Service requires organization_uuid in the request body
 * even though we also send it via x-app-org-uuid header for consistency.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssignRoleRequest {
    @NotBlank(message = "Role UUID is required")
    private String role_uuid;

    @NotBlank(message = "Organization UUID is required")
    private String organization_uuid;
}

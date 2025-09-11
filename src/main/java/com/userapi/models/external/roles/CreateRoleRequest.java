package com.userapi.models.external.roles;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Request model for creating new roles.
 * Organization context is provided via x-app-org-uuid header, not in request body.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateRoleRequest {
    @NotBlank(message = "Role name is required")
    @Size(max = 100, message = "Role name must not exceed 100 characters")
    @JsonProperty("role_name")  // API field name
    private String roleName;

    // Add name field for builder compatibility, but exclude from JSON serialization
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @JsonProperty("role_management_type")
    private String roleManagementType;

    @NotNull(message = "Policy is required")
    private JsonNode policy;

    // Lombok @Data automatically generates all getters/setters including getRoleName() and setRoleName()
}

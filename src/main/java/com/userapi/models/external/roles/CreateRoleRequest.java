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

    // Note: organizationUuid is removed - it's extracted from HTTP headers for security
    // This prevents clients from potentially providing different organization UUIDs
    // in the request body vs headers, which could be a security risk

    @JsonProperty("role_management_type")
    private String roleManagementType;

    @NotNull(message = "Policy is required")
    private JsonNode policy;

    // Custom getter to map name to roleName for backward compatibility
    public String getName() {
        return this.roleName;
    }

    // Custom setter to map name to roleName for backward compatibility
    public void setName(String name) {
        this.roleName = name;
    }
}

package com.userapi.models.external.roles;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.userapi.enums.RoleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Size;

/**
 * Request model for updating existing roles.
 * Only allows updating safe fields that don't break role integrity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateRoleRequest {
    
    /**
     * Role description - can be updated to reflect current role purpose
     */
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    /**
     * Role policy - defines permissions and features
     * This is the core functionality of the role
     */
    private JsonNode policy;

    /**
     * Role status - defines the current state of the role
     * Valid values: ACTIVE, INACTIVE, DELETED, SUSPENDED
     */
    private RoleStatus status;
}

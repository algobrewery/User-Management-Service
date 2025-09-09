package com.userapi.models.external.roles;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Size;

/**
 * Request model for updating role attributes.
 * Only allows updating specific fields to maintain data integrity and security.
 * 
 * IMMUTABLE FIELDS (not included in this model):
 * - roleName: Role name should not be changed for security and consistency
 * - role_uuid: Primary key, never changeable
 * - organization_uuid: Role belongs to specific organization
 * - role_management_type: System vs custom role type is fundamental
 * - created_at, created_by: Audit information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateRoleRequest {
    
    /**
     * Role description - can be updated to provide better context
     */
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    /**
     * Role policy/permissions - can be updated to modify role capabilities
     */
    private JsonNode policy;

    /**
     * Role active status - can be updated to enable/disable role
     */
    private Boolean is_active;
}

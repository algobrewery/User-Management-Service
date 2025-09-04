package com.userapi.models.external.roles;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponse {
    @JsonProperty("role_uuid")
    private String role_uuid;

    @JsonProperty("role_name")
    private String roleName;

    @JsonProperty("organization_uuid")
    private String organization_uuid;

    private String description;

    @JsonProperty("role_management_type")
    private String role_management_type;

    private JsonNode policy;

    @JsonProperty("created_at")
    private String created_at;

    @JsonProperty("updated_at")
    private String updated_at;

    @JsonProperty("created_by")
    private String created_by;

    // Backward compatibility methods
    public String getName() {
        return roleName;
    }

    public void setName(String name) {
        this.roleName = name;
    }

    public String getRole_uuid() {
        return role_uuid;
    }

    public void setRole_uuid(String role_uuid) {
        this.role_uuid = role_uuid;
    }

    public String getOrganization_uuid() {
        return organization_uuid;
    }

    public void setOrganization_uuid(String organization_uuid) {
        this.organization_uuid = organization_uuid;
    }

    public String getRole_management_type() {
        return role_management_type;
    }

    public void setRole_management_type(String role_management_type) {
        this.role_management_type = role_management_type;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getCreated_by() {
        return created_by;
    }

    public void setCreated_by(String created_by) {
        this.created_by = created_by;
    }

    // Legacy fields for backward compatibility
    private Boolean is_active;

    public Boolean getIs_active() {
        return is_active;
    }

    public void setIs_active(Boolean is_active) {
        this.is_active = is_active;
    }
}

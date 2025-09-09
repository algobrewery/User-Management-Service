package com.userapi.models.external.roles;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    // Backward compatibility method - only keep this one as it provides a different method name
    public String getName() {
        return roleName;
    }

    public void setName(String name) {
        this.roleName = name;
    }

    // Legacy fields for backward compatibility
    private Boolean is_active;
}

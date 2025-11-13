package com.userapi.models.external.roles;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.userapi.enums.PermissionResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PermissionCheckResponse {
    private PermissionResult result;
    private String message;
    private String resource;
    private String action;

    @JsonProperty("has_permission")
    private Boolean hasPermission;

    @JsonProperty("role_uuid")
    private String roleUuid;

    @JsonProperty("role_name")
    private String roleName;

    @JsonProperty("granted_scope")
    private String grantedScope;
}

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
public class PermissionCheckRequest {
    @NotBlank(message = "User UUID is required")
    private String user_uuid;

    @NotBlank(message = "Organization UUID is required")
    private String organization_uuid;

    @NotBlank(message = "Resource is required")
    private String resource;

    @NotBlank(message = "Action is required")
    private String action;
}

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
    // Note: user_uuid is removed - it's extracted from HTTP headers for security
    // This prevents clients from potentially providing different user UUIDs
    // in the request body vs headers, which could be a security risk

    // Note: organization_uuid is removed - it's extracted from HTTP headers for security
    // This prevents clients from potentially providing different organization UUIDs
    // in the request body vs headers, which could be a security risk

    @NotBlank(message = "Resource is required")
    private String resource;

    @NotBlank(message = "Action is required")
    private String action;
}

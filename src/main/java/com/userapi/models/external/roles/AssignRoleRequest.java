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
public class AssignRoleRequest {
    @NotBlank(message = "Role UUID is required")
    private String role_uuid;

    @NotBlank(message = "Organization UUID is required")
    private String organization_uuid;
}

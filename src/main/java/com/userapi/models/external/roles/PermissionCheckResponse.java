package com.userapi.models.external.roles;

import com.userapi.enums.PermissionResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionCheckResponse {
    private PermissionResult result;
    private String message;
    private String resource;
    private String action;
}

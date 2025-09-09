package com.userapi.service;

import com.userapi.models.external.roles.*;
import reactor.core.publisher.Mono;
import java.util.List;


public interface RolesServiceClient {
    
    // Role Management
    Mono<RoleResponse> createRole(CreateRoleRequest request, String organizationUuid);
    
    Mono<RoleResponse> getRoleByUuid(String roleUuid, String organizationUuid);
    
    
    Mono<RoleResponse> updateRole(String roleUuid, UpdateRoleRequest request, String organizationUuid);
    
    Mono<Void> deleteRole(String roleUuid, String organizationUuid);
    
    // User Role Management
    Mono<Void> assignRoleToUser(String userUuid, String roleUuid, String organizationUuid);
    
    
    Mono<Void> removeRoleFromUser(String userUuid, String roleUuid, String organizationUuid);
    
    // Permission Management
    Mono<PermissionCheckResponse> checkPermission(PermissionCheckRequest request);
    
    Mono<Boolean> hasPermission(String userUuid, String organizationUuid, String resource, String action);
    
    // Role Search and Filtering
    Mono<ListRolesResponse> searchRoles(ListRolesRequest request, String organizationUuid);
    
    // Legacy methods for internal use
    Mono<List<RoleResponse>> getOrganizationRoles(String organizationUuid);
    Mono<List<RoleResponse>> getSystemManagedRoles();
    Mono<List<RoleResponse>> getUserRoles(String userUuid, String organizationUuid);
}

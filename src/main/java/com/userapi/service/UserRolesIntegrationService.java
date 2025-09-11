package com.userapi.service;

import com.userapi.models.external.roles.RoleResponse;
import reactor.core.publisher.Mono;

import java.util.List;

public interface UserRolesIntegrationService {
    
    /**
     * Create default admin role for a new organization
     */
    Mono<RoleResponse> createDefaultAdminRole(String organizationUuid, String createdBy);
    
    /**
     * Create default user role for a new organization
     */
    Mono<RoleResponse> createDefaultUserRole(String organizationUuid, String createdBy);
    
    /**
     * Assign admin role to a user
     */
    Mono<Void> assignAdminRoleToUser(String userUuid, String organizationUuid, String assignedBy);
    
    /**
     * Assign admin role to a user with specific admin role UUID
     */
    Mono<Void> assignAdminRoleToUser(String userUuid, String organizationUuid, String assignedBy, String adminRoleUuid);
    
    /**
     * Assign user role to a user
     */
    Mono<Void> assignUserRoleToUser(String userUuid, String organizationUuid, String assignedBy);
    
    /**
     * Get all roles for a user
     */
    Mono<List<RoleResponse>> getUserRoles(String userUuid, String organizationUuid);
    
    /**
     * Check if user has permission to perform action on resource
     */
    Mono<Boolean> hasPermission(String userUuid, String organizationUuid, String resource, String action);
    
    /**
     * Check if user is admin in organization
     */
    Mono<Boolean> isUserAdmin(String userUuid, String organizationUuid);

    /**
     * Check if user exists and assign admin role to them
     * @param userUuid The UUID of the user to assign admin role to
     * @param organizationUuid The organization UUID
     * @param assignedBy The UUID of the user performing the assignment
     * @return Mono<Void> that completes when the role is assigned or fails if user doesn't exist
     */
    Mono<Void> checkUserExistsAndAssignAdminRole(String userUuid, String organizationUuid, String assignedBy);

    /**
     * Check if user exists and assign specific role by UUID to them
     * @param userUuid The UUID of the user to assign role to
     * @param roleUuid The UUID of the role to assign
     * @param organizationUuid The organization UUID
     * @param assignedBy The UUID of the user performing the assignment
     * @return Mono<Void> that completes when the role is assigned or fails if user doesn't exist
     */
    Mono<Void> checkUserExistsAndAssignRoleByUuid(String userUuid, String roleUuid, String organizationUuid, String assignedBy);
}


package com.userapi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.userapi.models.entity.UserProfile;
import com.userapi.models.external.roles.CreateRoleRequest;
import com.userapi.models.external.roles.ListRolesFilterCriteria;
import com.userapi.models.external.roles.ListRolesFilterCriteriaAttribute;
import com.userapi.models.external.roles.ListRolesRequest;
import com.userapi.models.external.roles.RoleResponse;
import com.userapi.repository.userprofile.UserProfileRepository;
import com.userapi.service.RolesServiceClient;
import com.userapi.service.UserRolesIntegrationService;
import com.userapi.util.PolicyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRolesIntegrationServiceImpl implements UserRolesIntegrationService {

    private final RolesServiceClient rolesServiceClient;
    private final UserProfileRepository userProfileRepository;

    @Override
    public Mono<RoleResponse> createDefaultAdminRole(String organizationUuid, String createdBy) {
        log.info("Creating default admin role for organization: {}", organizationUuid);

        try {
            // Build admin policy dynamically using ResourceType enum
            JsonNode adminPolicy = PolicyBuilder.buildAdminPolicy();

            CreateRoleRequest request = CreateRoleRequest.builder()
                    .roleName("Admin")
                    .description("Default admin role with full access")
                    .roleManagementType("SYSTEM_MANAGED")
                    .policy(adminPolicy)
                    .build();

            return rolesServiceClient.createRole(request, organizationUuid)
                    .doOnSuccess(response -> log.info("Default admin role created: {}", response.getRole_uuid()))
                    .doOnError(error -> log.error("Failed to create default admin role: {}", error.getMessage()));
        } catch (Exception e) {
            log.error("Failed to parse admin policy JSON: {}", e.getMessage());
            return Mono.error(new RuntimeException("Failed to create admin role policy", e));
        }
    }

    @Override
    public Mono<RoleResponse> createDefaultUserRole(String organizationUuid, String createdBy) {
        log.info("Creating default user role for organization: {}", organizationUuid);

        try {
            // Build user policy dynamically using ResourceType enum
            JsonNode userPolicy = PolicyBuilder.buildUserPolicy();

            CreateRoleRequest request = CreateRoleRequest.builder()
                    .roleName("User")
                    .description("Default user role with limited access")
                    .roleManagementType("SYSTEM_MANAGED")
                    .policy(userPolicy)
                    .build();

            return rolesServiceClient.createRole(request, organizationUuid)
                    .doOnSuccess(response -> log.info("Default user role created: {}", response.getRole_uuid()))
                    .doOnError(error -> log.error("Failed to create default user role: {}", error.getMessage()));
        } catch (Exception e) {
            log.error("Failed to parse user policy JSON: {}", e.getMessage());
            return Mono.error(new RuntimeException("Failed to create user role policy", e));
        }
    }

    @Override
    public Mono<Void> assignAdminRoleToUser(String userUuid, String organizationUuid, String assignedBy) {
        return assignAdminRoleToUser(userUuid, organizationUuid, assignedBy, null);
    }

    @Override
    public Mono<Void> assignAdminRoleToUser(String userUuid, String organizationUuid, String assignedBy, String adminRoleUuid) {
        log.info("Assigning admin role to user: {} in organization: {}", userUuid, organizationUuid);

        // If we have a specific admin role UUID (from bootstrap), use it directly
        if (adminRoleUuid != null) {
            log.info("Using provided admin role UUID: {}", adminRoleUuid);
            return rolesServiceClient.assignRoleToUser(userUuid, adminRoleUuid, organizationUuid)
                    .doOnSuccess(response -> log.info("Admin role {} assigned successfully to user: {}", adminRoleUuid, userUuid))
                    .doOnError(error -> log.error("Failed to assign admin role {} to user: {}", adminRoleUuid, userUuid));
        }

        // Since the search endpoint is not working, we'll use the system-managed roles endpoint
        // which should return all available roles including the newly created admin role
        return rolesServiceClient.getSystemManagedRoles()
                .flatMap(systemRoles -> {
                    // Log all available system-managed roles for debugging
                    log.info("Available system-managed roles:");
                    systemRoles.forEach(role -> log.info("  - Role: '{}' (UUID: {})", role.getRoleName(), role.getRole_uuid()));

                    // Try to find admin role with different possible names
                    RoleResponse adminRole = systemRoles.stream()
                            .filter(role -> {
                                String roleName = role.getRoleName();
                                return roleName != null && (
                                    "Admin".equalsIgnoreCase(roleName) ||
                                    "Administrator".equalsIgnoreCase(roleName) ||
                                    "Organization Administrator".equalsIgnoreCase(roleName) ||
                                    "admin".equals(roleName) ||
                                    "ADMIN".equals(roleName) ||
                                    roleName.toLowerCase().contains("admin") ||
                                    roleName.toLowerCase().contains("administrator")
                                );
                            })
                            .findFirst()
                            .orElse(null);

                    if (adminRole != null) {
                        log.info("Found system-managed admin role: '{}' (UUID: {})", adminRole.getRoleName(), adminRole.getRole_uuid());
                        return rolesServiceClient.assignRoleToUser(userUuid, adminRole.getRole_uuid(), organizationUuid);
                    } else {
                        log.error("No admin role found in system-managed roles. Available roles: {}", 
                                systemRoles.stream().map(RoleResponse::getRoleName).toList());
                        return Mono.error(new RuntimeException("Admin role not found"));
                    }
                })
                .doOnSuccess(response -> log.info("Admin role assigned successfully to user: {}", userUuid))
                .doOnError(error -> log.error("Failed to assign admin role to user: {}", error.getMessage()));
    }

    @Override
    public Mono<Void> assignUserRoleToUser(String userUuid, String organizationUuid, String assignedBy) {
        log.info("Assigning user role to user: {} in organization: {}", userUuid, organizationUuid);
        
        // First, try to get the user role from system-managed roles (since we create them as SYSTEM_MANAGED)
        ListRolesRequest systemRequest = ListRolesRequest.builder()
                .filterCriteria(ListRolesFilterCriteria.builder()
                        .attributes(List.of(
                                ListRolesFilterCriteriaAttribute.builder()
                                        .name("role_management_type")
                                        .values(List.of("SYSTEM_MANAGED"))
                                        .build()
                        ))
                        .build())
                .build();
        
        return rolesServiceClient.searchRoles(systemRequest, organizationUuid)
                .flatMap(systemResponse -> {
                    RoleResponse systemUserRole = systemResponse.getRoles().stream()
                            .filter(role -> "User".equals(role.getRoleName()))
                            .findFirst()
                            .orElse(null);
                    
                    if (systemUserRole != null) {
                        log.info("Found system-managed user role: {}", systemUserRole.getRole_uuid());
                        return rolesServiceClient.assignRoleToUser(userUuid, systemUserRole.getRole_uuid(), organizationUuid);
                    }
                    
                    // If no system-managed user role found, try customer-managed roles
                    log.info("No system-managed user role found, checking customer-managed roles");
                    ListRolesRequest userRequest = ListRolesRequest.builder()
                            .filterCriteria(ListRolesFilterCriteria.builder()
                                    .attributes(List.of(
                                            ListRolesFilterCriteriaAttribute.builder()
                                                    .name("role_management_type")
                                                    .values(List.of("CUSTOMER_MANAGED"))
                                                    .build()
                                    ))
                                    .build())
                            .build();
                    
                    return rolesServiceClient.searchRoles(userRequest, organizationUuid)
                            .flatMap(response -> {
                                RoleResponse userRole = response.getRoles().stream()
                                        .filter(role -> "User".equals(role.getRoleName()))
                                        .findFirst()
                                        .orElse(null);
                                
                                if (userRole == null) {
                                    log.error("User role not found in system-managed or customer-managed roles for organization: {}", organizationUuid);
                                    return Mono.error(new RuntimeException("User role not found"));
                                }
                                
                                log.info("Found customer-managed user role: {}", userRole.getRole_uuid());
                                return rolesServiceClient.assignRoleToUser(userUuid, userRole.getRole_uuid(), organizationUuid);
                            });
                })
                .doOnSuccess(response -> log.info("User role assigned successfully to user: {}", userUuid))
                .doOnError(error -> log.error("Failed to assign user role to user: {}", error.getMessage()));
    }

    @Override
    public Mono<List<RoleResponse>> getUserRoles(String userUuid, String organizationUuid) {
        log.info("Getting roles for user: {} in organization: {}", userUuid, organizationUuid);
        
        ListRolesRequest request = ListRolesRequest.builder()
                .filterCriteria(ListRolesFilterCriteria.builder()
                        .attributes(List.of(
                                ListRolesFilterCriteriaAttribute.builder()
                                        .name("user_uuid")
                                        .values(List.of(userUuid))
                                        .build()
                        ))
                        .build())
                .build();
        
        return rolesServiceClient.searchRoles(request, organizationUuid)
                .map(response -> response.getRoles())
                .doOnSuccess(roles -> log.info("Retrieved {} roles for user: {}", roles.size(), userUuid))
                .doOnError(error -> log.error("Failed to get user roles: {}", error.getMessage()));
    }

    @Override
    public Mono<Boolean> hasPermission(String userUuid, String organizationUuid, String resource, String action) {
        log.info("Checking permission for user: {} on resource: {} with action: {} in organization: {}", 
                userUuid, resource, action, organizationUuid);
        
        return rolesServiceClient.hasPermission(userUuid, organizationUuid, resource, action)
                .doOnSuccess(hasPermission -> log.info("Permission check result: {} for user: {} on {}:{}", 
                        hasPermission, userUuid, resource, action))
                .doOnError(error -> log.error("Failed to check permission: {}", error.getMessage()))
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Boolean> isUserAdmin(String userUuid, String organizationUuid) {
        log.info("Checking if user: {} is admin in organization: {}", userUuid, organizationUuid);

        return getUserRoles(userUuid, organizationUuid)
                .map(roles -> roles.stream()
                        .anyMatch(role -> "Admin".equals(role.getRoleName())))
                .defaultIfEmpty(false)
                .doOnSuccess(isAdmin -> log.info("User {} is admin: {} in organization: {}", 
                        userUuid, isAdmin, organizationUuid))
                .doOnError(error -> log.error("Failed to check if user is admin: {}", error.getMessage()));
    }

    @Override
    public Mono<Void> checkUserExistsAndAssignAdminRole(String userUuid, String organizationUuid, String assignedBy) {
        log.info("Checking if user exists and assigning admin role to user: {} in organization: {} by: {}", 
                userUuid, organizationUuid, assignedBy);

        return Mono.fromCallable(() -> {
            // Check if user exists in the database
            UserProfile user = userProfileRepository.findByUserId(organizationUuid, userUuid);
            if (user == null) {
                log.error("User not found in database: {} for organization: {}", userUuid, organizationUuid);
                throw new RuntimeException("User not found in database: " + userUuid);
            }
            log.info("User found in database: {} with username: {}", userUuid, user.getUsername());
            return user;
        })
        .flatMap(user -> {
            // User exists, now assign admin role
            log.info("User exists, proceeding to assign admin role to user: {}", userUuid);
            return assignAdminRoleToUser(userUuid, organizationUuid, assignedBy);
        })
        .doOnSuccess(response -> log.info("Successfully assigned admin role to existing user: {}", userUuid))
        .doOnError(error -> log.error("Failed to assign admin role to user: {} - {}", userUuid, error.getMessage()));
    }

    @Override
    public Mono<Void> checkUserExistsAndAssignRoleByUuid(String userUuid, String roleUuid, String organizationUuid, String assignedBy) {
        log.info("Checking if user exists and assigning role {} to user: {} in organization: {} by: {}", 
                roleUuid, userUuid, organizationUuid, assignedBy);

        return Mono.fromCallable(() -> {
            // Check if user exists in the database
            UserProfile user = userProfileRepository.findByUserId(organizationUuid, userUuid);
            if (user == null) {
                log.error("User not found in database: {} for organization: {}", userUuid, organizationUuid);
                throw new RuntimeException("User not found in database: " + userUuid);
            }
            log.info("User found in database: {} with username: {}", userUuid, user.getUsername());
            return user;
        })
        .flatMap(user -> {
            // User exists, now assign the specified role
            log.info("User exists, proceeding to assign role {} to user: {}", roleUuid, userUuid);
            return rolesServiceClient.assignRoleToUser(userUuid, roleUuid, organizationUuid);
        })
        .doOnSuccess(response -> log.info("Successfully assigned role {} to existing user: {}", roleUuid, userUuid))
        .doOnError(error -> log.error("Failed to assign role {} to user: {} - {}", roleUuid, userUuid, error.getMessage()));
    }
}

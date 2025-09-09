
package com.userapi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.userapi.models.entity.UserProfile;
import com.userapi.models.external.roles.CreateRoleRequest;
import com.userapi.models.external.roles.RoleResponse;
import com.userapi.repository.userprofile.UserProfileRepository;
import com.userapi.service.RolesServiceClient;
import com.userapi.service.UserRolesIntegrationService;
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
    private final ObjectMapper objectMapper;

    @Override
    public Mono<RoleResponse> createDefaultAdminRole(String organizationUuid, String createdBy) {
        log.info("Creating default admin role for organization: {}", organizationUuid);

        try {
            String adminPolicyString = """
                {
                  "version": "1.0",
                  "data": {
                    "read": ["*", "users", "roles", "organization", "tasks", "projects", "reports", "clients"],
                    "write": ["*", "users", "roles", "organization", "tasks", "projects", "reports", "clients"],
                    "delete": ["*", "users", "roles", "organization", "tasks", "projects", "reports", "clients"]
                  },
                  "features": {
                    "execute": ["*", "view_reports", "create_task", "assign_tasks", "manage_team", "view_analytics", "export_data"]
                  }
                }
                """;

            JsonNode adminPolicy = objectMapper.readTree(adminPolicyString);

            CreateRoleRequest request = CreateRoleRequest.builder()
                    .roleName("Admin")
                    .description("Default admin role with full access")
                    .organizationUuid(organizationUuid)
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
            String userPolicyString = """
                {
                  "version": "1.0",
                  "data": {
                    "read": ["users", "tasks", "clients", "organization"],
                    "write": ["tasks"],
                    "delete": []
                  },
                  "features": {
                    "execute": ["create_task", "view_reports"]
                  }
                }
                """;

            JsonNode userPolicy = objectMapper.readTree(userPolicyString);

            CreateRoleRequest request = CreateRoleRequest.builder()
                    .roleName("User")
                    .description("Default user role with limited access")
                    .organizationUuid(organizationUuid)
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
        log.info("Assigning admin role to user: {} in organization: {}", userUuid, organizationUuid);

        // First, try to get the admin role from organization-specific roles
        return rolesServiceClient.getOrganizationRoles(organizationUuid)
                .flatMap(orgRoles -> {
                    RoleResponse adminRole = orgRoles.stream()
                            .filter(role -> "Admin".equals(role.getName()))
                            .findFirst()
                            .orElse(null);

                    if (adminRole != null) {
                        log.info("Found organization-specific admin role: {}", adminRole.getRole_uuid());
                        return rolesServiceClient.assignRoleToUser(userUuid, adminRole.getRole_uuid(), organizationUuid);
                    }

                    // If no organization-specific admin role found, try system-managed roles
                    log.info("No organization-specific admin role found, checking system-managed roles");
                    return rolesServiceClient.getSystemManagedRoles()
                            .flatMap(systemRoles -> {
                                // Log all available system roles for debugging
                                log.info("Available system-managed roles:");
                                systemRoles.forEach(role -> log.info("  - Role: '{}' (UUID: {})", role.getName(), role.getRole_uuid()));

                                // Try to find admin role with different possible names
                                RoleResponse systemAdminRole = systemRoles.stream()
                                        .filter(role -> {
                                            String roleName = role.getName();
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

                                if (systemAdminRole == null) {
                                    log.error("Admin role not found in organization or system-managed roles. Available roles: {}", 
                                            systemRoles.stream().map(RoleResponse::getName).toList());
                                    return Mono.error(new RuntimeException("Admin role not found"));
                                }

                                log.info("Found system-managed admin role: '{}' (UUID: {})", systemAdminRole.getName(), systemAdminRole.getRole_uuid());
                                return rolesServiceClient.assignRoleToUser(userUuid, systemAdminRole.getRole_uuid(), organizationUuid);
                            });
                })
                .doOnSuccess(response -> log.info("Admin role assigned successfully to user: {}", userUuid))
                .doOnError(error -> log.error("Failed to assign admin role to user: {}", error.getMessage()));
    }

    @Override
    public Mono<Void> assignUserRoleToUser(String userUuid, String organizationUuid, String assignedBy) {
        log.info("Assigning user role to user: {} in organization: {}", userUuid, organizationUuid);
        
        // First, get the user role for the organization
        return rolesServiceClient.getOrganizationRoles(organizationUuid)
                .flatMap(roles -> {
                    RoleResponse userRole = roles.stream()
                            .filter(role -> "User".equals(role.getName()))
                            .findFirst()
                            .orElse(null);
                    
                    if (userRole == null) {
                        log.error("User role not found for organization: {}", organizationUuid);
                        return Mono.error(new RuntimeException("User role not found"));
                    }
                    
                    return rolesServiceClient.assignRoleToUser(userUuid, userRole.getRole_uuid(), organizationUuid);
                })
                .doOnSuccess(response -> log.info("User role assigned successfully to user: {}", userUuid))
                .doOnError(error -> log.error("Failed to assign user role to user: {}", error.getMessage()));
    }

    @Override
    public Mono<List<RoleResponse>> getUserRoles(String userUuid, String organizationUuid) {
        log.info("Getting roles for user: {} in organization: {}", userUuid, organizationUuid);
        
        return rolesServiceClient.getUserRoles(userUuid, organizationUuid)
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
                        .anyMatch(role -> "Admin".equals(role.getName())))
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

package com.userapi.security;

import com.userapi.service.RolesServiceClient;
import com.userapi.service.UserContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.Duration;

/**
 * Custom permission evaluator that integrates with the roles and permissions service
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private final RolesServiceClient rolesServiceClient;
    private final UserContextService userContextService;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || permission == null) {
            log.debug("Authentication or permission is null");
            return false;
        }

        String userUuid = userContextService.getCurrentUserUuid();
        String orgUuid = userContextService.getCurrentOrgUuid();
        
        if (userUuid == null || orgUuid == null) {
            log.warn("Missing user context - User UUID: {}, Org UUID: {}", userUuid, orgUuid);
            return false;
        }

        String resource = targetDomainObject != null ? targetDomainObject.toString() : "UNKNOWN";
        String action = permission.toString();

        // Map User Management Service permissions to Roles Service format
        resource = mapResourceToRolesService(resource);
        action = mapActionToRolesService(action);

        log.debug("Checking permission - User: {}, Org: {}, Resource: {}, Action: {}", 
                 userUuid, orgUuid, resource, action);

        try {
            // Call the roles service to check permission
            Boolean hasPermission = rolesServiceClient
                .hasPermission(userUuid, orgUuid, resource, action)
                .timeout(Duration.ofSeconds(5))
                .block();

            boolean result = Boolean.TRUE.equals(hasPermission);
            log.debug("Permission check result: {} for user: {} on resource: {} with action: {}", 
                     result, userUuid, resource, action);
            
            return result;
        } catch (Exception e) {
            log.error("Error checking permission for user: {} on resource: {} with action: {} - {}", 
                     userUuid, resource, action, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, 
                               String targetType, Object permission) {
        if (authentication == null || permission == null) {
            log.debug("Authentication or permission is null");
            return false;
        }

        String userUuid = userContextService.getCurrentUserUuid();
        String orgUuid = userContextService.getCurrentOrgUuid();
        
        if (userUuid == null || orgUuid == null) {
            log.warn("Missing user context - User UUID: {}, Org UUID: {}", userUuid, orgUuid);
            return false;
        }

        String resource = targetType != null ? targetType : "UNKNOWN";
        String action = permission.toString();

        // Map User Management Service permissions to Roles Service format
        resource = mapResourceToRolesService(resource);
        action = mapActionToRolesService(action);

        // If targetId is provided, append it to the resource for more specific permission checking
        if (targetId != null) {
            resource = resource + ":" + targetId.toString();
        }

        log.debug("Checking permission with target ID - User: {}, Org: {}, Resource: {}, Action: {}", 
                 userUuid, orgUuid, resource, action);

        try {
            // Call the roles service to check permission
            Boolean hasPermission = rolesServiceClient
                .hasPermission(userUuid, orgUuid, resource, action)
                .timeout(Duration.ofSeconds(5))
                .block();

            boolean result = Boolean.TRUE.equals(hasPermission);
            log.debug("Permission check result: {} for user: {} on resource: {} with action: {}", 
                     result, userUuid, resource, action);
            
            return result;
        } catch (Exception e) {
            log.error("Error checking permission for user: {} on resource: {} with action: {} - {}", 
                     userUuid, resource, action, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if user has permission on a specific resource instance
     */
    public boolean hasPermissionOnResource(String resourceType, String resourceId, String action) {
        String userUuid = userContextService.getCurrentUserUuid();
        String orgUuid = userContextService.getCurrentOrgUuid();
        
        if (userUuid == null || orgUuid == null) {
            log.warn("Missing user context for resource permission check");
            return false;
        }

        String resource = resourceType + (resourceId != null ? ":" + resourceId : "");
        
        try {
            Boolean hasPermission = rolesServiceClient
                .hasPermission(userUuid, orgUuid, resource, action)
                .timeout(Duration.ofSeconds(5))
                .block();

            return Boolean.TRUE.equals(hasPermission);
        } catch (Exception e) {
            log.error("Error checking resource permission: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Map User Management Service resource names to Roles Service format
     */
    private String mapResourceToRolesService(String resource) {
        if (resource == null) return "UNKNOWN";

        log.debug("Mapping resource: {} to roles service format", resource);
        
        switch (resource.toUpperCase()) {
            case "ROLE":
                // For admin-level permissions, use wildcard to match admin role policy
                log.debug("Mapping ROLE resource - using wildcard '*' for admin permissions");
                return "*";
            case "USER":
                // Map USER to users for proper permission checking
                log.debug("Mapping USER resource to 'users' for permission checking");
                return "users";
            case "TASK":
                return "tasks";
            case "CLIENT":
                return "clients";
            case "ORGANIZATION":
                return "organization";
            case "SYSTEM_ROLE":
                return "roles";
            case "USER_ROLE":
                return "users";
            case "PERMISSION":
                return "roles";
            default:
                log.debug("Using default resource mapping: {}", resource.toLowerCase());
                return resource.toLowerCase();
        }
    }

    /**
     * Map User Management Service action names to Roles Service format
     */
    private String mapActionToRolesService(String action) {
        if (action == null) return "read";

        switch (action.toUpperCase()) {
            case "CREATE":
                return "write";
            case "READ":
            case "VIEW":
                return "read";
            case "UPDATE":
            case "EDIT":
                return "write";
            case "DELETE":
            case "REMOVE":
                return "delete";
            case "ASSIGN":
            case "ASSIGN_ADMIN":
                return "write";
            case "CHECK":
                return "read";
            default:
                return action.toLowerCase();
        }
    }
}
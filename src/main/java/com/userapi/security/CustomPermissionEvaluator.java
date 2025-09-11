package com.userapi.security;

import com.userapi.models.external.roles.RoleResponse;
import com.userapi.service.RolesServiceClient;
import com.userapi.service.UserContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;

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

        if (targetDomainObject == null) {
            log.warn("Target domain object is null - cannot determine resource for permission check");
            return false;
        }
        
        String resource = targetDomainObject.toString();
        String action = permission.toString();

        // Map User Management Service permissions to Roles Service format
        resource = mapResourceToRolesService(resource);
        action = mapActionToRolesService(action);

        log.debug("Checking permission - User: {}, Org: {}, Resource: {}, Action: {}", 
                 userUuid, orgUuid, resource, action);
        
        // Debug: Check what roles the user actually has
        try {
            log.debug("DEBUG: Checking user roles for user: {} in org: {}", userUuid, orgUuid);
            List<RoleResponse> userRoles = rolesServiceClient.getUserRoles(userUuid, orgUuid)
                .timeout(Duration.ofSeconds(5))
                .block();
            if (userRoles != null && !userRoles.isEmpty()) {
                log.info("DEBUG: User {} has {} roles assigned: {}", userUuid, userRoles.size(), 
                    userRoles.stream().map(RoleResponse::getRoleName).collect(java.util.stream.Collectors.toList()));
                // Log the first role's policy for debugging
                if (!userRoles.isEmpty()) {
                    RoleResponse firstRole = userRoles.get(0);
                    log.info("DEBUG: First role '{}' policy: {}", firstRole.getRoleName(), firstRole.getPolicy());
                }
            } else {
                log.warn("DEBUG: User {} has NO roles assigned in org {}", userUuid, orgUuid);
            }
        } catch (Exception e) {
            log.error("DEBUG: Could not check user roles: {}", e.getMessage());
        }

        try {
            // First, try the external roles service
            Boolean hasPermission = rolesServiceClient
                .hasPermission(userUuid, orgUuid, resource, action)
                .timeout(Duration.ofSeconds(5))
                .block();

            boolean result = Boolean.TRUE.equals(hasPermission);
            log.debug("External roles service permission check result: {} for user: {} on resource: {} with action: {}", 
                     result, userUuid, resource, action);
            
            // If external service denies permission, check locally for wildcard permissions
            if (!result) {
                log.debug("External service denied permission, checking local wildcard permissions");
                result = checkLocalWildcardPermission(userUuid, orgUuid, resource, action);
            }
            
            return result;
        } catch (Exception e) {
            log.error("Error checking permission for user: {} on resource: {} with action: {} - {}", 
                     userUuid, resource, action, e.getMessage(), e);
            
            // If external service fails, fall back to local wildcard check
            log.debug("External service failed, falling back to local wildcard permission check");
            return checkLocalWildcardPermission(userUuid, orgUuid, resource, action);
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

        if (targetType == null) {
            log.warn("Target type is null - cannot determine resource for permission check");
            return false;
        }
        
        String resource = targetType;
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
        if (resource == null) {
            log.error("Resource is null - cannot map to roles service format");
            throw new IllegalArgumentException("Resource cannot be null");
        }

        log.debug("Mapping resource: {} to roles service format", resource);
        
        switch (resource.toUpperCase()) {
            case "ROLE":
                // Map ROLE to roles for proper permission checking
                log.debug("Mapping ROLE resource to 'roles' for permission checking");
                return "roles";
            case "USER":
                // Map USER to users for proper permission checking
                log.debug("Mapping USER resource to 'users' for permission checking");
                return "users";
            case "TASK":
                return "tasks";
            case "CLIENT":
                return "clients";
            case "ORGANIZATION":
                return "organizations";
            case "SYSTEM_ROLE":
                return "system_roles";
            case "USER_ROLE":
                return "user_roles";
            case "PERMISSION":
                return "permissions";
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

    /**
     * Check if user has wildcard permission locally by examining their roles and policies
     */
    private boolean checkLocalWildcardPermission(String userUuid, String orgUuid, String resource, String action) {
        try {
            log.debug("Checking local wildcard permissions for user: {} on resource: {} with action: {}", 
                     userUuid, resource, action);
            
            // Get user's roles
            List<RoleResponse> userRoles = rolesServiceClient.getUserRoles(userUuid, orgUuid)
                .timeout(Duration.ofSeconds(5))
                .block();
            
            if (userRoles == null || userRoles.isEmpty()) {
                log.debug("No roles found for user: {} in organization: {}", userUuid, orgUuid);
                return false;
            }
            
            // Check each role's policy for wildcard permissions
            for (RoleResponse role : userRoles) {
                if (hasWildcardPermissionInPolicy(role.getPolicy().toString(), action)) {
                    log.info("Wildcard permission found in role '{}' for user: {} on action: {}", 
                            role.getRoleName(), userUuid, action);
                    return true;
                }
            }
            
            log.debug("No wildcard permissions found for user: {} on action: {}", userUuid, action);
            return false;
            
        } catch (Exception e) {
            log.error("Error checking local wildcard permission for user: {} on action: {} - {}", 
                     userUuid, action, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Check if a role policy contains wildcard permission for the given action
     */
    private boolean hasWildcardPermissionInPolicy(String policyJson, String action) {
        try {
            if (policyJson == null || policyJson.trim().isEmpty()) {
                return false;
            }
            
            // Parse the policy JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode policy = mapper.readTree(policyJson);
            
            // Check data permissions
            if (policy.has("data")) {
                com.fasterxml.jackson.databind.JsonNode dataNode = policy.get("data");
                if (dataNode.has(action)) {
                    com.fasterxml.jackson.databind.JsonNode actionNode = dataNode.get(action);
                    if (actionNode.isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode resourceNode : actionNode) {
                            String allowedResource = resourceNode.asText();
                            if ("*".equals(allowedResource)) {
                                log.debug("Found wildcard permission in data section for action: {}", action);
                                return true;
                            }
                        }
                    }
                }
            }
            
            // Check feature permissions
            if (policy.has("features")) {
                com.fasterxml.jackson.databind.JsonNode featuresNode = policy.get("features");
                if (featuresNode.has(action)) {
                    com.fasterxml.jackson.databind.JsonNode actionNode = featuresNode.get(action);
                    if (actionNode.isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode resourceNode : actionNode) {
                            String allowedResource = resourceNode.asText();
                            if ("*".equals(allowedResource)) {
                                log.debug("Found wildcard permission in features section for action: {}", action);
                                return true;
                            }
                        }
                    }
                }
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("Error parsing policy JSON: {}", e.getMessage(), e);
            return false;
        }
    }

}
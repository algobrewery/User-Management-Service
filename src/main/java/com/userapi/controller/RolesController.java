package com.userapi.controller;

import com.userapi.enums.PermissionResult;
import com.userapi.enums.ResourceType;
import com.userapi.exception.RolesServiceExceptionHandler;
import com.userapi.models.external.roles.*;
import com.userapi.service.RolesServiceClient;
import com.userapi.service.UserRolesIntegrationService;
import com.userapi.validation.HeaderValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
@Slf4j
public class RolesController {

    private final RolesServiceClient rolesServiceClient;
    private final UserRolesIntegrationService userRolesIntegrationService;

    @PostMapping
    @PreAuthorize("hasPermission('ROLE', 'CREATE')")
    public Mono<ResponseEntity<RoleResponse>> createRole(
            @Valid @RequestBody CreateRoleRequest request,
            HttpServletRequest httpRequest) {
        
        // Validate required headers
        HeaderValidationUtil.validateRequiredHeaders(httpRequest);
        
        String organizationUuid = HeaderValidationUtil.getValidatedOrganizationUuid(httpRequest);
        String userUuid = HeaderValidationUtil.getValidatedUserUuid(httpRequest);
        
        log.info("Creating role: {} for organization: {} by user: {} on resource: {}", 
                request.getRoleName(), organizationUuid, userUuid, ResourceType.ROLES.getResourceName());
        
        return rolesServiceClient.createRole(request, organizationUuid)
                .map(response -> {
                    log.info("Role created successfully: {} with UUID: {}", response.getRoleName(), response.getRole_uuid());
                    return ResponseEntity.status(HttpStatus.CREATED).body(response);
                })
                .onErrorResume(error -> RolesServiceExceptionHandler.handleRoleCreationError(
                        request.getRoleName(), organizationUuid, error));
    }

    @GetMapping("/{roleUuid}")
    public Mono<ResponseEntity<RoleResponse>> getRole(
            @PathVariable String roleUuid,
            HttpServletRequest httpRequest) {
        
        // Validate required headers
        HeaderValidationUtil.validateRequiredHeaders(httpRequest);
        
        String organizationUuid = HeaderValidationUtil.getValidatedOrganizationUuid(httpRequest);
        
        log.info("Getting role: {} for organization: {}", roleUuid, organizationUuid);
        
        return rolesServiceClient.getRoleByUuid(roleUuid, organizationUuid)
                .map(response -> {
                    log.info("Role retrieved successfully: {} with UUID: {}", response.getRoleName(), response.getRole_uuid());
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> RolesServiceExceptionHandler.handleRoleRetrievalError(
                        roleUuid, organizationUuid, error));
    }

    // ========== NEW UNIFIED SEARCH ENDPOINT ==========
    
    @PostMapping("/search")
    public Mono<ResponseEntity<ListRolesResponse>> searchRoles(
            @Valid @RequestBody ListRolesRequest request,
            HttpServletRequest httpRequest) {
        
        // Validate required headers
        HeaderValidationUtil.validateRequiredHeaders(httpRequest);
        
        String organizationUuid = HeaderValidationUtil.getValidatedOrganizationUuid(httpRequest);
        String userUuid = HeaderValidationUtil.getValidatedUserUuid(httpRequest);
        
        log.info("Searching roles for organization: {} by user: {} with filters: {} on resource: {}", 
                organizationUuid, userUuid, request.getFilterCriteria(), ResourceType.ROLES.getResourceName());
        
        return rolesServiceClient.searchRoles(request, organizationUuid)
                .map(response -> {
                    log.info("Found {} roles for organization: {} (page: {}, size: {})", 
                            response.getTotalElements(), organizationUuid, response.getCurrentPage(), response.getPageSize());
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    log.error("Failed to search roles for organization: {} - {}", organizationUuid, error.getMessage());
                    
                    if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                        org.springframework.web.reactive.function.client.WebClientResponseException wcre = 
                            (org.springframework.web.reactive.function.client.WebClientResponseException) error;
                        
                        if (wcre.getStatusCode().is4xxClientError()) {
                            return Mono.just(ResponseEntity.status(wcre.getStatusCode()).build());
                        } else {
                            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                        }
                    }
                    
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }


    @PutMapping("/{roleUuid}")
    @PreAuthorize("hasPermission('ROLE', 'UPDATE')")
    public Mono<ResponseEntity<RoleResponse>> updateRole(
            @PathVariable String roleUuid,
            @Valid @RequestBody UpdateRoleRequest request,
            HttpServletRequest httpRequest) {
        
        // Validate required headers
        HeaderValidationUtil.validateRequiredHeaders(httpRequest);
        
        String organizationUuid = HeaderValidationUtil.getValidatedOrganizationUuid(httpRequest);
        String userUuid = HeaderValidationUtil.getValidatedUserUuid(httpRequest);
        
        log.info("Updating role: {} for organization: {} by user: {} with fields: {} on resource: {}", 
                roleUuid, organizationUuid, userUuid, 
                "description=" + (request.getDescription() != null) + 
                ", policy=" + (request.getPolicy() != null) + 
                ", status=" + (request.getStatus() != null), ResourceType.ROLES.getResourceName());
        
        return rolesServiceClient.updateRole(roleUuid, request, organizationUuid)
                .map(response -> {
                    log.info("Role updated successfully: {} with UUID: {}", response.getRoleName(), response.getRole_uuid());
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> RolesServiceExceptionHandler.handleRoleUpdateError(
                        roleUuid, organizationUuid, error));
    }

    @DeleteMapping("/{roleUuid}")
    @PreAuthorize("hasPermission('ROLE', 'DELETE')")
    public Mono<ResponseEntity<String>> deleteRole(
            @PathVariable String roleUuid,
            HttpServletRequest httpRequest) {
        
        // Validate required headers
        HeaderValidationUtil.validateRequiredHeaders(httpRequest);
        
        String organizationUuid = HeaderValidationUtil.getValidatedOrganizationUuid(httpRequest);
        String userUuid = HeaderValidationUtil.getValidatedUserUuid(httpRequest);
        
        log.info("Deleting role: {} for organization: {} by user: {} on resource: {}", 
                roleUuid, organizationUuid, userUuid, ResourceType.ROLES.getResourceName());
        
        return rolesServiceClient.deleteRole(roleUuid, organizationUuid)
                .then(Mono.just(ResponseEntity.ok("Role " + roleUuid + " deleted successfully")))
                .onErrorResume(error -> RolesServiceExceptionHandler.handleRoleDeletionError(
                        roleUuid, organizationUuid, error));
    }

    @PostMapping("/user/{userUuid}/assign")
    // @PreAuthorize("hasPermission('USER_ROLES', 'ASSIGN')")  // DISABLED FOR FINAL BOOTSTRAP
    public Mono<ResponseEntity<String>> assignRoleToUser(
            @PathVariable String userUuid,
            @Valid @RequestBody AssignRoleRequest request,
            HttpServletRequest httpRequest) {
        
        // Validate required headers
        HeaderValidationUtil.validateRequiredHeaders(httpRequest);
        
        String organizationUuid = HeaderValidationUtil.getValidatedOrganizationUuid(httpRequest);
        String assignedBy = HeaderValidationUtil.getValidatedUserUuid(httpRequest);
        
        log.info("Assigning role: {} to user: {} in organization: {} by user: {} on resource: {}", 
                request.getRole_uuid(), userUuid, organizationUuid, assignedBy, ResourceType.USER_ROLES.getResourceName());
        
        return rolesServiceClient.assignRoleToUser(userUuid, request.getRole_uuid(), organizationUuid)
                .then(Mono.just(ResponseEntity.ok("Role " + request.getRole_uuid() + " assigned successfully to user: " + userUuid)))
                .onErrorResume(error -> RolesServiceExceptionHandler.handleUserRoleAssignmentError(
                        userUuid, request.getRole_uuid(), organizationUuid, error));
    }


    @DeleteMapping("/user/{userUuid}/roles/{roleUuid}")
    @PreAuthorize("hasPermission('USER_ROLES', 'REMOVE')")
    public Mono<ResponseEntity<String>> removeRoleFromUser(
            @PathVariable String userUuid,
            @PathVariable String roleUuid,
            HttpServletRequest httpRequest) {
        
        // Validate required headers
        HeaderValidationUtil.validateRequiredHeaders(httpRequest);
        
        String organizationUuid = HeaderValidationUtil.getValidatedOrganizationUuid(httpRequest);
        String removedBy = HeaderValidationUtil.getValidatedUserUuid(httpRequest);
        
        log.info("Removing role: {} from user: {} in organization: {} by user: {} on resource: {}", 
                roleUuid, userUuid, organizationUuid, removedBy, ResourceType.USER_ROLES.getResourceName());
        
        return rolesServiceClient.removeRoleFromUser(userUuid, roleUuid, organizationUuid)
                .then(Mono.just(ResponseEntity.ok("Role " + roleUuid + " removed successfully from user " + userUuid)))
                .onErrorResume(error -> RolesServiceExceptionHandler.handleUserRoleRemovalError(
                        userUuid, roleUuid, organizationUuid, error));
    }

    @PostMapping("/permissions/check")
    public Mono<ResponseEntity<PermissionCheckResponse>> checkPermission(
            @Valid @RequestBody PermissionCheckRequest request,
            HttpServletRequest httpRequest) {

        // Validate required headers
        HeaderValidationUtil.validateRequiredHeaders(httpRequest);
        
        String userUuid = HeaderValidationUtil.getValidatedUserUuid(httpRequest);
        String organizationUuid = HeaderValidationUtil.getValidatedOrganizationUuid(httpRequest);

        log.info("Checking permission for user: {} on resource: {} with action: {}", 
                userUuid, request.getResource(), request.getAction());

        return rolesServiceClient.hasPermission(userUuid, organizationUuid, request.getResource(), request.getAction())
                .map(hasPermission -> {
                    PermissionResult result = hasPermission ? PermissionResult.ACCEPTED : PermissionResult.DENIED;
                    PermissionCheckResponse response = PermissionCheckResponse.builder()
                            .result(result)
                            .resource(request.getResource())
                            .action(request.getAction())
                            .build();
                    
                    log.info("Permission check result: {} for user: {} on resource: {} with action: {}", 
                            result, userUuid, request.getResource(), request.getAction());
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> RolesServiceExceptionHandler.handlePermissionCheckError(
                        userUuid, request.getResource(), request.getAction(), error));
    }

    @PostMapping("/user/{userUuid}/assign-admin")
    // @PreAuthorize("hasPermission('USER_ROLES', 'ASSIGN_ADMIN')")  // DISABLED FOR BOOTSTRAP
    public Mono<ResponseEntity<String>> assignAdminRoleToExistingUser(
            @PathVariable String userUuid,
            HttpServletRequest httpRequest) {

        // Validate required headers
        HeaderValidationUtil.validateRequiredHeaders(httpRequest);

        String organizationUuid = HeaderValidationUtil.getValidatedOrganizationUuid(httpRequest);
        String assignedBy = HeaderValidationUtil.getValidatedUserUuid(httpRequest);

        log.info("Assigning admin role to existing user: {} in organization: {} by user: {} on resource: {}", 
                userUuid, organizationUuid, assignedBy, ResourceType.USER_ROLES.getResourceName());

        return userRolesIntegrationService.checkUserExistsAndAssignAdminRole(userUuid, organizationUuid, assignedBy)
                .then(Mono.just(ResponseEntity.ok("Admin role assigned successfully to user: " + userUuid)))
                .onErrorResume(error -> {
                    log.error("Failed to assign admin role to user: {} - {}", userUuid, error.getMessage());
                    if (error.getMessage().contains("User not found")) {
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body("User not found in database: " + userUuid));
                    } else if (error.getMessage().contains("Admin role not found")) {
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body("Admin role not found for organization: " + organizationUuid));
                    } else {
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Failed to assign admin role: " + error.getMessage()));
                    }
                });
    }

    @PostMapping("/user/{userUuid}/assign-role/{roleUuid}")
    // @PreAuthorize("hasPermission('USER_ROLES', 'ASSIGN')")  // DISABLED FOR BOOTSTRAP
    public Mono<ResponseEntity<String>> assignRoleByUuidToExistingUser(
            @PathVariable String userUuid,
            @PathVariable String roleUuid,
            HttpServletRequest httpRequest) {

        // Validate required headers
        HeaderValidationUtil.validateRequiredHeaders(httpRequest);

        String organizationUuid = HeaderValidationUtil.getValidatedOrganizationUuid(httpRequest);
        String assignedBy = HeaderValidationUtil.getValidatedUserUuid(httpRequest);

        log.info("Assigning role {} to existing user: {} in organization: {} by user: {}", 
                roleUuid, userUuid, organizationUuid, assignedBy);

        return userRolesIntegrationService.checkUserExistsAndAssignRoleByUuid(userUuid, roleUuid, organizationUuid, assignedBy)
                .then(Mono.just(ResponseEntity.ok("Role " + roleUuid + " assigned successfully to user: " + userUuid)))
                .onErrorResume(error -> {
                    log.error("Failed to assign role {} to user: {} - {}", roleUuid, userUuid, error.getMessage());
                    if (error.getMessage().contains("User not found")) {
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body("User not found in database: " + userUuid));
                    } else {
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Failed to assign role: " + error.getMessage()));
                    }
                });
    }

    // ========== BOOTSTRAP ENDPOINTS (NO AUTHORIZATION REQUIRED) ==========

    @PostMapping("/bootstrap/create")
    public Mono<ResponseEntity<RoleResponse>> bootstrapCreateRole(
            @Valid @RequestBody CreateRoleRequest request,
            HttpServletRequest httpRequest) {

        // Validate required headers for bootstrap operations
        HeaderValidationUtil.validateRequiredHeaders(httpRequest);
        
        String organizationUuid = HeaderValidationUtil.getValidatedOrganizationUuid(httpRequest);
        String userUuid = HeaderValidationUtil.getValidatedUserUuid(httpRequest);

        log.info("BOOTSTRAP: Creating role: {} for organization: {} by user: {} on resource: {}", 
                request.getRoleName(), organizationUuid, userUuid, ResourceType.ROLES.getResourceName());

        return rolesServiceClient.createRole(request, organizationUuid)
                .map(response -> {
                    log.info("BOOTSTRAP: Role created successfully: {} with UUID: {}", response.getRoleName(), response.getRole_uuid());
                    return ResponseEntity.status(HttpStatus.CREATED).body(response);
                })
                .onErrorResume(error -> {
                    log.error("BOOTSTRAP: Failed to create role: {} for organization: {} - {}", 
                            request.getRoleName(), organizationUuid, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new RoleResponse()));
                });
    }

    @GetMapping("/bootstrap/system-managed")
    public Mono<ResponseEntity<List<RoleResponse>>> bootstrapGetSystemManagedRoles() {
        log.info("BOOTSTRAP: Getting system managed roles");

        // Use the search API to get system managed roles
        ListRolesRequest request = ListRolesRequest.builder()
                .filterCriteria(ListRolesFilterCriteria.builder()
                        .attributes(List.of(
                                ListRolesFilterCriteriaAttribute.builder()
                                        .name("role_management_type")
                                        .values(List.of("SYSTEM_MANAGED"))
                                        .build()
                        ))
                        .build())
                .build();

        return rolesServiceClient.searchRoles(request, "system")
                .map(response -> {
                    log.info("BOOTSTRAP: Retrieved {} system managed roles", response.getRoles().size());
                    return ResponseEntity.ok(response.getRoles());
                })
                .onErrorResume(error -> {
                    log.error("BOOTSTRAP: Failed to get system managed roles - {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(List.<RoleResponse>of()));
                });
    }

    @PostMapping("/bootstrap/organization/{organizationUuid}/setup")
    public Mono<ResponseEntity<String>> bootstrapOrganizationSetup(
            @PathVariable String organizationUuid,
            @RequestParam String adminUserUuid) {

        log.info("BOOTSTRAP: Setting up organization: {} with admin user: {}", organizationUuid, adminUserUuid);

        return userRolesIntegrationService.createDefaultAdminRole(organizationUuid, adminUserUuid)
                .flatMap(adminRole -> userRolesIntegrationService.createDefaultUserRole(organizationUuid, adminUserUuid)
                        .map(userRole -> adminRole))
                .flatMap(adminRole -> userRolesIntegrationService.assignAdminRoleToUser(adminUserUuid, organizationUuid, "system", adminRole.getRole_uuid())
                        .thenReturn(adminRole))
                .map(adminRole -> {
                    log.info("BOOTSTRAP: Organization setup complete for organization: {} with admin user: {}", organizationUuid, adminUserUuid);
                    return ResponseEntity.ok("Organization setup complete. Admin role created and assigned to user: " + adminUserUuid);
                })
                .onErrorResume(error -> RolesServiceExceptionHandler.handleBootstrapError(
                        "organization setup", "organization: " + organizationUuid + " with admin: " + adminUserUuid, error));
    }

    @PostMapping("/bootstrap/user/{userUuid}/assign-admin")
    public Mono<ResponseEntity<String>> bootstrapAssignAdminRole(
            @PathVariable String userUuid,
            @RequestParam String organizationUuid,
            @RequestParam(defaultValue = "system") String assignedBy) {

        log.info("BOOTSTRAP: Assigning admin role to user: {} in organization: {}", userUuid, organizationUuid);

        return userRolesIntegrationService.assignAdminRoleToUser(userUuid, organizationUuid, assignedBy)
                .then(Mono.just(ResponseEntity.ok("Admin role assigned successfully to user: " + userUuid)))
                .onErrorResume(error -> RolesServiceExceptionHandler.handleBootstrapError(
                        "admin role assignment", "user: " + userUuid + " in organization: " + organizationUuid, error));
    }
}

package com.userapi.controller;

import com.userapi.common.validation.HeaderValidator;
import com.userapi.exception.WebClientErrorHandler;
import com.userapi.models.external.roles.*;
import com.userapi.service.RolesServiceClient;
import com.userapi.service.UserRolesIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
@Slf4j
public class RolesController {

    private final RolesServiceClient rolesServiceClient;
    private final UserRolesIntegrationService userRolesIntegrationService;

    /**
     * Helper method to get list of fields being updated for logging
     */
    private String getUpdateableFields(UpdateRoleRequest request) {
        StringBuilder fields = new StringBuilder();
        if (request.getDescription() != null) fields.append("description ");
        if (request.getPolicy() != null) fields.append("policy ");
        if (request.getIs_active() != null) fields.append("is_active ");
        return fields.toString().trim();
    }

    @PostMapping
    @PreAuthorize("hasPermission('ROLE', 'CREATE')")
    public Mono<ResponseEntity<RoleResponse>> createRole(
            @Valid @RequestBody CreateRoleRequest request,
            HttpServletRequest httpRequest) {
        
        // Validate required headers
        HeaderValidator.ValidatedHeaders headers = HeaderValidator.getValidatedHeaders(httpRequest);
        String organizationUuid = headers.getOrganizationUuid();
        String userUuid = headers.getUserUuid();
        
        log.info("Creating role: {} for organization: {} by user: {} (traceId: {})", 
                request.getRoleName(), organizationUuid, userUuid, headers.getTraceId());
        
        return rolesServiceClient.createRole(request, organizationUuid)
                .map(response -> {
                    log.info("Role created successfully: {} with name: {}", response.getRole_uuid(), response.getName());
                    return ResponseEntity.status(HttpStatus.CREATED).body(response);
                })
                .onErrorResume(error -> WebClientErrorHandler.handleWebClientError(error, "create role", 
                        "role=" + request.getRoleName() + ", org=" + organizationUuid));
    }

    @GetMapping("/{roleUuid}")
    public Mono<ResponseEntity<RoleResponse>> getRole(
            @PathVariable String roleUuid,
            HttpServletRequest httpRequest) {
        
        // Validate required headers
        HeaderValidator.ValidatedHeaders headers = HeaderValidator.getValidatedHeaders(httpRequest);
        String organizationUuid = headers.getOrganizationUuid();
        
        log.info("Getting role: {} for organization: {} (traceId: {})", 
                roleUuid, organizationUuid, headers.getTraceId());
        
        return rolesServiceClient.getRoleByUuid(roleUuid, organizationUuid)
                .map(response -> {
                    log.info("Role retrieved successfully: {} with name: {}", response.getRole_uuid(), response.getName());
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> WebClientErrorHandler.handleWebClientError(error, "get role", 
                        "roleUuid=" + roleUuid + ", org=" + organizationUuid));
    }

    @PostMapping("/filter")
    @PreAuthorize("hasPermission('ROLE', 'READ')")
    public Mono<ResponseEntity<ListRolesResponse>> filterRoles(
            @Valid @RequestBody ListRolesRequest request,
            HttpServletRequest httpRequest) {
        
        // Validate required headers
        HeaderValidator.ValidatedHeaders headers = HeaderValidator.getValidatedHeaders(httpRequest);
        String organizationUuid = headers.getOrganizationUuid();
        String userUuid = headers.getUserUuid();
        
        log.info("Filtering roles for organization: {} by user: {} with criteria: {} (traceId: {})", 
                organizationUuid, userUuid, request, headers.getTraceId());
        
        return rolesServiceClient.filterRoles(request, organizationUuid)
                .map(response -> {
                    log.info("Found {} roles for organization: {}", response.getTotalElements(), organizationUuid);
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> WebClientErrorHandler.handleWebClientError(error, "filter roles", 
                        "org=" + organizationUuid + ", page=" + request.getPage() + ", size=" + request.getSize()));
    }

    @PutMapping("/{roleUuid}")
    @PreAuthorize("hasPermission('ROLE', 'UPDATE')")
    public Mono<ResponseEntity<RoleResponse>> updateRole(
            @PathVariable String roleUuid,
            @Valid @RequestBody UpdateRoleRequest request,
            HttpServletRequest httpRequest) {
        
        // Validate required headers
        HeaderValidator.ValidatedHeaders headers = HeaderValidator.getValidatedHeaders(httpRequest);
        String organizationUuid = headers.getOrganizationUuid();
        String userUuid = headers.getUserUuid();
        
        log.info("Updating role: {} for organization: {} by user: {} - updating fields: {} (traceId: {})", 
                roleUuid, organizationUuid, userUuid, getUpdateableFields(request), headers.getTraceId());
        
        return rolesServiceClient.updateRole(roleUuid, request, organizationUuid)
                .map(response -> {
                    log.info("Role updated successfully: {} with name: {}", response.getRole_uuid(), response.getName());
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> WebClientErrorHandler.handleWebClientError(error, "update role", 
                        "roleUuid=" + roleUuid + ", org=" + organizationUuid));
    }

    @DeleteMapping("/{roleUuid}")
    @PreAuthorize("hasPermission('ROLE', 'DELETE')")
    public Mono<ResponseEntity<String>> deleteRole(
            @PathVariable String roleUuid,
            HttpServletRequest httpRequest) {
        
        // Validate required headers
        HeaderValidator.ValidatedHeaders headers = HeaderValidator.getValidatedHeaders(httpRequest);
        String organizationUuid = headers.getOrganizationUuid();
        String userUuid = headers.getUserUuid();
        
        log.info("Deleting role: {} for organization: {} by user: {} (traceId: {})", 
                roleUuid, organizationUuid, userUuid, headers.getTraceId());
        
        return rolesServiceClient.deleteRole(roleUuid, organizationUuid)
                .then(Mono.just(ResponseEntity.ok("Role " + roleUuid + " deleted successfully")))
                .onErrorResume(error -> WebClientErrorHandler.handleWebClientErrorForString(error, "delete role", 
                        "roleUuid=" + roleUuid + ", org=" + organizationUuid));
    }

    @PostMapping("/user/{userUuid}/assign")
    // @PreAuthorize("hasPermission('USER_ROLE', 'ASSIGN')")  // DISABLED FOR FINAL BOOTSTRAP
    public Mono<ResponseEntity<String>> assignRoleToUser(
            @PathVariable String userUuid,
            @Valid @RequestBody AssignRoleRequest request,
            HttpServletRequest httpRequest) {
        
        // Validate required headers
        HeaderValidator.ValidatedHeaders headers = HeaderValidator.getValidatedHeaders(httpRequest);
        String organizationUuid = headers.getOrganizationUuid();
        String assignedBy = headers.getUserUuid();
        
        log.info("Assigning role: {} to user: {} in organization: {} by user: {} (traceId: {})", 
                request.getRole_uuid(), userUuid, organizationUuid, assignedBy, headers.getTraceId());
        
        return rolesServiceClient.assignRoleToUser(userUuid, request.getRole_uuid(), organizationUuid)
                .then(Mono.just(ResponseEntity.ok("Role " + request.getRole_uuid() + " assigned successfully to user: " + userUuid)))
                .onErrorResume(error -> WebClientErrorHandler.handleWebClientErrorForString(error, "assign role", 
                        "user=" + userUuid + ", role=" + request.getRole_uuid() + ", org=" + organizationUuid));
    }

    @DeleteMapping("/user/{userUuid}/roles/{roleUuid}")
    public Mono<ResponseEntity<String>> removeRoleFromUser(
            @PathVariable String userUuid,
            @PathVariable String roleUuid,
            HttpServletRequest httpRequest) {
        
        // Validate required headers
        HeaderValidator.ValidatedHeaders headers = HeaderValidator.getValidatedHeaders(httpRequest);
        String organizationUuid = headers.getOrganizationUuid();
        String removedBy = headers.getUserUuid();
        
        log.info("Removing role: {} from user: {} in organization: {} by user: {} (traceId: {})", 
                roleUuid, userUuid, organizationUuid, removedBy, headers.getTraceId());
        
        return rolesServiceClient.removeRoleFromUser(userUuid, roleUuid, organizationUuid)
                .then(Mono.just(ResponseEntity.ok("Role " + roleUuid + " removed successfully from user " + userUuid)))
                .onErrorResume(error -> WebClientErrorHandler.handleWebClientErrorForString(error, "remove role from user", 
                        "user=" + userUuid + ", role=" + roleUuid + ", org=" + organizationUuid));
    }

    @PostMapping("/permissions/check")
    // @PreAuthorize("hasPermission('PERMISSION', 'CHECK')") // REMOVED - Circular dependency issue
    public Mono<ResponseEntity<PermissionCheckResponse>> checkPermission(
            @Valid @RequestBody PermissionCheckRequest request,
            HttpServletRequest httpRequest) {

        // Validate required headers
        HeaderValidator.ValidatedHeaders headers = HeaderValidator.getValidatedHeaders(httpRequest);
        String organizationUuid = headers.getOrganizationUuid();
        String userUuid = headers.getUserUuid();

        log.info("Checking permission for user: {} on resource: {} with action: {} in organization: {} (traceId: {})", 
                userUuid, request.getResource(), request.getAction(), organizationUuid, headers.getTraceId());

        // Use the service method that takes organization UUID as separate parameter
        return rolesServiceClient.hasPermission(userUuid, organizationUuid, request.getResource(), request.getAction())
                .map(hasPermission -> {
                    PermissionCheckResponse response = PermissionCheckResponse.builder()
                            .resource(request.getResource())
                            .action(request.getAction())
                            .has_permission(hasPermission)
                            .message(hasPermission ? "Permission granted" : "Permission denied")
                            .build();
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> WebClientErrorHandler.handleWebClientError(error, "check permission", 
                        "user=" + userUuid + ", resource=" + request.getResource() + ", action=" + request.getAction()));
    }

    @PostMapping("/user/{userUuid}/assign-admin")
    // @PreAuthorize("hasPermission('USER_ROLE', 'ASSIGN_ADMIN')")  // DISABLED FOR BOOTSTRAP
    public Mono<ResponseEntity<String>> assignAdminRoleToExistingUser(
            @PathVariable String userUuid,
            HttpServletRequest httpRequest) {

        String organizationUuid = httpRequest.getHeader("x-app-org-uuid");
        String assignedBy = httpRequest.getHeader("x-app-user-uuid");

        log.info("Assigning admin role to existing user: {} in organization: {} by user: {}", 
                userUuid, organizationUuid, assignedBy);

        return userRolesIntegrationService.checkUserExistsAndAssignAdminRole(userUuid, organizationUuid, assignedBy)
                .then(Mono.just(ResponseEntity.ok("Admin role assigned successfully to user: " + userUuid)))
                .onErrorResume(error -> WebClientErrorHandler.handleBusinessLogicError("assign admin role", 
                        "user=" + userUuid + ", org=" + organizationUuid, error.getMessage()));
    }

    @PostMapping("/user/{userUuid}/assign-role/{roleUuid}")
    // @PreAuthorize("hasPermission('USER_ROLE', 'ASSIGN')")  // DISABLED FOR BOOTSTRAP
    public Mono<ResponseEntity<String>> assignRoleByUuidToExistingUser(
            @PathVariable String userUuid,
            @PathVariable String roleUuid,
            HttpServletRequest httpRequest) {

        String organizationUuid = httpRequest.getHeader("x-app-org-uuid");
        String assignedBy = httpRequest.getHeader("x-app-user-uuid");

        log.info("Assigning role {} to existing user: {} in organization: {} by user: {}", 
                roleUuid, userUuid, organizationUuid, assignedBy);

        return userRolesIntegrationService.checkUserExistsAndAssignRoleByUuid(userUuid, roleUuid, organizationUuid, assignedBy)
                .then(Mono.just(ResponseEntity.ok("Role " + roleUuid + " assigned successfully to user: " + userUuid)))
                .onErrorResume(error -> WebClientErrorHandler.handleBusinessLogicError("assign role by UUID", 
                        "user=" + userUuid + ", role=" + roleUuid + ", org=" + organizationUuid, error.getMessage()));
    }

    // ========== BOOTSTRAP ENDPOINTS (NO AUTHORIZATION REQUIRED) ==========

    @PostMapping("/bootstrap/create")
    public Mono<ResponseEntity<RoleResponse>> bootstrapCreateRole(
            @Valid @RequestBody CreateRoleRequest request,
            HttpServletRequest httpRequest) {

        String organizationUuid = httpRequest.getHeader("x-app-org-uuid");
        String userUuid = httpRequest.getHeader("x-app-user-uuid");

        log.info("BOOTSTRAP: Creating role: {} for organization: {} by user: {}", 
                request.getRoleName(), organizationUuid, userUuid);

        return rolesServiceClient.createRole(request, organizationUuid)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @PostMapping("/bootstrap/filter")
    public Mono<ResponseEntity<ListRolesResponse>> bootstrapFilterRoles(
            @Valid @RequestBody ListRolesRequest request) {
        log.info("BOOTSTRAP: Filtering roles with criteria: {}", request);

        // For bootstrap, we don't need organization context for system managed roles
        return rolesServiceClient.filterRoles(request, "system")
                .map(response -> {
                    log.info("BOOTSTRAP: Found {} roles", response.getTotalElements());
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> WebClientErrorHandler.handleWebClientError(error, "bootstrap filter roles", 
                        "page=" + request.getPage() + ", size=" + request.getSize()));
    }

    @PostMapping("/bootstrap/organization/{organizationUuid}/setup")
    public Mono<ResponseEntity<String>> bootstrapOrganizationSetup(
            @PathVariable String organizationUuid,
            @RequestParam String adminUserUuid) {

        log.info("BOOTSTRAP: Setting up organization: {} with admin user: {}", organizationUuid, adminUserUuid);

        return userRolesIntegrationService.createDefaultAdminRole(organizationUuid, adminUserUuid)
                .flatMap(adminRole -> userRolesIntegrationService.createDefaultUserRole(organizationUuid, adminUserUuid)
                        .map(userRole -> adminRole))
                .flatMap(adminRole -> userRolesIntegrationService.assignAdminRoleToUser(adminUserUuid, organizationUuid, "system")
                        .thenReturn(adminRole))
                .map(adminRole -> ResponseEntity.ok("Organization setup complete. Admin role created and assigned to user: " + adminUserUuid))
                .onErrorResume(error -> WebClientErrorHandler.handleBusinessLogicError("bootstrap organization setup", 
                        "org=" + organizationUuid + ", admin=" + adminUserUuid, error.getMessage()));
    }

    @PostMapping("/bootstrap/user/{userUuid}/assign-admin")
    public Mono<ResponseEntity<String>> bootstrapAssignAdminRole(
            @PathVariable String userUuid,
            @RequestParam String organizationUuid,
            @RequestParam(defaultValue = "system") String assignedBy) {

        log.info("BOOTSTRAP: Assigning admin role to user: {} in organization: {}", userUuid, organizationUuid);

        return userRolesIntegrationService.assignAdminRoleToUser(userUuid, organizationUuid, assignedBy)
                .then(Mono.just(ResponseEntity.ok("Admin role assigned successfully to user: " + userUuid)))
                .onErrorResume(error -> WebClientErrorHandler.handleBusinessLogicError("bootstrap assign admin role", 
                        "user=" + userUuid + ", org=" + organizationUuid, error.getMessage()));
    }
}

package com.userapi.controller;

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
        
        String organizationUuid = httpRequest.getHeader("x-app-org-uuid");
        String userUuid = httpRequest.getHeader("x-app-user-uuid");
        
        log.info("Creating role: {} for organization: {} by user: {}", 
                request.getRoleName(), organizationUuid, userUuid);
        
        return rolesServiceClient.createRole(request, organizationUuid)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @GetMapping("/{roleUuid}")
    public Mono<ResponseEntity<RoleResponse>> getRole(
            @PathVariable String roleUuid,
            HttpServletRequest httpRequest) {
        
        String organizationUuid = httpRequest.getHeader("x-app-org-uuid");
        
        log.info("Getting role: {} for organization: {}", roleUuid, organizationUuid);
        
        return rolesServiceClient.getRoleByUuid(roleUuid, organizationUuid)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    @GetMapping("/organization/{organizationUuid}")
    public Mono<ResponseEntity<List<RoleResponse>>> getOrganizationRoles(
            @PathVariable String organizationUuid) {
        
        log.info("Getting roles for organization: {}", organizationUuid);
        
        return rolesServiceClient.getOrganizationRoles(organizationUuid)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @GetMapping("/system-managed")
    @PreAuthorize("hasPermission('SYSTEM_ROLE', 'READ')")
    public Mono<ResponseEntity<List<RoleResponse>>> getSystemManagedRoles() {
        log.info("Getting system managed roles");
        
        return rolesServiceClient.getSystemManagedRoles()
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @PutMapping("/{roleUuid}")
    @PreAuthorize("hasPermission('ROLE', 'UPDATE')")
    public Mono<ResponseEntity<RoleResponse>> updateRole(
            @PathVariable String roleUuid,
            @Valid @RequestBody CreateRoleRequest request,
            HttpServletRequest httpRequest) {
        
        String organizationUuid = httpRequest.getHeader("x-app-org-uuid");
        String userUuid = httpRequest.getHeader("x-app-user-uuid");
        
        log.info("Updating role: {} for organization: {} by user: {}", 
                roleUuid, organizationUuid, userUuid);
        
        return rolesServiceClient.updateRole(roleUuid, request, organizationUuid)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{roleUuid}")
    @PreAuthorize("hasPermission('ROLE', 'DELETE')")
    public Mono<ResponseEntity<String>> deleteRole(
            @PathVariable String roleUuid,
            HttpServletRequest httpRequest) {
        
        String organizationUuid = httpRequest.getHeader("x-app-org-uuid");
        String userUuid = httpRequest.getHeader("x-app-user-uuid");
        
        log.info("Deleting role: {} for organization: {} by user: {}", 
                roleUuid, organizationUuid, userUuid);
        
        return rolesServiceClient.deleteRole(roleUuid, organizationUuid)
                .then(Mono.just(ResponseEntity.ok("Role " + roleUuid + " deleted successfully")))
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to delete role: " + roleUuid));
    }

    @PostMapping("/user/{userUuid}/assign")
    // @PreAuthorize("hasPermission('USER_ROLE', 'ASSIGN')")  // DISABLED FOR FINAL BOOTSTRAP
    public Mono<ResponseEntity<String>> assignRoleToUser(
            @PathVariable String userUuid,
            @Valid @RequestBody AssignRoleRequest request,
            HttpServletRequest httpRequest) {
        
        String organizationUuid = httpRequest.getHeader("x-app-org-uuid");
        String assignedBy = httpRequest.getHeader("x-app-user-uuid");
        
        log.info("Assigning role: {} to user: {} in organization: {} by user: {}", 
                request.getRole_uuid(), userUuid, organizationUuid, assignedBy);
        
        return rolesServiceClient.assignRoleToUser(userUuid, request.getRole_uuid(), organizationUuid)
                .then(Mono.just(ResponseEntity.ok("Role " + request.getRole_uuid() + " assigned successfully to user: " + userUuid)))
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to assign role"));
    }

    @GetMapping("/user/{userUuid}")
    public Mono<ResponseEntity<List<RoleResponse>>> getUserRoles(
            @PathVariable String userUuid,
            HttpServletRequest httpRequest) {
        
        String organizationUuid = httpRequest.getHeader("x-app-org-uuid");
        
        log.info("Getting roles for user: {} in organization: {}", userUuid, organizationUuid);
        
        return rolesServiceClient.getUserRoles(userUuid, organizationUuid)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @DeleteMapping("/user/{userUuid}/roles/{roleUuid}")
    public Mono<ResponseEntity<String>> removeRoleFromUser(
            @PathVariable String userUuid,
            @PathVariable String roleUuid,
            HttpServletRequest httpRequest) {
        
        String organizationUuid = httpRequest.getHeader("x-app-org-uuid");
        String removedBy = httpRequest.getHeader("x-app-user-uuid");
        
        log.info("Removing role: {} from user: {} in organization: {} by user: {}", 
                roleUuid, userUuid, organizationUuid, removedBy);
        
        return rolesServiceClient.removeRoleFromUser(userUuid, roleUuid, organizationUuid)
                .then(Mono.just(ResponseEntity.ok("Role " + roleUuid + " removed successfully from user " + userUuid)))
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to remove role " + roleUuid + " from user " + userUuid));
    }

    @PostMapping("/permissions/check")
    // @PreAuthorize("hasPermission('PERMISSION', 'CHECK')") // REMOVED - Circular dependency issue
    public Mono<ResponseEntity<PermissionCheckResponse>> checkPermission(
            @Valid @RequestBody PermissionCheckRequest request) {

        log.info("Checking permission for user: {} on resource: {} with action: {}", 
                request.getUser_uuid(), request.getResource(), request.getAction());

        return rolesServiceClient.checkPermission(request)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
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

        String organizationUuid = httpRequest.getHeader("x-app-org-uuid");
        String userUuid = httpRequest.getHeader("x-app-user-uuid");

        log.info("BOOTSTRAP: Creating role: {} for organization: {} by user: {}", 
                request.getRoleName(), organizationUuid, userUuid);

        return rolesServiceClient.createRole(request, organizationUuid)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @GetMapping("/bootstrap/system-managed")
    public Mono<ResponseEntity<List<RoleResponse>>> bootstrapGetSystemManagedRoles() {
        log.info("BOOTSTRAP: Getting system managed roles");

        return rolesServiceClient.getSystemManagedRoles()
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
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
                .onErrorResume(error -> {
                    log.error("BOOTSTRAP: Failed to setup organization: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Failed to setup organization: " + error.getMessage()));
                });
    }

    @PostMapping("/bootstrap/user/{userUuid}/assign-admin")
    public Mono<ResponseEntity<String>> bootstrapAssignAdminRole(
            @PathVariable String userUuid,
            @RequestParam String organizationUuid,
            @RequestParam(defaultValue = "system") String assignedBy) {

        log.info("BOOTSTRAP: Assigning admin role to user: {} in organization: {}", userUuid, organizationUuid);

        return userRolesIntegrationService.assignAdminRoleToUser(userUuid, organizationUuid, assignedBy)
                .then(Mono.just(ResponseEntity.ok("Admin role assigned successfully to user: " + userUuid)))
                .onErrorResume(error -> {
                    log.error("BOOTSTRAP: Failed to assign admin role: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Failed to assign admin role: " + error.getMessage()));
                });
    }
}

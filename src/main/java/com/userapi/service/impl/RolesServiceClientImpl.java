package com.userapi.service.impl;

import com.userapi.models.external.roles.*;
import com.userapi.service.RolesServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class RolesServiceClientImpl implements RolesServiceClient {

    private final WebClient webClient;
    private final String baseUrl;

    public RolesServiceClientImpl(
            @Qualifier("rolesServiceWebClient") WebClient webClient,
            @Value("${roles.service.url:http://localhost:8081}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.webClient = webClient;
    }

    @Override
    public Mono<RoleResponse> createRole(CreateRoleRequest request, String organizationUuid) {
        log.info("Creating role: {} for organization: {}", request.getRoleName(), organizationUuid);
        log.info("Request payload: {}", request); // Add detailed logging

        return webClient.post()
                .uri("/role")
                .header("x-app-user-uuid", "system-user") // GitHub service expects this header
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RoleResponse.class)  // Changed to return RoleResponse
                .map(response -> {
                    // Map the name from request to name in response
                    if (response.getName() == null && request.getRoleName() != null) {
                        response.setName(request.getRoleName());
                    }
                    // Set organization_uuid if null
                    if (response.getOrganization_uuid() == null) {
                        response.setOrganization_uuid(organizationUuid);
                    }
                    // Set created_by if null
                    if (response.getCreated_by() == null) {
                        response.setCreated_by("system-user");
                    }
                    // Set is_active if null
                    if (response.getIs_active() == null) {
                        response.setIs_active(true);
                    }
                    return response;
                })
                .doOnSuccess(response -> log.info("Role created successfully: {} with name: {}", response.getRole_uuid(), response.getName()))
                .doOnError(error -> {
                    log.error("Failed to create role: {}", error.getMessage());
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException wcre = (WebClientResponseException) error;
                        log.error("Response status: {}, Response body: {}", wcre.getStatusCode(), wcre.getResponseBodyAsString());
                    }
                });
    }

    @Override
    public Mono<RoleResponse> getRoleByUuid(String roleUuid, String organizationUuid) {
        log.info("Getting role: {} for organization: {}", roleUuid, organizationUuid);
        
        return webClient.get()
                .uri("/role/{roleUuid}", roleUuid)
                .header("x-app-org-uuid", organizationUuid)
                .header("x-app-user-uuid", "system-user") // Add missing user UUID header
                .retrieve()
                .bodyToMono(RoleResponse.class)
                .doOnError(error -> log.error("Failed to get role: {}", error.getMessage()));
    }

    @Override
    public Mono<List<RoleResponse>> getOrganizationRoles(String organizationUuid) {
        log.info("Getting roles for organization: {}", organizationUuid);
        
        return webClient.get()
                .uri("/role/organization/{orgUuid}", organizationUuid)
                .header("x-app-user-uuid", "system-user") // Add missing user UUID header
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<RoleResponse>>() {})
                .doOnSuccess(roles -> log.info("Retrieved {} roles for organization: {}", roles.size(), organizationUuid))
                .doOnError(error -> log.error("Failed to get organization roles: {}", error.getMessage()));
    }

    @Override
    public Mono<List<RoleResponse>> getSystemManagedRoles() {
        log.info("Getting system managed roles");
        
        return webClient.get()
                .uri("/role/system-managed")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<RoleResponse>>() {})
                .doOnSuccess(roles -> log.info("Retrieved {} system managed roles", roles.size()))
                .doOnError(error -> log.error("Failed to get system managed roles: {}", error.getMessage()));
    }

    @Override
    public Mono<RoleResponse> updateRole(String roleUuid, CreateRoleRequest request, String organizationUuid) {
        log.info("Updating role: {} for organization: {}", roleUuid, organizationUuid);
        
        return webClient.put()
                .uri("/role/{roleUuid}", roleUuid)
                .header("x-app-org-uuid", organizationUuid)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RoleResponse.class)
                .doOnSuccess(response -> log.info("Role updated successfully: {}", response.getRole_uuid()))
                .doOnError(error -> log.error("Failed to update role: {}", error.getMessage()));
    }

    @Override
    public Mono<Void> deleteRole(String roleUuid, String organizationUuid) {
        log.info("Deleting role: {} for organization: {}", roleUuid, organizationUuid);
        
        return webClient.delete()
                .uri("/role/{roleUuid}", roleUuid)
                .header("x-app-org-uuid", organizationUuid)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(response -> log.info("Role deleted successfully: {}", roleUuid))
                .doOnError(error -> log.error("Failed to delete role: {}", error.getMessage()));
    }

    @Override
    public Mono<Void> assignRoleToUser(String userUuid, String roleUuid, String organizationUuid) {
        log.info("Assigning role: {} to user: {} in organization: {}", roleUuid, userUuid, organizationUuid);

        // Updated to match GitHub service structure
        AssignRoleRequest request = AssignRoleRequest.builder()
                .role_uuid(roleUuid)
                .organization_uuid(organizationUuid)
                .build();

        return webClient.post()
                .uri("/user/{userUuid}/roles", userUuid)
                .header("x-app-user-uuid", "system-user") // GitHub service expects this header
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(response -> log.info("Role assigned successfully to user: {}", userUuid))
                .doOnError(error -> {
                    log.error("Failed to assign role to user: {}", error.getMessage());
                    if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                        org.springframework.web.reactive.function.client.WebClientResponseException wcre = 
                            (org.springframework.web.reactive.function.client.WebClientResponseException) error;
                        log.error("Response status: {}, Response body: {}", wcre.getStatusCode(), wcre.getResponseBodyAsString());
                    }
                });
    }

    @Override
    public Mono<List<RoleResponse>> getUserRoles(String userUuid, String organizationUuid) {
        log.info("Getting roles for user: {} in organization: {}", userUuid, organizationUuid);

        return webClient.get()
                .uri("/user/{userUuid}/roles?organization_uuid={organizationUuid}", userUuid, organizationUuid)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<RoleResponse>>() {})
                .doOnSuccess(roles -> log.info("Retrieved {} roles for user: {}", roles.size(), userUuid))
                .doOnError(error -> log.error("Failed to get user roles: {}", error.getMessage()));
    }

    @Override
    public Mono<Void> removeRoleFromUser(String userUuid, String roleUuid, String organizationUuid) {
        log.info("Removing role: {} from user: {} in organization: {}", roleUuid, userUuid, organizationUuid);
        
        return webClient.delete()
                .uri("/user/{userUuid}/roles/{roleUuid}?organization_uuid={organizationUuid}", 
                     userUuid, roleUuid, organizationUuid)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(response -> log.info("Role removed successfully from user: {}", userUuid))
                .doOnError(error -> log.error("Failed to remove role from user: {}", error.getMessage()));
    }

    @Override
    public Mono<PermissionCheckResponse> checkPermission(PermissionCheckRequest request) {
        log.info("Checking permission for user: {} on resource: {} with action: {}", 
                request.getUser_uuid(), request.getResource(), request.getAction());
        
        return webClient.post()
                .uri("/permission/check")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PermissionCheckResponse.class)
                .doOnSuccess(response -> log.info("Permission check result: {}", response.getHas_permission()))
                .doOnError(error -> log.error("Failed to check permission: {}", error.getMessage()));
    }

    @Override
    public Mono<Boolean> hasPermission(String userUuid, String organizationUuid, String resource, String action) {
        PermissionCheckRequest request = PermissionCheckRequest.builder()
                .user_uuid(userUuid)
                .organization_uuid(organizationUuid)
                .resource(resource)
                .action(action)
                .build();
        
        return checkPermission(request)
                .map(PermissionCheckResponse::getHas_permission)
                .defaultIfEmpty(false);
    }
}

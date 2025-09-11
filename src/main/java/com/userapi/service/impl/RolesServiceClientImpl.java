package com.userapi.service.impl;

import com.userapi.enums.RoleStatus;
import com.userapi.models.external.roles.*;
import com.userapi.service.RolesServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RolesServiceClientImpl implements RolesServiceClient {

    private final WebClient webClient;

    public RolesServiceClientImpl(
            @Qualifier("rolesServiceWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<RoleResponse> createRole(CreateRoleRequest request, String organizationUuid) {
        log.info("Creating role: {} for organization: {}", request.getRoleName(), organizationUuid);
        log.info("Request payload: {}", request); // Add detailed logging

        return webClient.post()
                .uri("/role")
                .header("x-app-org-uuid", organizationUuid) // Organization context via header
                .header("x-app-user-uuid", "system-user") // GitHub service expects this header
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RoleResponse.class)  // Changed to return RoleResponse
                .map(response -> {
                    // Map the name from request to name in response
                    if (response.getRoleName() == null && request.getRoleName() != null) {
                        response.setRoleName(request.getRoleName());
                    }
                    // Set organization_uuid if null
                    if (response.getOrganization_uuid() == null) {
                        response.setOrganization_uuid(organizationUuid);
                    }
                    // Set created_by if null
                    if (response.getCreated_by() == null) {
                        response.setCreated_by("system-user");
                    }
                    // Set roleStatus if null
                    if (response.getRoleStatus() == null) {
                        response.setRoleStatus(RoleStatus.ACTIVE);
                    }
                    return response;
                })
                .doOnSuccess(response -> log.info("Role created successfully: {} with name: {}", response.getRole_uuid(), response.getRoleName()))
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
        
        // Use the search endpoint with organization context in headers (no filter criteria)
        // The roles service will return all roles for the organization based on the header
        ListRolesRequest request = ListRolesRequest.builder()
                .filterCriteria(ListRolesFilterCriteria.builder()
                        .attributes(List.of())
                        .build())
                .build();
        
        return webClient.post()
                .uri("/role/search")
                .header("x-app-org-uuid", organizationUuid) // Organization context via header
                .header("x-app-user-uuid", "system-user") // GitHub service expects this header
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ListRolesResponse.class)
                .map(response -> response.getRoles())
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
    public Mono<RoleResponse> updateRole(String roleUuid, UpdateRoleRequest request, String organizationUuid) {
        log.info("Updating role: {} for organization: {}", roleUuid, organizationUuid);
        log.info("Update request payload: {}", request);
        
        // First, get the current role to preserve immutable fields
        return getRoleByUuid(roleUuid, organizationUuid)
                .flatMap(currentRole -> {
                    // Create a CreateRoleRequest with current role data and updated fields
                    CreateRoleRequest updateRequest = CreateRoleRequest.builder()
                            .roleName(currentRole.getRoleName()) // Preserve role name
                            .description(request.getDescription() != null ? request.getDescription() : currentRole.getDescription())
                            .roleManagementType(currentRole.getRole_management_type()) // Preserve management type
                            .policy(request.getPolicy() != null ? request.getPolicy() : currentRole.getPolicy())
                            .build();
                    
                    log.info("Mapped update request: {}", updateRequest);
                    log.info("Sending PUT request to: /role/{} with organization: {}", roleUuid, organizationUuid);
                    
                    return webClient.put()
                            .uri("/role/{roleUuid}", roleUuid)
                            .header("x-app-org-uuid", organizationUuid)
                            .header("x-app-user-uuid", "system-user")
                            .header("Content-Type", "application/json")
                            .bodyValue(updateRequest)
                            .retrieve()
                            .bodyToMono(RoleResponse.class)
                            .map(response -> {
                                // Handle status field if it was provided in the update request
                                if (request.getStatus() != null) {
                                    response.setRoleStatus(request.getStatus());
                                    log.info("Updated status field to: {}", request.getStatus());
                                }
                                return response;
                            })
                            .doOnSuccess(response -> log.info("Role updated successfully: {}", response.getRole_uuid()))
                            .doOnError(error -> {
                                log.error("Failed to update role: {}", error.getMessage());
                                if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                                    org.springframework.web.reactive.function.client.WebClientResponseException wcre = 
                                        (org.springframework.web.reactive.function.client.WebClientResponseException) error;
                                    log.error("External service error - Status: {}, Body: {}", 
                                            wcre.getStatusCode(), wcre.getResponseBodyAsString());
                                }
                            });
                });
    }

    @Override
    public Mono<Void> deleteRole(String roleUuid, String organizationUuid) {
        log.info("Deleting role: {} for organization: {}", roleUuid, organizationUuid);
        
        return webClient.delete()
                .uri("/role/{roleUuid}", roleUuid)
                .header("x-app-org-uuid", organizationUuid)
                .header("x-app-user-uuid", "system-user") // Add missing user UUID header
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(response -> log.info("Role deleted successfully: {}", roleUuid))
                .doOnError(error -> log.error("Failed to delete role: {}", error.getMessage()));
    }

    @Override
    public Mono<Void> assignRoleToUser(String userUuid, String roleUuid, String organizationUuid) {
        log.info("Assigning role: {} to user: {} in organization: {}", roleUuid, userUuid, organizationUuid);

        // Create request with only role_uuid - organization context comes from headers
        AssignRoleRequest request = AssignRoleRequest.builder()
                .role_uuid(roleUuid)
                .build();

        return webClient.post()
                .uri("/user/{userUuid}/roles", userUuid)
                .header("x-app-org-uuid", organizationUuid) // Organization context via header
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
                .uri("/user/{userUuid}/roles", userUuid)
                .header("x-app-user-uuid", "system-user") // Add missing user UUID header
                .header("x-app-org-uuid", organizationUuid) // Add organization UUID header
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<RoleResponse>>() {})
                .flatMapMany(Flux::fromIterable) // Convert to Flux for individual processing
                .flatMap(role -> {
                    // If role has incomplete data, fetch the full role details
                    if (role.getRoleName() == null || role.getDescription() == null) {
                        log.info("Role {} has incomplete data, fetching full details", role.getRole_uuid());
                        return getRoleByUuid(role.getRole_uuid(), organizationUuid);
                    } else {
                        return Mono.just(role);
                    }
                })
                .collectList()
                .doOnSuccess(roles -> log.info("Retrieved {} complete roles for user: {}", roles.size(), userUuid))
                .doOnError(error -> log.error("Failed to get user roles: {}", error.getMessage()));
    }

    @Override
    public Mono<Void> removeRoleFromUser(String userUuid, String roleUuid, String organizationUuid) {
        log.info("Removing role: {} from user: {} in organization: {}", roleUuid, userUuid, organizationUuid);
        
        return webClient.delete()
                .uri("/user/{userUuid}/roles/{roleUuid}", userUuid, roleUuid)
                .header("x-app-user-uuid", "system-user") // Add missing user UUID header
                .header("x-app-org-uuid", organizationUuid) // Add organization UUID header
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(response -> log.info("Role removed successfully from user: {}", userUuid))
                .doOnError(error -> log.error("Failed to remove role from user: {}", error.getMessage()));
    }

    @Override
    public Mono<PermissionCheckResponse> checkPermission(PermissionCheckRequest request) {
        log.info("Checking permission for resource: {} with action: {}", 
                request.getResource(), request.getAction());
        
        return webClient.post()
                .uri("/permission/check")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PermissionCheckResponse.class)
                .doOnSuccess(response -> log.info("Permission check result: {}", response.getResult()))
                .doOnError(error -> log.error("Failed to check permission: {}", error.getMessage()));
    }

    @Override
    public Mono<Boolean> hasPermission(String userUuid, String organizationUuid, String resource, String action) {
        PermissionCheckRequest request = PermissionCheckRequest.builder()
                .resource(resource)
                .action(action)
                .build();
        
        return webClient.post()
                .uri("/permission/check")
                .header("x-app-user-uuid", userUuid) // User context via header
                .header("x-app-org-uuid", organizationUuid) // Organization context via header
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PermissionCheckResponse.class)
                .map(response -> response.getResult() == com.userapi.enums.PermissionResult.ACCEPTED)
                .defaultIfEmpty(false)
                .doOnSuccess(result -> log.info("Permission check result: {} for user: {} on resource: {} with action: {}", 
                        result, userUuid, resource, action))
                .doOnError(error -> log.error("Failed to check permission for user: {} on resource: {} with action: {} - {}", 
                        userUuid, resource, action, error.getMessage()));
    }

    @Override
    public Mono<ListRolesResponse> searchRoles(ListRolesRequest request, String organizationUuid) {
        log.info("Searching roles for organization: {} with filters: {}", organizationUuid, request.getFilterCriteria());
        
        // Determine which roles to fetch based on filter criteria
        Mono<List<RoleResponse>> rolesMono;
        
        if (request.getFilterCriteria() != null && request.getFilterCriteria().getAttributes() != null) {
            // Check if filtering for system-managed roles
            boolean isSystemManaged = request.getFilterCriteria().getAttributes().stream()
                    .anyMatch(attr -> ("roleManagementType".equals(attr.getName()) || "role_management_type".equals(attr.getName())) && 
                            attr.getValues().contains("SYSTEM_MANAGED"));
            
            // Check if filtering for customer-managed roles
            boolean isCustomerManaged = request.getFilterCriteria().getAttributes().stream()
                    .anyMatch(attr -> ("roleManagementType".equals(attr.getName()) || "role_management_type".equals(attr.getName())) && 
                            attr.getValues().contains("CUSTOMER_MANAGED"));
            
            // Check if filtering for both system-managed and customer-managed (organization-specific roles)
            boolean isBothTypes = isSystemManaged && isCustomerManaged;
            
            if (isBothTypes) {
                // When searching for both types, get organization-specific roles (both SYSTEM_MANAGED and CUSTOMER_MANAGED for the organization)
                log.info("Filtering for both system-managed and customer-managed roles for organization: {}", organizationUuid);
                rolesMono = getOrganizationRoles(organizationUuid);
            } else if (isSystemManaged && !isCustomerManaged) {
                // Only system-managed roles - check if this is for global roles or organization-specific
                if ("system".equals(organizationUuid)) {
                    log.info("Filtering for global system-managed roles");
                    rolesMono = getSystemManagedRoles();
                } else {
                    log.info("Filtering for organization-specific system-managed roles for organization: {}", organizationUuid);
                    rolesMono = getOrganizationRoles(organizationUuid);
                }
            } else if (isCustomerManaged) {
                log.info("Filtering for customer-managed roles for organization: {}", organizationUuid);
                rolesMono = getOrganizationRoles(organizationUuid);
            } else {
                // No specific role management type filter - get organization roles by default
                log.info("No role management type specified, defaulting to organization roles for: {}", organizationUuid);
                rolesMono = getOrganizationRoles(organizationUuid);
            }
        } else {
            // Default: get organization roles
            log.info("No filter criteria provided, defaulting to organization roles for: {}", organizationUuid);
            rolesMono = getOrganizationRoles(organizationUuid);
        }
        
        return rolesMono
                .<ListRolesResponse>map(roles -> {
                    // Apply filtering logic here if needed
                    List<RoleResponse> filteredRoles = roles;
                    
                    if (request.getFilterCriteria() != null && request.getFilterCriteria().getAttributes() != null) {
                        filteredRoles = applyFilters(roles, request.getFilterCriteria());
                        
                        // Additional filtering by organization UUID if specified
                        boolean hasOrgFilter = request.getFilterCriteria().getAttributes().stream()
                                .anyMatch(attr -> ("organizationUuid".equals(attr.getName()) || "organization_uuid".equals(attr.getName())));
                        
                        if (hasOrgFilter) {
                            filteredRoles = filteredRoles.stream()
                                    .filter(role -> {
                                        // For system-managed roles, organization_uuid is typically null
                                        // For customer-managed roles, it should match the specified organization
                                        String roleOrgUuid = role.getOrganization_uuid();
                                        return request.getFilterCriteria().getAttributes().stream()
                                                .filter(attr -> ("organizationUuid".equals(attr.getName()) || "organization_uuid".equals(attr.getName())))
                                                .flatMap(attr -> attr.getValues().stream())
                                                .anyMatch(filterValue -> 
                                                    filterValue.equals(roleOrgUuid) || 
                                                    (roleOrgUuid == null && "SYSTEM_MANAGED".equals(role.getRole_management_type()))
                                                );
                                    })
                                    .collect(Collectors.toList());
                        }
                    }
                    
                    // Apply pagination (1-based page numbers)
                    int page = request.getPage() != null ? request.getPage() : 1;
                    int size = request.getSize() != null ? request.getSize() : 10;
                    int startIndex = (page - 1) * size; // Convert to 0-based index
                    int endIndex = Math.min(startIndex + size, filteredRoles.size());
                    
                    List<RoleResponse> paginatedRoles = filteredRoles.subList(startIndex, endIndex);
                    
                    // Calculate pagination info
                    long totalElements = filteredRoles.size();
                    int totalPages = (int) Math.ceil((double) totalElements / size);
                    
                    return ListRolesResponse.builder()
                            .roles(paginatedRoles)
                            .totalElements(totalElements)
                            .totalPages(totalPages)
                            .currentPage(page)
                            .pageSize(size)
                            .build();
                })
                .doOnSuccess(response -> log.info("Found {} roles for organization: {}", response.getTotalElements(), organizationUuid))
                .doOnError(error -> log.error("Failed to search roles: {}", error.getMessage()));
    }
    
    private List<RoleResponse> applyFilters(List<RoleResponse> roles, ListRolesFilterCriteria filterCriteria) {
        return roles.stream()
                .filter(role -> {
                    for (ListRolesFilterCriteriaAttribute attribute : filterCriteria.getAttributes()) {
                        if (!matchesFilter(role, attribute)) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
    
    private boolean matchesFilter(RoleResponse role, ListRolesFilterCriteriaAttribute attribute) {
        String attributeName = attribute.getName();
        List<String> values = attribute.getValues();
        
        switch (attributeName) {
            case "roleName":
            case "role_name":
                return values.contains(role.getRoleName());
            case "roleManagementType":
            case "role_management_type":
                return values.contains(role.getRole_management_type());
            case "description":
                return values.stream().anyMatch(value -> 
                    role.getDescription() != null && role.getDescription().toLowerCase().contains(value.toLowerCase()));
            case "organizationUuid":
            case "organization_uuid":
                return values.contains(role.getOrganization_uuid());
            default:
                log.warn("Unknown filter attribute: {}", attributeName);
                return true;
        }
    }
}

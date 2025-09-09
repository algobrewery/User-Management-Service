package com.userapi.exception;

import com.userapi.models.external.roles.RoleResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive exception handler for all Roles Service related operations
 * This class handles all WebClient exceptions and provides proper error responses
 */
@ControllerAdvice
@Slf4j
public class RolesServiceExceptionHandler {

    /**
     * Handle WebClientResponseException for role creation operations
     */
    public static Mono<ResponseEntity<RoleResponse>> handleRoleCreationError(
            String roleName, String organizationUuid, Throwable error) {
        
        log.error("Failed to create role: {} for organization: {} - {}", 
                roleName, organizationUuid, error.getMessage());
        
        if (error instanceof WebClientResponseException) {
            WebClientResponseException wcre = (WebClientResponseException) error;
            
            if (wcre.getStatusCode().is4xxClientError()) {
                return Mono.just(ResponseEntity.status(wcre.getStatusCode())
                        .body(new RoleResponse()));
            } else {
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new RoleResponse()));
            }
        }
        
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new RoleResponse()));
    }

    /**
     * Handle WebClientResponseException for role retrieval operations
     */
    public static Mono<ResponseEntity<RoleResponse>> handleRoleRetrievalError(
            String roleUuid, String organizationUuid, Throwable error) {
        
        log.error("Failed to get role: {} for organization: {} - {}", 
                roleUuid, organizationUuid, error.getMessage());
        
        if (error instanceof WebClientResponseException) {
            WebClientResponseException wcre = (WebClientResponseException) error;
            
            if (wcre.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Mono.just(ResponseEntity.notFound().build());
            } else if (wcre.getStatusCode().is4xxClientError()) {
                return Mono.just(ResponseEntity.status(wcre.getStatusCode()).build());
            } else {
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            }
        }
        
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    /**
     * Handle WebClientResponseException for role list operations
     */
    public static Mono<ResponseEntity<List<RoleResponse>>> handleRoleListError(
            String context, Throwable error) {
        
        log.error("Failed to get roles for {} - {}", context, error.getMessage());
        
        if (error instanceof WebClientResponseException) {
            WebClientResponseException wcre = (WebClientResponseException) error;
            
            if (wcre.getStatusCode().is4xxClientError()) {
                return Mono.just(ResponseEntity.status(wcre.getStatusCode()).build());
            } else {
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            }
        }
        
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    /**
     * Handle WebClientResponseException for role update operations
     */
    public static Mono<ResponseEntity<RoleResponse>> handleRoleUpdateError(
            String roleUuid, String organizationUuid, Throwable error) {
        
        log.error("Failed to update role: {} for organization: {} - {}", 
                roleUuid, organizationUuid, error.getMessage());
        
        if (error instanceof WebClientResponseException) {
            WebClientResponseException wcre = (WebClientResponseException) error;
            
            if (wcre.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Mono.just(ResponseEntity.notFound().build());
            } else if (wcre.getStatusCode().is4xxClientError()) {
                return Mono.just(ResponseEntity.status(wcre.getStatusCode())
                        .body(new RoleResponse()));
            } else {
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new RoleResponse()));
            }
        }
        
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new RoleResponse()));
    }

    /**
     * Handle WebClientResponseException for role deletion operations
     */
    public static Mono<ResponseEntity<String>> handleRoleDeletionError(
            String roleUuid, String organizationUuid, Throwable error) {
        
        log.error("Failed to delete role: {} for organization: {} - {}", 
                roleUuid, organizationUuid, error.getMessage());
        
        if (error instanceof WebClientResponseException) {
            WebClientResponseException wcre = (WebClientResponseException) error;
            
            if (wcre.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Role not found: " + roleUuid));
            } else if (wcre.getStatusCode().is4xxClientError()) {
                return Mono.just(ResponseEntity.status(wcre.getStatusCode())
                        .body("Client error: " + wcre.getResponseBodyAsString()));
            } else {
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to delete role: " + roleUuid));
            }
        }
        
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to delete role: " + roleUuid));
    }

    /**
     * Handle WebClientResponseException for user role assignment operations
     */
    public static Mono<ResponseEntity<String>> handleUserRoleAssignmentError(
            String userUuid, String roleUuid, String organizationUuid, Throwable error) {
        
        log.error("Failed to assign role: {} to user: {} in organization: {} - {}", 
                roleUuid, userUuid, organizationUuid, error.getMessage());
        
        if (error instanceof WebClientResponseException) {
            WebClientResponseException wcre = (WebClientResponseException) error;
            
            if (wcre.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User or role not found"));
            } else if (wcre.getStatusCode() == HttpStatus.CONFLICT) {
                return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Role already assigned to user"));
            } else if (wcre.getStatusCode().is4xxClientError()) {
                return Mono.just(ResponseEntity.status(wcre.getStatusCode())
                        .body("Client error: " + wcre.getResponseBodyAsString()));
            } else {
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to assign role"));
            }
        }
        
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to assign role"));
    }

    /**
     * Handle WebClientResponseException for user role removal operations
     */
    public static Mono<ResponseEntity<String>> handleUserRoleRemovalError(
            String userUuid, String roleUuid, String organizationUuid, Throwable error) {
        
        log.error("Failed to remove role: {} from user: {} in organization: {} - {}", 
                roleUuid, userUuid, organizationUuid, error.getMessage());
        
        if (error instanceof WebClientResponseException) {
            WebClientResponseException wcre = (WebClientResponseException) error;
            
            if (wcre.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User or role assignment not found"));
            } else if (wcre.getStatusCode().is4xxClientError()) {
                return Mono.just(ResponseEntity.status(wcre.getStatusCode())
                        .body("Client error: " + wcre.getResponseBodyAsString()));
            } else {
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to remove role " + roleUuid + " from user " + userUuid));
            }
        }
        
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to remove role " + roleUuid + " from user " + userUuid));
    }

    /**
     * Handle WebClientResponseException for permission check operations
     */
    public static Mono<ResponseEntity<com.userapi.models.external.roles.PermissionCheckResponse>> handlePermissionCheckError(
            String userUuid, String resource, String action, Throwable error) {
        
        log.error("Failed to check permission for user: {} on resource: {} with action: {} - {}", 
                userUuid, resource, action, error.getMessage());
        
        if (error instanceof WebClientResponseException) {
            WebClientResponseException wcre = (WebClientResponseException) error;
            
            if (wcre.getStatusCode().is4xxClientError()) {
                return Mono.just(ResponseEntity.status(wcre.getStatusCode()).build());
            } else {
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            }
        }
        
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    /**
     * Handle WebClientResponseException for bootstrap operations
     */
    public static Mono<ResponseEntity<String>> handleBootstrapError(
            String operation, String context, Throwable error) {
        
        log.error("BOOTSTRAP: Failed {} for {} - {}", operation, context, error.getMessage());
        
        if (error instanceof WebClientResponseException) {
            WebClientResponseException wcre = (WebClientResponseException) error;
            
            if (wcre.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Resource not found during bootstrap: " + context));
            } else if (wcre.getStatusCode().is4xxClientError()) {
                return Mono.just(ResponseEntity.status(wcre.getStatusCode())
                        .body("Client error during bootstrap: " + wcre.getResponseBodyAsString()));
            } else {
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed during bootstrap: " + error.getMessage()));
            }
        }
        
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed during bootstrap: " + error.getMessage()));
    }

    /**
     * Create a standardized error response
     */
    public static Map<String, Object> createErrorResponse(String message, String errorCode, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("message", message);
        errorResponse.put("errorCode", errorCode);
        return errorResponse;
    }

    /**
     * Handle generic WebClientResponseException
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleWebClientResponseException(WebClientResponseException ex) {
        log.error("WebClient error: Status={}, Body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
        
        Map<String, Object> errorResponse = createErrorResponse(
                "External service error: " + ex.getResponseBodyAsString(),
                "EXTERNAL_SERVICE_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

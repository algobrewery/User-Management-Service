package com.userapi.exception;

import com.userapi.models.external.roles.RoleResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Centralized error handler for WebClient exceptions
 * Provides consistent error handling across all controllers
 */
@Slf4j
public class WebClientErrorHandler {

    /**
     * Handle WebClient errors for RoleResponse objects
     */
    @SuppressWarnings("unchecked")
    public static <T> Mono<ResponseEntity<T>> handleWebClientError(Throwable error, String operation, String context) {
        log.error("Failed to {} - {}: {}", operation, context, error.getMessage(), error);
        
        if (error instanceof WebClientResponseException) {
            WebClientResponseException webEx = (WebClientResponseException) error;
            HttpStatus status = HttpStatus.valueOf(webEx.getStatusCode().value());
            String errorMessage = getErrorMessageForStatus(status, operation, context, webEx.getResponseBodyAsString());
            
            return Mono.just(ResponseEntity.status(status).body((T) createErrorResponse(errorMessage)));
        } else {
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body((T) createErrorResponse("Internal server error during " + operation + ": " + error.getMessage())));
        }
    }

    /**
     * Handle WebClient errors for String responses (delete operations, etc.)
     */
    public static Mono<ResponseEntity<String>> handleWebClientErrorForString(Throwable error, String operation, String context) {
        log.error("Failed to {} - {}: {}", operation, context, error.getMessage(), error);
        
        if (error instanceof WebClientResponseException) {
            WebClientResponseException webEx = (WebClientResponseException) error;
            HttpStatus status = HttpStatus.valueOf(webEx.getStatusCode().value());
            String errorMessage = getErrorMessageForStatus(status, operation, context, webEx.getResponseBodyAsString());
            
            return Mono.just(ResponseEntity.status(status).body(errorMessage));
        } else {
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error during " + operation + ": " + error.getMessage()));
        }
    }

    /**
     * Create error response object
     */
    private static RoleResponse createErrorResponse(String errorMessage) {
        RoleResponse errorResponse = new RoleResponse();
        errorResponse.setName("ERROR");
        errorResponse.setDescription(errorMessage);
        return errorResponse;
    }

    /**
     * Get appropriate error message based on HTTP status
     */
    private static String getErrorMessageForStatus(HttpStatus status, String operation, String context, String responseBody) {
        switch (status) {
            case BAD_REQUEST:
                return "Invalid data for " + operation + ": " + responseBody;
            case UNAUTHORIZED:
                return "Unauthorized to " + operation;
            case FORBIDDEN:
                return "Insufficient permissions to " + operation;
            case NOT_FOUND:
                return "Resource not found for " + operation + ": " + context;
            case CONFLICT:
                return "Conflict during " + operation + ": " + responseBody;
            case UNPROCESSABLE_ENTITY:
                return "Validation failed for " + operation + ": " + responseBody;
            case TOO_MANY_REQUESTS:
                return "Rate limit exceeded for " + operation;
            case SERVICE_UNAVAILABLE:
                return "Service temporarily unavailable for " + operation;
            default:
                return "Failed to " + operation + ": " + responseBody;
        }
    }

    /**
     * Handle specific business logic errors
     */
    public static Mono<ResponseEntity<String>> handleBusinessLogicError(String operation, String context, String specificError) {
        log.error("Business logic error during {} - {}: {}", operation, context, specificError);
        
        if (specificError.contains("not found")) {
            return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(specificError));
        } else if (specificError.contains("already exists") || specificError.contains("duplicate")) {
            return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(specificError));
        } else if (specificError.contains("permission") || specificError.contains("unauthorized")) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(specificError));
        } else {
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(specificError));
        }
    }
}

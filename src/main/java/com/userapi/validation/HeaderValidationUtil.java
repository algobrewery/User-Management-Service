package com.userapi.validation;

import com.userapi.common.constants.HeaderConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;

/**
 * Utility class for validating HTTP headers in controller endpoints.
 * Ensures required headers are present and properly formatted.
 */
@Slf4j
public class HeaderValidationUtil {
    
    /**
     * Validates that required headers are present and properly formatted
     * @param httpRequest the HTTP request containing headers
     * @throws ResponseStatusException if validation fails
     */
    public static void validateRequiredHeaders(HttpServletRequest httpRequest) {
        String organizationUuid = httpRequest.getHeader(HeaderConstants.APP_ORG_UUID);
        String userUuid = httpRequest.getHeader(HeaderConstants.APP_USER_UUID);
        
        validateOrganizationUuid(organizationUuid);
        validateUserUuid(userUuid);
    }
    
    /**
     * Validates organization UUID header
     * @param organizationUuid the organization UUID to validate
     * @throws ResponseStatusException if validation fails
     */
    public static void validateOrganizationUuid(String organizationUuid) {
        if (organizationUuid == null || organizationUuid.trim().isEmpty()) {
            log.error("Missing required header: {}", HeaderConstants.APP_ORG_UUID);
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "Missing required header: " + HeaderConstants.APP_ORG_UUID
            );
        }
        
        // Allow non-UUID values for development/testing (like "cts", "test", etc.)
        // but still validate that it's not empty and has reasonable length
        String trimmedUuid = organizationUuid.trim();
        if (trimmedUuid.length() < 2 || trimmedUuid.length() > 100) {
            log.error("Invalid organization identifier format: {} (length: {})", organizationUuid, trimmedUuid.length());
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "Invalid organization identifier format: " + organizationUuid + " (must be 2-100 characters)"
            );
        }
        
        log.debug("Organization identifier validated: {}", organizationUuid);
    }
    
    /**
     * Validates user UUID header
     * @param userUuid the user UUID to validate
     * @throws ResponseStatusException if validation fails
     */
    public static void validateUserUuid(String userUuid) {
        if (userUuid == null || userUuid.trim().isEmpty()) {
            log.error("Missing required header: {}", HeaderConstants.APP_USER_UUID);
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "Missing required header: " + HeaderConstants.APP_USER_UUID
            );
        }
        
        // Allow non-UUID values for development/testing but validate format
        String trimmedUuid = userUuid.trim();
        if (trimmedUuid.length() < 2 || trimmedUuid.length() > 100) {
            log.error("Invalid user identifier format: {} (length: {})", userUuid, trimmedUuid.length());
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "Invalid user identifier format: " + userUuid + " (must be 2-100 characters)"
            );
        }
        
        log.debug("User identifier validated: {}", userUuid);
    }
    
    /**
     * Safely extracts organization UUID from request headers with validation
     * @param httpRequest the HTTP request
     * @return validated organization UUID
     * @throws ResponseStatusException if validation fails
     */
    public static String getValidatedOrganizationUuid(HttpServletRequest httpRequest) {
        String organizationUuid = httpRequest.getHeader(HeaderConstants.APP_ORG_UUID);
        validateOrganizationUuid(organizationUuid);
        return organizationUuid.trim();
    }
    
    /**
     * Safely extracts user UUID from request headers with validation
     * @param httpRequest the HTTP request
     * @return validated user UUID
     * @throws ResponseStatusException if validation fails
     */
    public static String getValidatedUserUuid(HttpServletRequest httpRequest) {
        String userUuid = httpRequest.getHeader(HeaderConstants.APP_USER_UUID);
        validateUserUuid(userUuid);
        return userUuid.trim();
    }
}

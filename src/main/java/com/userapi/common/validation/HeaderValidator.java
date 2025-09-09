package com.userapi.common.validation;

import com.userapi.common.constants.HeaderConstants;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * Utility class for validating HTTP headers in requests.
 * Provides consistent validation across all controllers.
 */
@Slf4j
public class HeaderValidator {

    /**
     * Validates that required headers are present and valid
     */
    public static void validateRequiredHeaders(HttpServletRequest request) {
        validateOrganizationUuid(request);
        validateUserUuid(request);
    }

    /**
     * Validates organization UUID header
     */
    public static String validateOrganizationUuid(HttpServletRequest request) {
        String organizationUuid = request.getHeader(HeaderConstants.APP_ORG_UUID);
        
        if (organizationUuid == null || organizationUuid.trim().isEmpty()) {
            log.error("Missing required header: {}", HeaderConstants.APP_ORG_UUID);
            throw new IllegalArgumentException("Missing required header: " + HeaderConstants.APP_ORG_UUID);
        }
        
        if (!isValidUuid(organizationUuid)) {
            log.error("Invalid organization UUID format: {}", organizationUuid);
            throw new IllegalArgumentException("Invalid organization UUID format: " + organizationUuid);
        }
        
        return organizationUuid;
    }

    /**
     * Validates user UUID header
     */
    public static String validateUserUuid(HttpServletRequest request) {
        String userUuid = request.getHeader(HeaderConstants.APP_USER_UUID);
        
        if (userUuid == null || userUuid.trim().isEmpty()) {
            log.error("Missing required header: {}", HeaderConstants.APP_USER_UUID);
            throw new IllegalArgumentException("Missing required header: " + HeaderConstants.APP_USER_UUID);
        }
        
        if (!isValidUuid(userUuid)) {
            log.error("Invalid user UUID format: {}", userUuid);
            throw new IllegalArgumentException("Invalid user UUID format: " + userUuid);
        }
        
        return userUuid;
    }

    /**
     * Validates trace ID header (optional but recommended)
     */
    public static String validateTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(HeaderConstants.APP_TRACE_ID);
        
        if (traceId != null && !traceId.trim().isEmpty() && !isValidUuid(traceId)) {
            log.warn("Invalid trace ID format: {}", traceId);
            // Don't throw exception for trace ID as it's optional
        }
        
        return traceId;
    }

    /**
     * Validates session UUID header (optional)
     */
    public static String validateSessionUuid(HttpServletRequest request) {
        String sessionUuid = request.getHeader(HeaderConstants.APP_CLIENT_USER_SESSION_UUID);
        
        if (sessionUuid != null && !sessionUuid.trim().isEmpty() && !isValidUuid(sessionUuid)) {
            log.warn("Invalid session UUID format: {}", sessionUuid);
            // Don't throw exception for session UUID as it's optional
        }
        
        return sessionUuid;
    }

    /**
     * Validates region ID header (optional)
     */
    public static String validateRegionId(HttpServletRequest request) {
        String regionId = request.getHeader(HeaderConstants.APP_REGION_ID);
        
        if (regionId != null && !regionId.trim().isEmpty()) {
            // Add any specific region ID validation logic here
            log.debug("Region ID provided: {}", regionId);
        }
        
        return regionId;
    }

    /**
     * Validates if a string is a valid UUID format
     */
    private static boolean isValidUuid(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return false;
        }
        
        try {
            UUID.fromString(uuid.trim());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Gets all header values with validation
     */
    public static ValidatedHeaders getValidatedHeaders(HttpServletRequest request) {
        return ValidatedHeaders.builder()
                .organizationUuid(validateOrganizationUuid(request))
                .userUuid(validateUserUuid(request))
                .traceId(validateTraceId(request))
                .sessionUuid(validateSessionUuid(request))
                .regionId(validateRegionId(request))
                .build();
    }

    /**
     * Data class to hold validated header values
     */
    @lombok.Data
    @lombok.Builder
    public static class ValidatedHeaders {
        private final String organizationUuid;
        private final String userUuid;
        private final String traceId;
        private final String sessionUuid;
        private final String regionId;
    }
}

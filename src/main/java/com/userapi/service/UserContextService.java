package com.userapi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Service to extract user context information from HTTP headers
 */
@Service
@Slf4j
public class UserContextService {

    private static final String USER_UUID_HEADER = "x-app-user-uuid";
    private static final String ORG_UUID_HEADER = "x-app-org-uuid";

    /**
     * Get the current user UUID from the request headers
     */
    public String getCurrentUserUuid() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            log.warn("No current request found in context");
            return null;
        }
        
        String userUuid = request.getHeader(USER_UUID_HEADER);
        log.debug("Retrieved user UUID from header: {}", userUuid);
        return userUuid;
    }

    /**
     * Get the current user UUID from a specific request
     */
    public String getCurrentUserUuid(HttpServletRequest request) {
        if (request == null) {
            log.warn("Request is null");
            return null;
        }
        
        String userUuid = request.getHeader(USER_UUID_HEADER);
        log.debug("Retrieved user UUID from request header: {}", userUuid);
        return userUuid;
    }

    /**
     * Get the current organization UUID from the request headers
     */
    public String getCurrentOrgUuid() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            log.warn("No current request found in context");
            return null;
        }
        
        String orgUuid = request.getHeader(ORG_UUID_HEADER);
        log.debug("Retrieved organization UUID from header: {}", orgUuid);
        return orgUuid;
    }

    /**
     * Get the current organization UUID from a specific request
     */
    public String getCurrentOrgUuid(HttpServletRequest request) {
        if (request == null) {
            log.warn("Request is null");
            return null;
        }
        
        String orgUuid = request.getHeader(ORG_UUID_HEADER);
        log.debug("Retrieved organization UUID from request header: {}", orgUuid);
        return orgUuid;
    }

    /**
     * Check if the current user has the required context (user UUID and org UUID)
     */
    public boolean hasValidUserContext() {
        String userUuid = getCurrentUserUuid();
        String orgUuid = getCurrentOrgUuid();
        
        boolean isValid = userUuid != null && !userUuid.trim().isEmpty() && 
                         orgUuid != null && !orgUuid.trim().isEmpty();
        
        log.debug("User context validation - User UUID: {}, Org UUID: {}, Valid: {}", 
                 userUuid, orgUuid, isValid);
        
        return isValid;
    }

    /**
     * Get the current HTTP request from the request context
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest();
    }
}
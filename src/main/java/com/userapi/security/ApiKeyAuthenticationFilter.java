package com.userapi.security;

import com.userapi.service.ApiKeyAuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

import static com.userapi.common.constants.HeaderConstants.API_KEY;

/**
 * Filter to authenticate requests using API keys
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyAuthenticationService apiKeyAuthenticationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String apiKey = request.getHeader(API_KEY);
        String requestUri = request.getRequestURI();
        
        log.debug("Processing request to: {} with API key present: {}", requestUri, apiKey != null);

        // Skip authentication for health check and actuator endpoints
        if (isPublicEndpoint(requestUri)) {
            log.debug("Skipping API key authentication for public endpoint: {}", requestUri);
            filterChain.doFilter(request, response);
            return;
        }

        // Check if API key is present
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Missing API key for protected endpoint: {}", requestUri);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing API key\",\"message\":\"x-api-key header is required\"}");
            return;
        }

        // Validate API key
        if (apiKeyAuthenticationService.validateApiKey(apiKey)) {
            log.debug("API key validation successful for request to: {}", requestUri);
            
            // Set authentication in security context
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(
                    "api-client", 
                    null, 
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_CLIENT"))
                );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            filterChain.doFilter(request, response);
        } else {
            log.warn("Invalid API key for request to: {}", requestUri);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid API key\",\"message\":\"The provided API key is invalid or expired\"}");
        }
    }

    /**
     * Check if the endpoint is public and doesn't require authentication
     */
    private boolean isPublicEndpoint(String requestUri) {
        return requestUri.startsWith("/actuator/") || 
               requestUri.equals("/actuator") ||
               requestUri.startsWith("/health") ||
               requestUri.equals("/") ||
               requestUri.startsWith("/swagger") ||
               requestUri.startsWith("/v3/api-docs");
    }
}

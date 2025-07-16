package com.userapi.security;

import com.userapi.service.ApiKeyAuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import static com.userapi.common.constants.HeaderConstants.API_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticationFilterTest {

    @Mock
    private ApiKeyAuthenticationService apiKeyAuthenticationService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private ApiKeyAuthenticationFilter filter;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws IOException {
        filter = new ApiKeyAuthenticationFilter(apiKeyAuthenticationService);
        responseWriter = new StringWriter();

        // Clear security context before each test
        SecurityContextHolder.clearContext();
    }

    private void setupResponseWriter() throws IOException {
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    @Test
    void doFilterInternal_WhenPublicEndpoint_ShouldSkipAuthentication() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/actuator/health");
        
        // Act
        filter.doFilterInternal(request, response, filterChain);
        
        // Assert
        verify(filterChain).doFilter(request, response);
        verify(apiKeyAuthenticationService, never()).validateApiKey(anyString());
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void doFilterInternal_WhenMissingApiKey_ShouldReturnUnauthorized() throws ServletException, IOException {
        // Arrange
        setupResponseWriter();
        when(request.getRequestURI()).thenReturn("/user/123");
        when(request.getHeader(API_KEY)).thenReturn(null);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        assertTrue(responseWriter.toString().contains("Missing API key"));
        verify(filterChain, never()).doFilter(request, response);
        verify(apiKeyAuthenticationService, never()).validateApiKey(anyString());
    }

    @Test
    void doFilterInternal_WhenEmptyApiKey_ShouldReturnUnauthorized() throws ServletException, IOException {
        // Arrange
        setupResponseWriter();
        when(request.getRequestURI()).thenReturn("/user/123");
        when(request.getHeader(API_KEY)).thenReturn("");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        assertTrue(responseWriter.toString().contains("Missing API key"));
        verify(filterChain, never()).doFilter(request, response);
        verify(apiKeyAuthenticationService, never()).validateApiKey(anyString());
    }

    @Test
    void doFilterInternal_WhenValidApiKey_ShouldAuthenticate() throws ServletException, IOException {
        // Arrange
        String apiKey = "valid-api-key";
        when(request.getRequestURI()).thenReturn("/user/123");
        when(request.getHeader(API_KEY)).thenReturn(apiKey);
        when(apiKeyAuthenticationService.validateApiKey(apiKey))
                .thenReturn(CompletableFuture.completedFuture(true));

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(apiKeyAuthenticationService).validateApiKey(apiKey);
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());

        // Verify authentication was set
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("api-client", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Test
    void doFilterInternal_WhenInvalidApiKey_ShouldReturnUnauthorized() throws ServletException, IOException {
        // Arrange
        setupResponseWriter();
        String apiKey = "invalid-api-key";
        when(request.getRequestURI()).thenReturn("/user/123");
        when(request.getHeader(API_KEY)).thenReturn(apiKey);
        when(apiKeyAuthenticationService.validateApiKey(apiKey))
                .thenReturn(CompletableFuture.completedFuture(false));

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(apiKeyAuthenticationService).validateApiKey(apiKey);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        assertTrue(responseWriter.toString().contains("Invalid API key"));
        verify(filterChain, never()).doFilter(request, response);

        // Verify no authentication was set
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WhenValidationThrowsException_ShouldReturnInternalServerError() throws ServletException, IOException {
        // Arrange
        setupResponseWriter();
        String apiKey = "test-api-key";
        when(request.getRequestURI()).thenReturn("/user/123");
        when(request.getHeader(API_KEY)).thenReturn(apiKey);

        CompletableFuture<Boolean> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Service error"));
        when(apiKeyAuthenticationService.validateApiKey(apiKey)).thenReturn(failedFuture);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(apiKeyAuthenticationService).validateApiKey(apiKey);
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        verify(response).setContentType("application/json");
        assertTrue(responseWriter.toString().contains("Authentication service error"));
        verify(filterChain, never()).doFilter(request, response);

        // Verify no authentication was set
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WhenValidationTimesOut_ShouldReturnInternalServerError() throws ServletException, IOException {
        // Arrange
        setupResponseWriter();
        String apiKey = "test-api-key";
        when(request.getRequestURI()).thenReturn("/user/123");
        when(request.getHeader(API_KEY)).thenReturn(apiKey);

        // Create a future that will never complete (simulating timeout)
        CompletableFuture<Boolean> timeoutFuture = new CompletableFuture<>();
        when(apiKeyAuthenticationService.validateApiKey(apiKey)).thenReturn(timeoutFuture);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(apiKeyAuthenticationService).validateApiKey(apiKey);
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        verify(response).setContentType("application/json");
        assertTrue(responseWriter.toString().contains("Authentication service error"));
        verify(filterChain, never()).doFilter(request, response);

        // Verify no authentication was set
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WhenSwaggerEndpoint_ShouldSkipAuthentication() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");
        
        // Act
        filter.doFilterInternal(request, response, filterChain);
        
        // Assert
        verify(filterChain).doFilter(request, response);
        verify(apiKeyAuthenticationService, never()).validateApiKey(anyString());
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void doFilterInternal_WhenApiDocsEndpoint_ShouldSkipAuthentication() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/v3/api-docs");
        
        // Act
        filter.doFilterInternal(request, response, filterChain);
        
        // Assert
        verify(filterChain).doFilter(request, response);
        verify(apiKeyAuthenticationService, never()).validateApiKey(anyString());
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void doFilterInternal_WhenHealthEndpoint_ShouldSkipAuthentication() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/health");
        
        // Act
        filter.doFilterInternal(request, response, filterChain);
        
        // Assert
        verify(filterChain).doFilter(request, response);
        verify(apiKeyAuthenticationService, never()).validateApiKey(anyString());
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void doFilterInternal_WhenRootEndpoint_ShouldSkipAuthentication() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/");
        
        // Act
        filter.doFilterInternal(request, response, filterChain);
        
        // Assert
        verify(filterChain).doFilter(request, response);
        verify(apiKeyAuthenticationService, never()).validateApiKey(anyString());
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void doFilterInternal_WhenWhitespaceApiKey_ShouldReturnUnauthorized() throws ServletException, IOException {
        // Arrange
        setupResponseWriter();
        when(request.getRequestURI()).thenReturn("/user/123");
        when(request.getHeader(API_KEY)).thenReturn("   ");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        assertTrue(responseWriter.toString().contains("Missing API key"));
        verify(filterChain, never()).doFilter(request, response);
        verify(apiKeyAuthenticationService, never()).validateApiKey(anyString());
    }
}

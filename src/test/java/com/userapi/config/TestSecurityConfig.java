package com.userapi.config;

import com.userapi.enums.PermissionResult;
import com.userapi.service.ApiKeyAuthenticationService;
import com.userapi.service.RolesServiceClient;
import com.userapi.models.external.roles.PermissionCheckRequest;
import com.userapi.models.external.roles.PermissionCheckResponse;
import com.userapi.security.CustomPermissionEvaluator;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

/**
 * Test configuration that provides a mock API key authentication service
 * to avoid external service calls during testing
 */
@Configuration
@Profile({"test", "ci"})
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = false) // Disable method security for tests
public class TestSecurityConfig {
    
    public TestSecurityConfig() {
        System.out.println("TestSecurityConfig is being loaded!");
    }

    @Bean
    @Primary
    public ApiKeyAuthenticationService mockApiKeyAuthenticationService() {
        ApiKeyAuthenticationService mockService = Mockito.mock(ApiKeyAuthenticationService.class);
        
        // Configure the mock to return true for the real API key (for unit tests)
        Mockito.when(mockService.validateApiKey("APAHdSmELUW4iMvBR6w4xP_q8K-blauC8HKml3CROOA"))
                .thenReturn(CompletableFuture.completedFuture(true));
        
        // Configure the mock to return true for the new API key (for integration tests)
        Mockito.when(mockService.validateApiKey("pzKOjno8c-aLPvTz0L4b6U-UGDs7_7qq3W7qu7lpF7w"))
                .thenReturn(CompletableFuture.completedFuture(true));
        
        // Configure the mock to return false for any other API key
        Mockito.when(mockService.validateApiKey(Mockito.argThat(key -> 
            !"APAHdSmELUW4iMvBR6w4xP_q8K-blauC8HKml3CROOA".equals(key) && 
            !"pzKOjno8c-aLPvTz0L4b6U-UGDs7_7qq3W7qu7lpF7w".equals(key))))
                .thenReturn(CompletableFuture.completedFuture(false));
        
        return mockService;
    }

    @Bean
    @Primary
    public RolesServiceClient mockRolesServiceClient() {
        RolesServiceClient mockService = Mockito.mock(RolesServiceClient.class);
        
        // Mock permission check to always return true for integration tests
        PermissionCheckResponse mockResponse = PermissionCheckResponse.builder()
                .result(PermissionResult.ACCEPTED)
                .build();
        
        Mockito.when(mockService.checkPermission(Mockito.any(PermissionCheckRequest.class)))
                .thenReturn(Mono.just(mockResponse));
        
        // Mock hasPermission method to always return true
        Mockito.when(mockService.hasPermission(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.just(true));
        
        return mockService;
    }

    @Bean
    @Primary
    public PermissionEvaluator testPermissionEvaluator() {
        return new PermissionEvaluator() {
            @Override
            public boolean hasPermission(org.springframework.security.core.Authentication authentication, Object targetDomainObject, Object permission) {
                // Always return true for tests
                return true;
            }

            @Override
            public boolean hasPermission(org.springframework.security.core.Authentication authentication, Serializable targetId, String targetType, Object permission) {
                // Always return true for tests
                return true;
            }
        };
    }

    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .formLogin().disable()
            .httpBasic().disable()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests(authz -> authz
                .anyRequest().permitAll() // Allow all requests in tests
            );
        
        return http.build();
    }

    @Bean
    @Primary
    public MethodSecurityExpressionHandler testMethodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(testPermissionEvaluator());
        return expressionHandler;
    }
}
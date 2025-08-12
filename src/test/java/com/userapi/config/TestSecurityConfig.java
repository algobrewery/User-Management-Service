package com.userapi.config;

import com.userapi.service.ApiKeyAuthenticationService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.CompletableFuture;

/**
 * Test configuration that provides a mock API key authentication service
 * to avoid external service calls during testing
 */
@TestConfiguration
@Profile("test")
public class TestSecurityConfig {

    @Bean
    @Primary
    public ApiKeyAuthenticationService mockApiKeyAuthenticationService() {
        ApiKeyAuthenticationService mockService = Mockito.mock(ApiKeyAuthenticationService.class);
        
        // Configure the mock to return true for the real API key (for unit tests)
        Mockito.when(mockService.validateApiKey("APAHdSmELUW4iMvBR6w4xP_q8K-blauC8HKml3CROOA"))
                .thenReturn(CompletableFuture.completedFuture(true));
        
        // Configure the mock to return false for any other API key
        Mockito.when(mockService.validateApiKey(Mockito.argThat(key -> !"APAHdSmELUW4iMvBR6w4xP_q8K-blauC8HKml3CROOA".equals(key))))
                .thenReturn(CompletableFuture.completedFuture(false));
        
        return mockService;
    }
}
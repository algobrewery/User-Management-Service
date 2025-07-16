package com.userapi.service;

import com.userapi.service.ApiKeyAuthenticationService.ApiKeyValidationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticationServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private ApiKeyAuthenticationService apiKeyAuthenticationService;

    @BeforeEach
    void setUp() {
        apiKeyAuthenticationService = new ApiKeyAuthenticationService(webClientBuilder);

        // Set up test configuration values
        ReflectionTestUtils.setField(apiKeyAuthenticationService, "clientManagementServiceUrl", "http://localhost:8081");
        ReflectionTestUtils.setField(apiKeyAuthenticationService, "timeoutSeconds", 5);
    }

    private void setupWebClientMocks() {
        // Set up mock chain
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void validateApiKey_WhenApiKeyIsNull_ShouldReturnFalse() throws Exception {
        // Act
        CompletableFuture<Boolean> result = apiKeyAuthenticationService.validateApiKey(null);

        // Assert
        assertNotNull(result);
        Boolean isValid = result.get(1, TimeUnit.SECONDS);
        assertFalse(isValid);

        // Verify no web client calls were made
        verifyNoInteractions(webClientBuilder);
    }

    @Test
    void validateApiKey_WhenApiKeyIsEmpty_ShouldReturnFalse() throws Exception {
        // Act
        CompletableFuture<Boolean> result = apiKeyAuthenticationService.validateApiKey("");

        // Assert
        assertNotNull(result);
        Boolean isValid = result.get(1, TimeUnit.SECONDS);
        assertFalse(isValid);

        // Verify no web client calls were made
        verifyNoInteractions(webClientBuilder);
    }

    @Test
    void validateApiKey_WhenApiKeyIsWhitespace_ShouldReturnFalse() throws Exception {
        // Act
        CompletableFuture<Boolean> result = apiKeyAuthenticationService.validateApiKey("   ");

        // Assert
        assertNotNull(result);
        Boolean isValid = result.get(1, TimeUnit.SECONDS);
        assertFalse(isValid);

        // Verify no web client calls were made
        verifyNoInteractions(webClientBuilder);
    }

    @Test
    void validateApiKey_WhenValidApiKey_ShouldReturnTrue() throws Exception {
        // Arrange
        setupWebClientMocks();
        String apiKey = "valid-api-key";
        ApiKeyValidationResponse validResponse = new ApiKeyValidationResponse(true, "client-123", "Valid key");

        when(responseSpec.bodyToMono(ApiKeyValidationResponse.class))
                .thenReturn(Mono.just(validResponse));

        // Act
        CompletableFuture<Boolean> result = apiKeyAuthenticationService.validateApiKey(apiKey);

        // Assert
        assertNotNull(result);
        Boolean isValid = result.get(10, TimeUnit.SECONDS);
        assertTrue(isValid);

        // Verify the correct calls were made
        verify(webClientBuilder).baseUrl("http://localhost:8081");
        verify(webClient).get();
        verify(requestHeadersUriSpec).uri("/api/validate");
        verify(requestHeadersSpec).header("x-api-key", apiKey);
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(ApiKeyValidationResponse.class);
    }

    @Test
    void validateApiKey_WhenInvalidApiKey_ShouldReturnFalse() throws Exception {
        // Arrange
        setupWebClientMocks();
        String apiKey = "invalid-api-key";
        ApiKeyValidationResponse invalidResponse = new ApiKeyValidationResponse(false, null, "Invalid key");

        when(responseSpec.bodyToMono(ApiKeyValidationResponse.class))
                .thenReturn(Mono.just(invalidResponse));

        // Act
        CompletableFuture<Boolean> result = apiKeyAuthenticationService.validateApiKey(apiKey);

        // Assert
        assertNotNull(result);
        Boolean isValid = result.get(10, TimeUnit.SECONDS);
        assertFalse(isValid);
    }

    @Test
    void validateApiKey_WhenWebClientThrowsException_ShouldReturnFalse() throws Exception {
        // Arrange
        setupWebClientMocks();
        String apiKey = "test-api-key";
        WebClientResponseException exception = WebClientResponseException.create(
                401, "Unauthorized", null, null, null);

        when(responseSpec.bodyToMono(ApiKeyValidationResponse.class))
                .thenReturn(Mono.error(exception));

        // Act
        CompletableFuture<Boolean> result = apiKeyAuthenticationService.validateApiKey(apiKey);

        // Assert
        assertNotNull(result);
        Boolean isValid = result.get(10, TimeUnit.SECONDS);
        assertFalse(isValid);
    }

    @Test
    void validateApiKey_WhenTimeoutOccurs_ShouldReturnFalse() throws Exception {
        // Arrange
        setupWebClientMocks();
        String apiKey = "test-api-key";

        when(responseSpec.bodyToMono(ApiKeyValidationResponse.class))
                .thenReturn(Mono.never()); // This will cause a timeout

        // Act
        CompletableFuture<Boolean> result = apiKeyAuthenticationService.validateApiKey(apiKey);

        // Assert
        assertNotNull(result);
        Boolean isValid = result.get(10, TimeUnit.SECONDS);
        assertFalse(isValid);
    }

    @Test
    void validateApiKey_WhenUnexpectedExceptionDuringSetup_ShouldReturnFalse() throws Exception {
        // Arrange
        String apiKey = "test-api-key";

        when(webClientBuilder.baseUrl(anyString())).thenThrow(new RuntimeException("Setup error"));

        // Act
        CompletableFuture<Boolean> result = apiKeyAuthenticationService.validateApiKey(apiKey);

        // Assert
        assertNotNull(result);
        Boolean isValid = result.get(1, TimeUnit.SECONDS);
        assertFalse(isValid);
    }

    @Test
    void validateApiKey_ShouldCompleteAsynchronously() {
        // Arrange
        setupWebClientMocks();
        String apiKey = "async-test-key";
        ApiKeyValidationResponse validResponse = new ApiKeyValidationResponse(true, "client-123", "Valid key");

        when(responseSpec.bodyToMono(ApiKeyValidationResponse.class))
                .thenReturn(Mono.just(validResponse).delayElement(Duration.ofMillis(100)));

        // Act
        CompletableFuture<Boolean> result = apiKeyAuthenticationService.validateApiKey(apiKey);

        // Assert
        assertNotNull(result);
        assertFalse(result.isDone()); // Should not be completed immediately

        // Wait for completion
        assertDoesNotThrow(() -> {
            Boolean isValid = result.get(5, TimeUnit.SECONDS);
            assertTrue(isValid);
        });
    }

    @Test
    void validateApiKey_WhenResponseIsNull_ShouldReturnFalse() throws Exception {
        // Arrange
        setupWebClientMocks();
        String apiKey = "test-api-key";

        when(responseSpec.bodyToMono(ApiKeyValidationResponse.class))
                .thenReturn(Mono.just(new ApiKeyValidationResponse(false, null, null)));

        // Act
        CompletableFuture<Boolean> result = apiKeyAuthenticationService.validateApiKey(apiKey);

        // Assert
        assertNotNull(result);
        Boolean isValid = result.get(10, TimeUnit.SECONDS);
        assertFalse(isValid);
    }

    @Test
    void validateApiKey_ShouldHandleMultipleConcurrentRequests() throws Exception {
        // Arrange
        setupWebClientMocks();
        String apiKey1 = "key1";
        String apiKey2 = "key2";
        ApiKeyValidationResponse validResponse = new ApiKeyValidationResponse(true, "client-123", "Valid key");

        when(responseSpec.bodyToMono(ApiKeyValidationResponse.class))
                .thenReturn(Mono.just(validResponse));

        // Act
        CompletableFuture<Boolean> result1 = apiKeyAuthenticationService.validateApiKey(apiKey1);
        CompletableFuture<Boolean> result2 = apiKeyAuthenticationService.validateApiKey(apiKey2);

        // Assert
        CompletableFuture<Void> allResults = CompletableFuture.allOf(result1, result2);
        assertDoesNotThrow(() -> allResults.get(10, TimeUnit.SECONDS));

        assertTrue(result1.get());
        assertTrue(result2.get());
    }
}

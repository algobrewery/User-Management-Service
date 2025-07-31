package com.userapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Service for validating API keys with the Client Management Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationService {

    private final WebClient.Builder webClientBuilder;

    @Value("${client-management.service.url:http://localhost:8081}")
    private String clientManagementServiceUrl;

    @Value("${client-management.service.timeout:5}")
    private int timeoutSeconds;

    /**
     * Validates an API key by calling the Client Management Service
     *
     * @param apiKey the API key to validate
     * @return CompletableFuture that resolves to true if the API key is valid, false otherwise
     */
    public CompletableFuture<Boolean> validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.debug("API key is null or empty");
            return CompletableFuture.completedFuture(false);
        }

        log.debug("Validating API key against Client Management Service at: {}", clientManagementServiceUrl);

        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(clientManagementServiceUrl)
                    .build();

            // Try to call the root endpoint first to see what's available
            String validationUrl = "/";
            log.debug("Calling root endpoint to check API structure: {}{}", clientManagementServiceUrl, validationUrl);

            // Call the Client Management Service to validate the API key asynchronously
            Mono<String> validationMono = webClient.get()
                    .uri(validationUrl)
                    .header("x-api-key", apiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .doOnNext(response -> log.debug("Received response from root endpoint: {}", response))
                    .doOnError(WebClientResponseException.class, e ->
                        log.warn("Root endpoint call failed with status: {} - {} - Response body: {}", 
                                e.getStatusCode(), e.getMessage(), e.getResponseBodyAsString()))
                    .doOnError(Exception.class, e ->
                        log.error("Error calling root endpoint: {}", e.getMessage(), e));

            // Convert Mono to CompletableFuture and return true if we get any response (meaning API key is valid)
            return validationMono.toFuture()
                    .thenApply(response -> {
                        log.debug("API key validation successful - received response from root endpoint");
                        return true;
                    })
                    .exceptionally(throwable -> {
                        log.error("API key validation failed: {}", throwable.getMessage(), throwable);
                        return false;
                    });

        } catch (Exception e) {
            log.error("Error setting up API key validation: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Response model for API key validation
     */
    public static class ApiKeyValidationResponse {
        private boolean valid;
        private String clientId;
        private String message;

        public ApiKeyValidationResponse() {}

        public ApiKeyValidationResponse(boolean valid, String clientId, String message) {
            this.valid = valid;
            this.clientId = clientId;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}

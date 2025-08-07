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

            // Call the dedicated validation endpoint
            String validationUrl = "/api/validate";
            log.debug("Calling validation endpoint: {}{}", clientManagementServiceUrl, validationUrl);

            // Call the Client Management Service to validate the API key asynchronously
            Mono<ApiKeyValidationResponse> validationMono = webClient.get()
                    .uri(validationUrl)
                    .header("x-api-key", apiKey)
                    .retrieve()
                    .bodyToMono(ApiKeyValidationResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .doOnNext(response -> log.debug("Received validation response: valid={}, message={}", 
                            response.isValid(), response.getMessage()))
                    .doOnError(WebClientResponseException.class, e -> {
                        if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                            log.debug("API key validation failed - unauthorized: {}", e.getStatusCode());
                        } else {
                            log.warn("Validation endpoint call failed with status: {} - {} - Response body: {}", 
                                    e.getStatusCode(), e.getMessage(), e.getResponseBodyAsString());
                        }
                    })
                    .doOnError(Exception.class, e ->
                        log.error("Error calling validation endpoint: {}", e.getMessage(), e));

            // Convert Mono to CompletableFuture and return the validation result
            return validationMono.toFuture()
                    .thenApply(response -> {
                        boolean isValid = response.isValid();
                        log.debug("API key validation result: {}", isValid);
                        return isValid;
                    })
                    .exceptionally(throwable -> {
                        if (throwable.getCause() instanceof WebClientResponseException) {
                            WebClientResponseException webEx = (WebClientResponseException) throwable.getCause();
                            if (webEx.getStatusCode().value() == 401 || webEx.getStatusCode().value() == 403) {
                                log.debug("API key validation failed - invalid key");
                                return false;
                            }
                        }
                        log.error("API key validation failed due to error: {}", throwable.getMessage(), throwable);
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

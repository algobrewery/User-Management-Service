package com.userapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

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
     * @return true if the API key is valid, false otherwise
     */
    public boolean validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.debug("API key is null or empty");
            return false;
        }

        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(clientManagementServiceUrl)
                    .build();

            // Call the Client Management Service to validate the API key
            Boolean isValid = webClient.get()
                    .uri("/api/validate")
                    .header("x-api-key", apiKey)
                    .retrieve()
                    .bodyToMono(ApiKeyValidationResponse.class)
                    .map(response -> response.isValid())
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .onErrorReturn(false)
                    .block();

            log.debug("API key validation result: {}", isValid);
            return Boolean.TRUE.equals(isValid);

        } catch (WebClientResponseException e) {
            log.warn("API key validation failed with status: {} - {}", e.getStatusCode(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error validating API key: {}", e.getMessage(), e);
            return false;
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

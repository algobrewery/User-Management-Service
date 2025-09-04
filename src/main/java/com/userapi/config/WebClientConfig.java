package com.userapi.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for WebClient used to communicate with Client Management Service
 */
@Configuration
public class WebClientConfig {

    @Value("${client-management.service.timeout:5}")
    private int clientManagementTimeout;

    @Value("${roles.service.timeout:10}")
    private int rolesServiceTimeout;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean("clientManagementWebClient")
    public WebClient clientManagementWebClient(WebClient.Builder webClientBuilder) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, clientManagementTimeout * 1000)
                .responseTimeout(Duration.ofSeconds(clientManagementTimeout))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(clientManagementTimeout, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(clientManagementTimeout, TimeUnit.SECONDS)));

        return webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean("rolesServiceWebClient")
    public WebClient rolesServiceWebClient(WebClient.Builder webClientBuilder, 
                                        @Value("${roles.service.url:http://localhost:8081}") String baseUrl) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, rolesServiceTimeout * 1000)
                .responseTimeout(Duration.ofSeconds(rolesServiceTimeout))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(rolesServiceTimeout, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(rolesServiceTimeout, TimeUnit.SECONDS)));

        return webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}

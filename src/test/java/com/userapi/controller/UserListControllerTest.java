package com.userapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.userapi.TestConstants;
import com.userapi.config.TestSecurityConfig;
import com.userapi.models.external.ListUsersRequest;
import com.userapi.models.external.ListUsersResponse;
import com.userapi.models.external.UserHierarchyResponse;
import com.userapi.service.UserService;
import com.userapi.service.ApiKeyAuthenticationService;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.concurrent.CompletableFuture;

// ✅ Import constants
import static com.userapi.common.constants.HeaderConstants.*;
import static com.userapi.TestConstants.*;

@WebMvcTest(value = UserListController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@ActiveProfiles("test")
public class UserListControllerTest {

    private static final String TEST_API_KEY = "APAHdSmELUW4iMvBR6w4xP_q8K-blauC8HKml3CROOA";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private ApiKeyAuthenticationService apiKeyAuthenticationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testListUsers() throws Exception {
        // Setup API key authentication mock
        Mockito.when(apiKeyAuthenticationService.validateApiKey(TEST_API_KEY))
                .thenReturn(CompletableFuture.completedFuture(true));

        ListUsersRequest request = new ListUsersRequest();
        request.setPage(DEFAULT_PAGE);
        request.setSize(DEFAULT_SIZE);
        // Set optional fields if needed

        ListUsersResponse mockResponse = new ListUsersResponse();
        mockResponse.setUsers(Collections.emptyList());
        mockResponse.setTotalElements(0L);
        mockResponse.setTotalPages(0);
        mockResponse.setCurrentPage(DEFAULT_PAGE);
        mockResponse.setPageSize(DEFAULT_SIZE);
        mockResponse.setHasNext(false);
        mockResponse.setHasPrevious(false);

        Mockito.when(userService.listUsers(any(ListUsersRequest.class), eq(LIST_TEST_ORG_UUID)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/users/filter")
                        .contentType(JSON_CONTENT_TYPE)
                        .header(API_KEY, TEST_API_KEY)
                        .header(APP_ORG_UUID, LIST_TEST_ORG_UUID)
                        .header(APP_USER_UUID, LIST_TEST_USER_UUID)
                        .header(APP_CLIENT_USER_SESSION_UUID, LIST_TEST_SESSION_UUID)
                        .header(APP_TRACE_ID, LIST_TEST_TRACE_ID)
                        .header(APP_REGION_ID, LIST_TEST_REGION_ID)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.currentPage").value(DEFAULT_PAGE))
                .andExpect(jsonPath("$.pageSize").value(DEFAULT_SIZE))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(false));
    }

    @Test
    void testGetUserHierarchy() throws Exception {
        // Setup API key authentication mock
        Mockito.when(apiKeyAuthenticationService.validateApiKey(TEST_API_KEY))
                .thenReturn(CompletableFuture.completedFuture(true));

        String userId = TEST_USER_ID;
        String orgUuid = LIST_TEST_ORG_UUID;

        UserHierarchyResponse mockResponse = new UserHierarchyResponse();
        // Optionally populate mockResponse

        Mockito.when(userService.getUserHierarchy(orgUuid, userId))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/users/{userId}/hierarchy", userId)
                        .header(API_KEY, TEST_API_KEY)
                        .header(APP_ORG_UUID, orgUuid))
                .andExpect(status().isOk());
    }
}

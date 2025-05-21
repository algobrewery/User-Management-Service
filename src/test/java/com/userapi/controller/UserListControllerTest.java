package com.userapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.userapi.models.external.ListUsersRequest;
import com.userapi.models.external.ListUsersResponse;
import com.userapi.models.external.UserHierarchyResponse;
import com.userapi.service.UserService;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ✅ Import header constants
import static com.userapi.common.constants.HeaderConstants.*;

@WebMvcTest(UserListController.class)
public class UserListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testListUsers() throws Exception {
        ListUsersRequest request = new ListUsersRequest();
        request.setPage(0);
        request.setSize(10);
        // Set optional fields if needed

        ListUsersResponse mockResponse = new ListUsersResponse();
        mockResponse.setUsers(Collections.emptyList());
        mockResponse.setTotalElements(0L);
        mockResponse.setTotalPages(0);
        mockResponse.setCurrentPage(0);
        mockResponse.setPageSize(10);
        mockResponse.setHasNext(false);
        mockResponse.setHasPrevious(false);

        Mockito.when(userService.listUsers(any(ListUsersRequest.class), eq("org-uuid")))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/users/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(APP_ORG_UUID, "org-uuid")
                        .header(APP_USER_UUID, "user-uuid")
                        .header(APP_CLIENT_USER_SESSION_UUID, "session-uuid")
                        .header(APP_TRACE_ID, "trace-id")
                        .header(APP_REGION_ID, "region-id")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(false));
    }

    @Test
    void testGetUserHierarchy() throws Exception {
        String userId = "user123";
        String orgUuid = "org-uuid";

        UserHierarchyResponse mockResponse = new UserHierarchyResponse();
        // Optionally populate mockResponse

        Mockito.when(userService.getUserHierarchy(orgUuid, userId))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/users/{userId}/hierarchy", userId)
                        .header(APP_ORG_UUID, orgUuid))
                .andExpect(status().isOk());
    }
}

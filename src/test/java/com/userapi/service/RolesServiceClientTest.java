package com.userapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.userapi.enums.PermissionResult;
import com.userapi.enums.RoleStatus;
import com.userapi.models.external.roles.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RolesServiceClientTest {

    @Test
    void testCreateRoleRequestBuilder() throws Exception {
        // Test the DTO builder
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode policy = objectMapper.readTree("{\"test\": \"policy\"}");
        
        CreateRoleRequest request = CreateRoleRequest.builder()
                .roleName("Test Role")
                .description("Test Description")
                .roleManagementType("CUSTOMER_MANAGED")
                .policy(policy)
                .build();

        assert request.getRoleName().equals("Test Role");
        assert request.getDescription().equals("Test Description");
        assert request.getRoleManagementType().equals("CUSTOMER_MANAGED");
        assert request.getPolicy().equals(policy);
    }

    @Test
    void testCreateRoleResponseBuilder() {
        // Test the response DTO builder
        CreateRoleResponse response = CreateRoleResponse.builder()
                .role_uuid("test-uuid")
                .name("Test Role")
                .status("success")
                .roleStatus(RoleStatus.ACTIVE)
                .build();

        assert response.getRole_uuid().equals("test-uuid");
        assert response.getName().equals("Test Role");
        assert response.isActive() == true;
    }

    @Test
    void testPermissionCheckRequestBuilder() {
        // Test the permission check DTO builder (header-based approach)
        PermissionCheckRequest request = PermissionCheckRequest.builder()
                .resource("users")
                .action("read")
                .build();

        // Note: user_uuid and organization_uuid are now provided via headers, not in request body
        assert request.getResource().equals("users");
        assert request.getAction().equals("read");
    }

    @Test
    void testPermissionCheckResponseBuilder() {
        // Test the permission check response DTO builder
        PermissionCheckResponse response = PermissionCheckResponse.builder()
                .result(PermissionResult.ACCEPTED)
                .resource("users")
                .action("read")
                .build();

        assert response.getResult() == PermissionResult.ACCEPTED;
        assert response.getResource().equals("users");
        assert response.getAction().equals("read");
    }
}

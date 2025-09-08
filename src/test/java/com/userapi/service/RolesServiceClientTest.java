package com.userapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

        assert request.getName().equals("Test Role");
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
                .is_active(true)
                .build();

        assert response.getRole_uuid().equals("test-uuid");
        assert response.getName().equals("Test Role");
        assert response.getIs_active().equals(true);
    }

    @Test
    void testPermissionCheckRequestBuilder() {
        // Test the permission check DTO builder
        PermissionCheckRequest request = PermissionCheckRequest.builder()
                .user_uuid("user-uuid")
                .organization_uuid("org-uuid")
                .resource("users")
                .action("read")
                .build();

        assert request.getUser_uuid().equals("user-uuid");
        assert request.getOrganization_uuid().equals("org-uuid");
        assert request.getResource().equals("users");
        assert request.getAction().equals("read");
    }

    @Test
    void testPermissionCheckResponseBuilder() {
        // Test the permission check response DTO builder
        PermissionCheckResponse response = PermissionCheckResponse.builder()
                .has_permission(true)
                .resource("users")
                .action("read")
                .build();

        assert response.getHas_permission().equals(true);
        assert response.getResource().equals("users");
        assert response.getAction().equals("read");
    }
}

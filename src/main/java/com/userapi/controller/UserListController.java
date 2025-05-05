package com.userapi.controller;

import com.userapi.models.external.ListUsersRequest;
import com.userapi.models.external.ListUsersResponse;
import com.userapi.models.external.UserHierarchyResponse;
import com.userapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static com.userapi.common.constants.HeaderConstants.*;

@RestController
@RequestMapping("/users") // Base path for multiple users operations
@RequiredArgsConstructor
public class UserListController {

    private final UserService userService;

    // POST http://52.23.186.179:8080/users/filter
    @PostMapping("/filter")
    public ResponseEntity<ListUsersResponse> listUsers(
            @RequestHeader(APP_ORG_UUID) String orgUuid,
            @RequestHeader(APP_USER_UUID) String userUuid,
            @RequestHeader(APP_CLIENT_USER_SESSION_UUID) String sessionUuid,
            @RequestHeader(APP_TRACE_ID) String traceId,
            @RequestHeader(APP_REGION_ID) String regionId,
            @Valid @RequestBody ListUsersRequest request) {

        ListUsersResponse response = userService.listUsers(request, orgUuid);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/{userId}/hierarchy")
    public ResponseEntity<UserHierarchyResponse> getUserHierarchy(
            @RequestHeader(APP_ORG_UUID) String orgUUID,
            @PathVariable String userId) {

        UserHierarchyResponse response = userService.getUserHierarchy(orgUUID, userId);
        return ResponseEntity.ok(response);
    }

}

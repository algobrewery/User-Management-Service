package com.userapi.controller;

import com.userapi.models.external.ListUsersRequest;
import com.userapi.models.external.ListUsersResponse;
import com.userapi.models.external.UserHierarchyResponse;
import com.userapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static com.userapi.common.constants.HeaderConstants.*;

@RestController
@RequestMapping("/users") // Base path for multiple users operations
@RequiredArgsConstructor
public class UserListController {
    private static final Logger logger = LoggerFactory.getLogger(UserListController.class);


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

        logger.info("Listing users for org: {}, traceId: {}, page: {}, size: {}",
                orgUuid, traceId, request.getPage(), request.getSize());

        ListUsersResponse response = userService.listUsers(request, orgUuid);
        logger.info("Found {} users for org: {}", response.getTotalElements(), orgUuid);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/hierarchy")
    public ResponseEntity<UserHierarchyResponse> getUserHierarchy(
            @RequestHeader(APP_ORG_UUID) String orgUUID,
            @PathVariable String userId) {

        logger.info("Fetching user hierarchy for user: {} in org: {}", userId, orgUUID);

        UserHierarchyResponse response = userService.getUserHierarchy(orgUUID, userId);
        logger.info("User hierarchy fetched successfully for user: {}", userId);
        return ResponseEntity.ok(response);
    }
}

package com.userapi.controller;

import com.userapi.converters.CreateUserRequestConverter;
import com.userapi.converters.CreateUserResponseConverter;
import com.userapi.models.external.CreateUserRequest;
import com.userapi.models.external.CreateUserResponse;
import com.userapi.models.external.GetUserResponse;
import com.userapi.models.internal.CreateUserInternalRequest;
import com.userapi.models.internal.CreateUserInternalResponse;
import com.userapi.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.userapi.models.external.UpdateUserRequest;
import com.userapi.models.external.UpdateUserResponse;


import javax.validation.Valid;

import java.util.concurrent.ExecutionException;

import static com.userapi.common.constants.HeaderConstants.*;

@RestController
@RequestMapping("/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final CreateUserRequestConverter createUserRequestConverter;
    private final CreateUserResponseConverter createUserResponseConverter;
    private final UserService userService;

    @Autowired
    public UserController(
            @Qualifier("CreateUserRequestConverter") CreateUserRequestConverter createUserRequestConverter,
            @Qualifier("CreateUserResponseConverter") CreateUserResponseConverter createUserResponseConverter,
            UserService userService) {
        this.createUserRequestConverter = createUserRequestConverter;
        this.createUserResponseConverter = createUserResponseConverter;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<CreateUserResponse> createUser(
            @RequestHeader(APP_ORG_UUID) String orgUUID,
            @RequestHeader(APP_USER_UUID) String userUUID,
            @RequestHeader(APP_CLIENT_USER_SESSION_UUID) String clientUserSessionUUID,
            @RequestHeader(APP_TRACE_ID) String traceID,
            @RequestHeader(APP_REGION_ID) String regionID,
            @Valid @RequestBody CreateUserRequest request) {

        logger.info("Creating user for org: {}, user: {}, traceId: {}", orgUUID, userUUID, traceID);

        CreateUserInternalRequest internalRequest = createUserRequestConverter.toInternal(
                orgUUID,
                userUUID,
                clientUserSessionUUID,
                traceID,
                regionID,
                request);
        try {
            return userService.createUser(internalRequest)
                    .thenApply(createUserResponseConverter::toExternal)
                    .thenApply(r -> new ResponseEntity<>(r, HttpStatus.CREATED))
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Exception in creating user", e);
            return new ResponseEntity<>(
                    CreateUserResponse.builder()
                            .message(e.getMessage())
                            .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                            .build(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<GetUserResponse> getUserById(
            @RequestHeader(APP_ORG_UUID) String orgUUID,
            @RequestHeader(APP_USER_UUID) String userUUID,
            @RequestHeader(APP_CLIENT_USER_SESSION_UUID) String clientUserSessionUUID,
            @RequestHeader(APP_TRACE_ID) String traceID,
            @RequestHeader(APP_REGION_ID) String regionID,
            @PathVariable String userId) {

        logger.info("Fetching user with ID: {} for org: {}, traceId: {}", userId, orgUUID, traceID);

        GetUserResponse response = userService.getUser(orgUUID, userId);
        logger.info("User fetched successfully for ID: {}", userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UpdateUserResponse> updateUser(
            @RequestHeader(APP_ORG_UUID) String orgUuid,
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRequest request) {

        logger.info("Updating user with ID: {} for org: {}", userId, orgUuid);

        UpdateUserResponse response = userService.updateUser(orgUuid, userId, request);
        logger.info("User updated successfully for ID: {}", userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<UpdateUserResponse> deactivateUser(
            @RequestHeader(APP_ORG_UUID) String orgUuid,
            @PathVariable String userId) {

        logger.info("Deactivating user with ID: {} for org: {}", userId, orgUuid);

        UpdateUserResponse response = userService.deactivateUser(orgUuid, userId);
        logger.info("User deactivated successfully for ID: {}", userId);
        return ResponseEntity.ok(response);
    }
}

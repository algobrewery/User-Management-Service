package com.userapi.controller;

import com.userapi.converters.CreateUserRequestConverter;
import com.userapi.converters.CreateUserResponseConverter;
import com.userapi.models.external.CreateUserRequest;
import com.userapi.models.external.CreateUserResponse;
import com.userapi.models.external.GetUserResponse;
import com.userapi.models.internal.CreateUserInternalRequest;
import com.userapi.models.internal.CreateUserInternalResponse;
import com.userapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.userapi.models.external.UpdateUserRequest;
import com.userapi.models.external.UpdateUserResponse;


import javax.validation.Valid;

import static com.userapi.common.constants.HeaderConstants.*;

@RestController
@RequestMapping("/user")
public class UserController {

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

        CreateUserInternalRequest internalRequest = createUserRequestConverter.toInternal(
                orgUUID,
                userUUID,
                clientUserSessionUUID,
                traceID,
                regionID,
                request);
        CreateUserInternalResponse internalResponse = userService.createUser(internalRequest);
        CreateUserResponse externalResponse = createUserResponseConverter.toExternal(internalResponse);
        return new ResponseEntity<>(externalResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<GetUserResponse> getUserById(
            @RequestHeader(APP_ORG_UUID) String orgUUID,
            @RequestHeader(APP_USER_UUID) String userUUID,
            @RequestHeader(APP_CLIENT_USER_SESSION_UUID) String clientUserSessionUUID,
            @RequestHeader(APP_TRACE_ID) String traceID,
            @RequestHeader(APP_REGION_ID) String regionID,
            @PathVariable String userId) {

        GetUserResponse response = userService.getUser(orgUUID, userId);
        return ResponseEntity.ok(response);
    }
        @PutMapping("/{userId}")
    public ResponseEntity<UpdateUserResponse> updateUser(
            @RequestHeader(APP_ORG_UUID) String orgUuid,
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRequest request) {

        UpdateUserResponse response = userService.updateUser(orgUuid, userId, request);
        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/{userId}")
    public ResponseEntity<UpdateUserResponse> deactivateUser(
            @RequestHeader(APP_ORG_UUID) String orgUuid,
            @PathVariable String userId) {

        UpdateUserResponse response = userService.deactivateUser(orgUuid, userId);
        return ResponseEntity.ok(response);
    }

}
